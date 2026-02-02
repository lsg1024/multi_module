package com.msa.order.local.sale.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.OutboxCreatedEvent;
import com.msa.order.global.dto.StatusHistoryDto;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.feign_client.client.ProductClient;
import com.msa.order.global.feign_client.client.StoreClient;
import com.msa.order.global.feign_client.dto.ProductImageDto;
import com.msa.order.global.kafka.dto.AccountDto;
import com.msa.order.global.util.DateConversionUtil;
import com.msa.order.global.util.GoldUtils;
import com.msa.order.local.order.dto.StoreDto;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.repository.CustomOrderStoneRepository;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.outbox.domain.entity.OutboxEvent;
import com.msa.order.local.outbox.repository.OutboxEventRepository;
import com.msa.order.local.sale.entity.Sale;
import com.msa.order.local.sale.entity.SaleItem;
import com.msa.order.local.sale.entity.SalePayment;
import com.msa.order.local.sale.entity.dto.SaleDto;
import com.msa.order.local.sale.entity.dto.SaleItemResponse;
import com.msa.order.local.sale.entity.dto.SalePrintResponse;
import com.msa.order.local.sale.repository.CustomSaleRepository;
import com.msa.order.local.sale.repository.SaleItemRepository;
import com.msa.order.local.sale.repository.SalePaymentRepository;
import com.msa.order.local.sale.repository.SaleRepository;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.entity.ProductSnapshot;
import com.msa.order.local.stock.entity.Stock;
import com.msa.order.local.stock.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.msa.order.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.util.DateConversionUtil.StringToOffsetDateTime;
import static com.msa.order.local.order.util.StoneUtil.countStoneCost;
import static com.msa.order.local.order.util.StoneUtil.updateStockStoneInfo;

@Slf4j
@Service
@Transactional
public class SaleService {
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxEventRepository outboxEventRepository;
    private final StockRepository stockRepository;
    private final SaleRepository saleRepository;
    private final CustomOrderStoneRepository customOrderStoneRepository;
    private final SaleItemRepository saleItemRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final CustomSaleRepository customSaleRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final StoreClient storeClient;
    private final ProductClient productClient;

    private static final EnumSet<SaleStatus> PAYMENT_STATUSES = EnumSet.of(
            SaleStatus.PAYMENT,
            SaleStatus.WG,
            SaleStatus.DISCOUNT,
            SaleStatus.PAYMENT_TO_BANK
    );

    public SaleService(JwtUtil jwtUtil, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher, OutboxEventRepository outboxEventRepository, StockRepository stockRepository, SaleRepository saleRepository, CustomOrderStoneRepository customOrderStoneRepository, SaleItemRepository saleItemRepository, SalePaymentRepository salePaymentRepository, CustomSaleRepository customSaleRepository, StatusHistoryRepository statusHistoryRepository, StoreClient storeClient, ProductClient productClient) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.outboxEventRepository = outboxEventRepository;
        this.stockRepository = stockRepository;
        this.saleRepository = saleRepository;
        this.customOrderStoneRepository = customOrderStoneRepository;
        this.saleItemRepository = saleItemRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.customSaleRepository = customSaleRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.storeClient = storeClient;
        this.productClient = productClient;
    }

    @Transactional(readOnly = true)
    public SaleDto.Response getDetailSale(Long flowCode, String orderStatus) {

        if (orderStatus.equals(SaleStatus.SALE.name())) {
            SaleItem saleItem = saleItemRepository.findByFlowCode(flowCode)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            Stock stock = saleItem.getStock();
            ProductSnapshot product = stock.getProduct();

            List<OrderStone> orderStones = stock.getOrderStones();
            List<StoneDto.StoneInfo> stonesDtos = new ArrayList<>();
            for (OrderStone orderStone : orderStones) {
                StoneDto.StoneInfo stoneDto = new StoneDto.StoneInfo(
                        orderStone.getOriginStoneId().toString(),
                        orderStone.getOriginStoneName(),
                        orderStone.getOriginStoneWeight().toPlainString(),
                        orderStone.getStonePurchaseCost(),
                        orderStone.getStoneLaborCost(),
                        orderStone.getStoneAddLaborCost(),
                        orderStone.getStoneQuantity(),
                        orderStone.getMainStone(),
                        orderStone.getIncludeStone()
                );
                stonesDtos.add(stoneDto);
            }

            String assistantStoneCreateAt = null;
            if (product.getAssistantStoneCreateAt() != null) {
                assistantStoneCreateAt = String.valueOf(product.getAssistantStoneCreateAt());
            }

            return SaleDto.Response.builder()
                    .flowCode(saleItem.getFlowCode())
                    .createAt(String.valueOf(saleItem.getCreateDate()))
                    .saleType(saleItem.getItemStatus().name())
                    .name(stock.getStoreName())
                    .grade(stock.getStoreGrade())
                    .harry(stock.getStoreHarry())
                    .productName(product.getProductName())
                    .productSize(product.getSize())
                    .materialName(product.getMaterialName())
                    .colorName(product.getColorName())
                    .mainStoneNote(stock.getStockMainStoneNote())
                    .assistanceStoneNote(stock.getStockAssistanceStoneNote())
                    .note(stock.getStockNote())
                    .accountGoldPrice(saleItem.getSale().getAccountGoldPrice())
                    .goldWeight(product.getGoldWeight())
                    .stoneWeight(product.getStoneWeight())
                    .productLaborCost(product.getProductLaborCost())
                    .productAddLaborCost(product.getProductAddLaborCost())
                    .addStoneLaborCost(stock.getStoneAddLaborCost())
                    .assistantStone(product.isAssistantStone())
                    .assistantStoneId(String.valueOf(product.getAssistantStoneId()))
                    .assistantStoneName(product.getAssistantStoneName())
                    .assistantStoneCreateAt(assistantStoneCreateAt)
                    .stoneInfos(stonesDtos)
                    .build();
        }

        SalePayment salePayment = salePaymentRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        Sale sale = salePayment.getSale();

        return SaleDto.Response.builder()
                .flowCode(salePayment.getFlowCode())
                .createAt(String.valueOf(salePayment.getCreateDate()))
                .saleType(salePayment.getSaleStatus().name())
                .id(sale.getAccountId().toString())
                .name(sale.getAccountName())
                .grade(sale.getAccountGrade())
                .harry(sale.getAccountHarry())
                .materialName(salePayment.getMaterial())
                .note(salePayment.getPaymentNote())
                .goldWeight(salePayment.getGoldWeight())
                .productLaborCost(salePayment.getCashAmount())
                .build();
    }

    @Transactional(readOnly = true)
    public CustomPage<SaleItemResponse.SaleItem> getSale(String input, String startAt, String endAt, String material, Pageable pageable) {
        SaleDto.Condition condition = new SaleDto.Condition(input, startAt, endAt, material);
        CustomPage<SaleItemResponse.SaleItem> sales = customSaleRepository.findSales(condition, pageable);

        List<Long> flowCodes = sales.stream()
                .map(dto -> Long.valueOf(dto.getFlowCode()))
                .toList();

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        List<SaleItemResponse.SaleItem> content = sales.getContent();

        content
                .forEach(dto -> {
                    Long flowCode = Long.valueOf(dto.getFlowCode());
                    List<StatusHistory> statusHistories = historyMap.getOrDefault(flowCode, new ArrayList<>());

                    List<StatusHistoryDto> statusHistoryDtos = statusHistories.stream()
                            .map(StatusHistory::toDto)
                            .toList();

                    dto.updateHistory(statusHistoryDtos);
                });

        return sales;
    }

    /**
     * 판매 상품 수정
     * @param accessToken
     * @param eventId 판매번호
     * @param flowCode 생성번호
     * @param updateDto 판매정보
     */
    public void updateSale(String accessToken, String eventId, Long flowCode, SaleDto.updateRequest updateDto) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        String nickname = jwtUtil.getNickname(accessToken);

        if (eventId == null || eventId.isEmpty()) {
            throw new IllegalStateException("잘못된 형식의 요청입니다.");
        }

        try {
            performUpdate(eventId, flowCode, updateDto, tenantId, nickname);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    /**
     * 판매처 상품 미수금 결제 처리
     * @param accessToken 유저 유효성 체크
     * @param eventId 멱등성 체크
     * @param saleDto 상품 결제 정보
     * @param createNewSheet 주문창 추가 여부 (추가 true/신규 false)
     */
    public void createStorePayment(String accessToken, String eventId, SaleDto.Request saleDto, boolean createNewSheet) {

        if (!StringUtils.hasText(eventId)) {
            throw new IllegalStateException("멱등키 누락");
        }

        String tenantId = jwtUtil.getTenantId(accessToken);

        try {
            LocalDateTime saleDate = LocalDateTime.now();
            Long storeId = saleDto.getId();
            String storeName = saleDto.getName();
            BigDecimal harry = saleDto.getHarry();
            String grade = saleDto.getGrade();

            Sale sale = getSale(saleDate, storeId, storeName, harry, grade, createNewSheet);
            sale.updateAccountGoldPrice(saleDto.getAccountGoldPrice());

            SalePayment payment = createSalePayment(saleDto);
            payment.updateEventId(eventId);
            sale.addPayment(payment);
            salePaymentRepository.saveAndFlush(payment);

            publishBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, payment.getSaleStatus().name(), "STORE", storeId, storeName, payment.getMaterial(), payment.getPureGoldWeight(), payment.getCashAmount(), payment.getCreateDate());
        } catch (DataIntegrityViolationException e) {
            log.warn("멱등성 키 중복: 이미 처리된 요청입니다. eventId={}", eventId);
        }
    }

    /**
     * 재고 -> 판매 등록
     * @param accessToken 유저 유효성 체크
     * @param eventId 멱등성 ID
     * @param flowCode 고유 주문 상품 번호
     * @param stockDto 판매 내역
     * @param createNewSheet 기존 주문창 추가(true) 혹은 신규 생성(false)
     */
    public void stockToSale(String accessToken, String eventId, Long flowCode, StockDto.stockRequest stockDto, boolean createNewSheet) {

        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (saleItemRepository.existsByStock(stock)) {
            return;
        }

        if (!(stock.getOrderStatus() == OrderStatus.STOCK || stock.getOrderStatus() == OrderStatus.NORMAL)) {
            throw new IllegalStateException("판매로 전환 불가 상태: " + stock.getOrderStatus());
        }

        List<StoneDto.StoneInfo> stoneInfos = stockDto.getStoneInfos();
        updateStockStoneInfo(stoneInfos, stock);

        int[] countStoneCost = countStoneCost(stock.getOrderStones());
        stock.updateStoneCost(countStoneCost[0], countStoneCost[1], countStoneCost[2], countStoneCost[3], stockDto.getStoneAddLaborCost());

        LocalDateTime transactionDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        Long storeId = stock.getStoreId();
        String storeName = stock.getStoreName();
        BigDecimal storeHarry = stock.getStoreHarry();
        String grade = stock.getStoreGrade();

        Long factoryId = stock.getFactoryId();
        String factoryName = stock.getFactoryName();

        Sale sale = createOrAddToSale(stock, transactionDate, storeId, storeName, storeHarry, grade, createNewSheet);
        ProductSnapshot product = stock.getProduct();
        stock.updateOrderStatus(OrderStatus.SALE);
        stock.updateStockNote(stockDto.getMainStoneNote(), stockDto.getAssistanceStoneNote(), stockDto.getStockNote());

        Long assistantId = Long.valueOf(stockDto.getAssistantStoneId());
        if (!stock.getProduct().getAssistantStoneId().equals(assistantId)) {
            OffsetDateTime assistantStoneCreateAt = null;
            if (StringUtils.hasText(stockDto.getAssistantStoneCreateAt())) {
                assistantStoneCreateAt = DateConversionUtil.StringToOffsetDateTime(stockDto.getAssistantStoneCreateAt());
            }
            stock.getProduct().updateAssistantStone(stockDto.isAssistantStone(), assistantId, stockDto.getAssistantStoneName(), assistantStoneCreateAt);
        }

        product.updateProductAddCost(stockDto.getAddProductLaborCost());
        product.updateProductWeightAndSize(stockDto.getProductSize(), new BigDecimal(stockDto.getGoldWeight()), new BigDecimal(stockDto.getStoneWeight()));

        updateNewHistory(stock.getFlowCode(), nickname, BusinessPhase.SALE, "판매 등록");

        BigDecimal storePureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), product.getMaterialName().toUpperCase(), storeHarry);
        Integer storeTotalMoney = stock.getTotalStoneLaborCost() + stockDto.getStoneAddLaborCost() +
                product.getProductLaborCost() + stockDto.getAddProductLaborCost();

        BigDecimal factoryHarry = new BigDecimal("1.10");
        BigDecimal factoryPureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), product.getMaterialName().toUpperCase(), factoryHarry);
        Integer factoryTotalMoney = stock.getTotalStonePurchaseCost() + product.getProductPurchaseCost();

        publishBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.SALE.name(), "STORE", storeId, storeName, product.getMaterialName(), storePureGoldWeight, storeTotalMoney, transactionDate);
        publishBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.PURCHASE.name(), "FACTORY", factoryId, factoryName, product.getMaterialName(), factoryPureGoldWeight, factoryTotalMoney, transactionDate);
    }

    public void orderToSale(String accessToken, String eventId, Long flowCode, StockDto.StockRegisterRequest stockDto, boolean createNewSheet) {
        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (saleItemRepository.existsByStock(stock)) {
            return;
        }

        if (!(stock.getOrderStatus() == OrderStatus.STOCK || stock.getOrderStatus() == OrderStatus.NORMAL)) {
            throw new IllegalStateException("판매로 전환 불가 상태: " + stock.getOrderStatus());
        }

        LocalDateTime transactionDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        Long storeId = stock.getStoreId();
        String storeName = stock.getStoreName();
        BigDecimal storeHarry = stock.getStoreHarry();
        String grade = stock.getStoreGrade();

        Long factoryId = stock.getFactoryId();
        String factoryName = stock.getFactoryName();

        Sale sale = createOrAddToSale(stock, transactionDate, storeId, storeName, storeHarry, grade, createNewSheet);
        stock.updateOrderStatus(OrderStatus.SALE);

        updateNewHistory(stock.getFlowCode(), nickname, BusinessPhase.SALE, "판매 등록");

        // 판매처 정산 (판매가)
        BigDecimal storePureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), stockDto.getMaterialName().toUpperCase(), storeHarry);
        Integer storeTotalMoney = stock.getTotalStoneLaborCost() + stockDto.getStoneAddLaborCost() +
                stock.getProduct().getProductLaborCost() + stockDto.getProductAddLaborCost();

        // 공장 정산 (매입가) - 공장 해리는 1.10 고정
        BigDecimal factoryHarry = new BigDecimal("1.10");
        BigDecimal factoryPureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), stockDto.getMaterialName().toUpperCase(), factoryHarry);
        Integer factoryTotalMoney = stock.getTotalStonePurchaseCost() + stock.getProduct().getProductPurchaseCost();

        publishBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.SALE.name(), "STORE", storeId, storeName, stock.getProduct().getMaterialName(), storePureGoldWeight, storeTotalMoney, transactionDate);
        publishBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.PURCHASE.name(), "FACTORY", factoryId, factoryName, stock.getProduct().getMaterialName(), factoryPureGoldWeight, factoryTotalMoney, transactionDate);
    }

    //반품 로직 -> 제품은 다시 재고로, 결제는 다시 원복 -> 마지막 결제일의 경우?
    public void cancelSale(String accessToken, String eventId, String type, String flowCode) {
        String role = jwtUtil.getRole(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);
        String nickname = jwtUtil.getNickname(accessToken);

        if (role.equals("WAIT")) {
            throw new IllegalStateException(NOT_ACCESS);
        }

        Long newFlowCode = Long.valueOf(flowCode);

        SaleStatus inputStatus;
        try {
            inputStatus = SaleStatus.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("존재하지 않는 판매 타입입니다: " + type);
        }

        if (type.equals(SaleStatus.SALE.name())) {
            SaleItem saleItem = saleItemRepository.findByFlowCode(newFlowCode)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            if (saleItem.isReturned()) {
                throw new IllegalStateException("이미 반품된 상품입니다.");
            }

            Stock stock = saleItem.getStock();
            if (stock != null) {
                stock.returnToStock();
            }

            saleItem.markAsReturn();

            Sale sale = saleItem.getSale();
            sale.getItems().remove(saleItem);
            cleanupEmptySaleIfNeeded(sale);

            Long storeId = saleItem.getStock().getStoreId();
            String storeName = saleItem.getStock().getStoreName();
            BigDecimal goldWeight = saleItem.getStock().getProduct().getGoldWeight();
            String materialName = saleItem.getStock().getProduct().getMaterialName();
            BigDecimal pureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(goldWeight.toPlainString(), materialName.toUpperCase(), sale.getAccountHarry());

            Integer productLaborCost = saleItem.getStock().getProduct().getProductLaborCost();
            Integer productAddLaborCost = saleItem.getStock().getProduct().getProductAddLaborCost();
            Integer totalStoneLaborCost = saleItem.getStock().getTotalStoneLaborCost();
            Integer stoneAddLaborCost = saleItem.getStock().getStoneAddLaborCost();

            int totalLaborCost = productLaborCost + productAddLaborCost + totalStoneLaborCost + stoneAddLaborCost;

            updateNewHistory(newFlowCode, nickname, BusinessPhase.RETURN, "판매 취소");
            // 미수액 변경
            LocalDateTime lastModifiedDate = sale.getCreateDate();
            publishBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.RETURN.name(), "STORE", storeId, storeName, materialName, pureGoldWeight.negate(), totalLaborCost * -1, lastModifiedDate);
        } else if (PAYMENT_STATUSES.contains(inputStatus)) {
            SalePayment payment = salePaymentRepository.findByFlowCode(newFlowCode)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            Sale sale = payment.getSale();
            LocalDateTime lastModifiedDate = sale.getCreateDate();
            publishBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.RETURN.name(), "STORE", sale.getAccountId(), sale.getAccountName(), payment.getMaterial(), payment.getPureGoldWeight().negate(), payment.getCashAmount() * -1, lastModifiedDate);
            salePaymentRepository.delete(payment);
        } else {
            throw new IllegalArgumentException("처리할 수 없는 취소 타입입니다: " + type);
        }
    }

    // 과거 판매 내역
    public List<SaleDto.SaleDetailDto> findSaleProductNameAndMaterial(Long storeId, Long productId, String materialName) {
        List<SaleDto.SaleDetailDto> saleDetailDtos = customSaleRepository.findSalePast(storeId, productId, materialName);

        if (saleDetailDtos.isEmpty()) {
            return saleDetailDtos;
        }

        List<Long> flowCodes = saleDetailDtos.stream()
                .map(SaleDto.SaleDetailDto::getFlowCode)
                .distinct()
                .toList();

        List<SaleDto.StoneCountDto> stoneCounts = customOrderStoneRepository.findStoneCountsByStockIds(flowCodes);

        Map<Long, Map<Boolean, Integer>> stoneCountMaps = stoneCounts.stream()
                .collect(Collectors.groupingBy(
                        SaleDto.StoneCountDto::getFlowCode,
                        Collectors.toMap(SaleDto.StoneCountDto::getMainStone, SaleDto.StoneCountDto::getTotalQuantity)
                ));

        saleDetailDtos.forEach(dto -> {
            Map<Boolean, Integer> counts = stoneCountMaps.get(dto.getFlowCode());
            if (counts != null) {
                dto.setMainStoneQuantity(counts.getOrDefault(true, 0));
                dto.setAssistanceStoneQuantity(counts.getOrDefault(false, 0));
            } else {
                dto.setMainStoneQuantity(0);
                dto.setAssistanceStoneQuantity(0);
            }
        });

        return saleDetailDtos;
    }

    // 판매 수정 아직 활성화는 안되어 있음
    private void performUpdate(String eventId, Long flowCode, SaleDto.updateRequest updateDto, String tenantId, String nickname) {

        SaleItem saleItem = saleItemRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        Stock stock = saleItem.getStock();
        ProductSnapshot product = stock.getProduct();
        List<OrderStone> orderStones = stock.getOrderStones();

        stock.updateStockNote(updateDto.getMainStoneNote(), updateDto.getAssistanceStoneNote() , updateDto.getStockNote());
        Integer oldProductLaborCost = product.getProductLaborCost();
        Integer oldProductAddLaborCost = product.getProductAddLaborCost();
        Integer oldTotalStoneLaborCost = stock.getTotalStoneLaborCost();
        Integer oldStoneAddLaborCost = stock.getStoneAddLaborCost();

        int oldTotalMoney = oldProductLaborCost + oldProductAddLaborCost + oldTotalStoneLaborCost + oldStoneAddLaborCost;

        BigDecimal oldGoldWeight = product.getGoldWeight();
        BigDecimal oldPureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(
                oldGoldWeight.toPlainString(),
                product.getMaterialName().toUpperCase(),
                stock.getStoreHarry()
        );

        updateStockStoneInfo(updateDto.getStoneInfos(), stock);

        int[] countStoneCost = countStoneCost(orderStones);
        stock.updateStoneCost(countStoneCost[0], countStoneCost[1], countStoneCost[2], countStoneCost[3], updateDto.getStoneAddLaborCost());

        Long assistantId = Long.valueOf(updateDto.getAssistantStoneId());
        stock.getProduct().updateAssistantStone(updateDto.isAssistantStone(), assistantId,
                updateDto.getAssistantStoneName(), updateDto.isAssistantStone() ? StringToOffsetDateTime(updateDto.getAssistantStoneCreateAt()) : null);

        product.updateProductAddCost(updateDto.getProductAddLaborCost());
        product.updateProductWeightAndSize(updateDto.getProductSize(), new BigDecimal(updateDto.getGoldWeight()), new BigDecimal(updateDto.getStoneWeight()));

        updateNewHistory(flowCode, nickname, BusinessPhase.SALE, "판매 수정");

        int newTotalMoney = stock.getTotalStoneLaborCost() +
                stock.getStoneAddLaborCost() +
                stock.getProduct().getProductLaborCost() +
                stock.getProduct().getProductAddLaborCost();

        int moneyBalanceDelta = newTotalMoney - oldTotalMoney;

        BigDecimal newPureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(
                updateDto.getGoldWeight(),
                stock.getProduct().getMaterialName().toUpperCase(),
                stock.getStoreHarry()
        );
        BigDecimal pureGoldWeightDelta = newPureGoldWeight.subtract(oldPureGoldWeight);

        Sale sale = saleItem.getSale();
        LocalDateTime lastModifiedDate = sale.getCreateDate();
        publishBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.SALE.name(), "STORE", stock.getStoreId(), stock.getStoreName(), product.getMaterialName(), pureGoldWeightDelta, moneyBalanceDelta, lastModifiedDate);
    }

    private void publishBalanceChange(String eventId, String saleCode, String tenantId, String saleType,
                                      String type, Long id, String name, String material,
                                      BigDecimal pureGoldBalance, Integer moneyBalance, LocalDateTime saleDate) {
        AccountDto.updateCurrentBalance dto = AccountDto.updateCurrentBalance.builder()
                .eventId(eventId)
                .saleCode(saleCode)
                .tenantId(tenantId)
                .saleType(saleType)
                .type(type)
                .id(id)
                .name(name)
                .material(material)
                .pureGoldBalance(pureGoldBalance)
                .moneyBalance(moneyBalance)
                .saleDate(saleDate)
                .build();

        log.info("publishBalanceChange start = {}",dto.toString());
        publishAccountEvent(eventId, tenantId, id, dto);
    }

    private void publishAccountEvent(String eventId, String tenantId, Long accountId, AccountDto.updateCurrentBalance dto) {
        try {
            String payload = objectMapper.writeValueAsString(dto);

            OutboxEvent outboxEvent = new OutboxEvent(
                    "current-balance-update",
                    accountId.toString(),
                    payload,
                    "STOCK_CREATED"
            );

            outboxEventRepository.save(outboxEvent);
            eventPublisher.publishEvent(new OutboxCreatedEvent(tenantId));
        } catch (JsonProcessingException e) {
            log.error("DTO 직렬화 실패 eventId: {}", eventId);
            throw new IllegalStateException("주문 변경 실패");
        }
    }

    private void cleanupEmptySaleIfNeeded(Sale sale) {
        boolean allItemsReturned = sale.getItems().stream()
                .allMatch(SaleItem::isReturned);

        boolean allPaymentsCanceled = sale.getSalePayments().isEmpty();

        if (allItemsReturned && allPaymentsCanceled) {
            saleRepository.delete(sale);
        }
    }

    private Sale createOrAddToSale(Stock stock, LocalDateTime saleDate, Long storeId, String storeName, BigDecimal storeHarry, String grade, boolean createNewSheet) {
        Sale sale = getSale(saleDate, storeId, storeName, storeHarry, grade, createNewSheet);

        SaleItem saleItem = SaleItem.builder()
                .flowCode(stock.getFlowCode())
                .build();

        sale.addItem(saleItem);
        saleItem.setStock(stock);
        saleItemRepository.save(saleItem);

        return sale;
    }

    private Sale getSale(LocalDateTime saleDate, Long storeId, String storeName, BigDecimal harry, String grade, boolean createNewSheet) {

        if (createNewSheet) {
            return createNewSaleEntity(storeId, storeName, harry, grade);
        }

        LocalDate today = saleDate.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        return saleRepository.findLatestSaleByAccountIdAndDate(storeId, startOfDay, endOfDay)
                .orElseGet(() ->
                        createNewSaleEntity(storeId, storeName, harry, grade)
                );
    }

    private Sale createNewSaleEntity(Long storeId, String storeName, BigDecimal harry, String grade) {
        Sale newSale = Sale.builder()
                .saleStatus(SaleStatus.SALE)
                .accountId(storeId)
                .accountName(storeName)
                .accountHarry(harry)
                .accountGrade(grade)
                .items(new ArrayList<>())
                .build();

        return saleRepository.save(newSale);
    }

    @NotNull
    private static SalePayment createSalePayment(SaleDto.Request saleDto) {

        final Integer cashAmount = saleDto.getPayAmount();
        final String material = saleDto.getMaterial();
        BigDecimal pureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(saleDto.getGoldWeight(), material, saleDto.getHarry());
        final BigDecimal goldWeight = new BigDecimal(saleDto.getGoldWeight());
        final String note = saleDto.getNote();

        SaleStatus saleStatus;
        try {
            saleStatus = SaleStatus.valueOf(saleDto.getOrderStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            saleStatus = SaleStatus.fromDisplayName(saleDto.getOrderStatus());
        }

        if (saleStatus.equals(SaleStatus.WG)) {

            Integer accountGoldPrice = saleDto.getAccountGoldPrice();
            pureGoldWeight = GoldUtils.calculateWeightFromPrice(cashAmount, accountGoldPrice);

            return SalePayment.builder()
                    .cashAmount(cashAmount * -1)
                    .pureGoldWeight(pureGoldWeight.negate())
                    .goldWeight(pureGoldWeight.negate())
                    .material("24K")
                    .paymentNote(note)
                    .saleStatus(saleStatus)
                    .build();
        }

        return SalePayment.builder()
                .cashAmount(cashAmount * -1)
                .material(material)
                .pureGoldWeight(pureGoldWeight.negate())
                .goldWeight(goldWeight.negate())
                .paymentNote(note)
                .saleStatus(saleStatus)
                .build();
    }

    private void updateNewHistory(Long flowCode, String nickname, BusinessPhase businessPhase, String content) {
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory = StatusHistory.phaseChange(
                flowCode,
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                businessPhase,
                content,
                nickname
        );

        statusHistoryRepository.save(statusHistory);
    }

    /**
     * 당일 주문장 존재 여부 확인
     * @param accountId 주문 가게 아이디
     * @return true(존재), false(미존재)
     */
    public SaleDto.PastSaleRequest checkBeforeSale(Long accountId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        Sale sale = saleRepository.findSaleCodeByAccountIdAndDate(accountId, startOfDay, endOfDay)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        return new SaleDto.PastSaleRequest(sale.getSaleCode().toString(), sale.getAccountGoldPrice());
    }

    public Integer checkAccountGoldPrice(String saleCode) {
        Sale sale = saleRepository.findBySaleCode(Long.valueOf(saleCode))
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (sale.isAccountGoldPrice()) {
            return sale.getAccountGoldPrice();
        }
        return 0;
    }

    /**
     * 주문장 시세 추가
     * @param saleCode 주문장 고유 번호
     * @param goldPriceRequest 추가할 시세
     */
    public void updateAccountGoldPrice(String saleCode, SaleDto.GoldPriceRequest goldPriceRequest) {
        Sale sale = saleRepository.findBySaleCodeAndSalePayments(Long.valueOf(saleCode))
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (sale.isAccountGoldPrice()) {
            // 업데이트 기존 WG 값이 있었다면 이전 값들 수정 수정
            List<SalePayment> salePayments = sale.getSalePayments();
            for (SalePayment salePayment : salePayments) {
                if (salePayment.getSaleStatus().equals(SaleStatus.WG)) {

                }
            }
        }

        sale.updateAccountGoldPrice(goldPriceRequest.getAccountGoldPrice());
    }

    /**
     * 거래 명세서 출력
     * @param token 인증 토큰
     * @param saleCode 판매 코드
     * @return 거래 명세서 데이터
     */
    @Transactional(readOnly = true)
    public SalePrintResponse getSalePrint(String token, String saleCode) {
        List<SaleItemResponse> printSales = customSaleRepository.findPrintSales(saleCode);

        SaleItemResponse saleItemResponse = printSales.get(0);

        List<Long> productIds = new ArrayList<>();
        for (SaleItemResponse printSale : printSales) {
            List<SaleItemResponse.SaleItem> saleItems = printSale.getSaleItems();
            List<Long> ids = saleItems.stream()
                    .map(SaleItemResponse.SaleItem::getStoreId)
                    .filter(id -> id != null && !id.isEmpty())
                    .map(Long::parseLong)
                    .distinct()
                    .toList();

            productIds.addAll(ids);
        }

        Map<Long, ProductImageDto> productImages = productClient.getProductImages(token, productIds);

        for (SaleItemResponse printSale : printSales) {
            for (SaleItemResponse.SaleItem item : printSale.getSaleItems()) {
                if (StringUtils.hasText(item.getStoreId())) {
                    Long key = Long.parseLong(item.getStoreId());

                    if (productImages.containsKey(key)) {
                        item.updateImagePath(productImages.get(key).getImagePath());
                    }
                }
            }
        }

        //미수금액 조회
        if (StringUtils.hasText(saleItemResponse.getStoreName())) {
            StoreDto.accountResponse storeAttemptDetail = storeClient.getStoreReceivableDetailLog(token, saleItemResponse.getStoreId(), saleCode);

            return SalePrintResponse.builder()
                    .lastPaymentDate(storeAttemptDetail.getLastSaleDate())
                    .businessOwnerNumber(storeAttemptDetail.getBusinessOwnerNumber())
                    .faxNumber(storeAttemptDetail.getFaxNumber())
                    .previousMoneyBalance(storeAttemptDetail.getPreviousMoneyBalance())
                    .previousGoldBalance(storeAttemptDetail.getPreviousGoldBalance())
                    .afterMoneyBalance(storeAttemptDetail.getAfterMoneyBalance())
                    .afterGoldBalance(storeAttemptDetail.getAfterGoldBalance())
                    .saleItemResponses(printSales)
                    .build();
        }

        return SalePrintResponse.builder()
                .saleItemResponses(printSales)
                .build();
    }
}
