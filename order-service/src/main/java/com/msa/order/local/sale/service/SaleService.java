package com.msa.order.local.sale.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.OutboxCreatedEvent;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.kafka.dto.AccountDto;
import com.msa.order.global.util.DateConversionUtil;
import com.msa.order.global.util.GoldUtils;
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
import com.msa.order.local.sale.repository.CustomSaleRepository;
import com.msa.order.local.sale.repository.SaleItemRepository;
import com.msa.order.local.sale.repository.SalePaymentRepository;
import com.msa.order.local.sale.repository.SaleRepository;
import com.msa.order.local.sale.sale_enum.SaleStatus;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.msa.order.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
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

    private static final EnumSet<SaleStatus> PAYMENT_STATUSES = EnumSet.of(
            SaleStatus.PAYMENT,
            SaleStatus.WG,
            SaleStatus.DISCOUNT,
            SaleStatus.PAYMENT_TO_BANK
    );

    public SaleService(JwtUtil jwtUtil, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher, OutboxEventRepository outboxEventRepository, StockRepository stockRepository, SaleRepository saleRepository, CustomOrderStoneRepository customOrderStoneRepository, SaleItemRepository saleItemRepository, SalePaymentRepository salePaymentRepository, CustomSaleRepository customSaleRepository, StatusHistoryRepository statusHistoryRepository) {
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
    public CustomPage<SaleItemResponse> getSale(String input, String startAt, String endAt, String material, Pageable pageable) {
        SaleDto.Condition condition = new SaleDto.Condition(input, startAt, endAt, material);
        return customSaleRepository.findSales(condition, pageable);
    }

    // 판매 상품 수정
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

    // 재고 -> 판매
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

        LocalDateTime saleDate = LocalDateTime.now();
        Long storeId = stock.getStoreId();
        String storeName = stock.getStoreName();
        BigDecimal storeHarry = stock.getStoreHarry();
        String grade = stock.getStoreGrade();

        createOrAddToSale(stock, saleDate, storeId, storeName, storeHarry, grade, createNewSheet);

        stock.updateOrderStatus(OrderStatus.SALE);
        stock.updateStockNote(stockDto.getMainStoneNote(), stockDto.getAssistanceStoneNote(), stockDto.getStockNote());
        stock.getProduct().updateProductAddCost(stockDto.getAddProductLaborCost());
        stock.getProduct().updateProductWeightAndSize(stockDto.getProductSize(), new BigDecimal(stockDto.getGoldWeight()), new BigDecimal(stockDto.getStoneWeight()));

        updateNewHistory(stock.getFlowCode(), nickname, BusinessPhase.SALE);


        BigDecimal pureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), stock.getProduct().getMaterialName().toUpperCase(), storeHarry);
        Integer totalBalanceMoney = stock.getTotalStoneLaborCost() + stockDto.getStoneAddLaborCost() +
                stock.getProduct().getProductLaborCost() + stockDto.getAddProductLaborCost();

        AccountDto.updateCurrentBalance dto = AccountDto.updateCurrentBalance.builder()
                .eventId(eventId)
                .tenantId(tenantId)
                .saleType(SaleStatus.SALE.name())
                .type("STORE")
                .id(storeId)
                .name(storeName)
                .pureGoldBalance(pureGoldWeight)
                .moneyBalance(totalBalanceMoney)
                .build();

        publishAccountEvent(eventId, tenantId, storeId, dto);
    }

    public void createPayment(String accessToken, String eventId, SaleDto.Request saleDto, boolean createNewSheet) {

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

            SalePayment payment = createSalePayment(saleDto);
            payment.updateEventId(eventId);
            sale.addPayment(payment);
            salePaymentRepository.saveAndFlush(payment);

            BigDecimal pureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(saleDto.getGoldWeight(), saleDto.getMaterial().toUpperCase(), harry);

            AccountDto.updateCurrentBalance dto = AccountDto.updateCurrentBalance.builder()
                    .eventId(eventId)
                    .tenantId(tenantId)
                    .saleType(saleDto.getOrderStatus())
                    .type("STORE")
                    .id(storeId)
                    .name(storeName)
                    .pureGoldBalance(pureGoldWeight.negate())
                    .moneyBalance(saleDto.getPayAmount() * -1)
                    .build();

            publishAccountEvent(eventId, tenantId, storeId, dto);
        } catch (DataIntegrityViolationException e) {
            log.warn("멱등성 키 중복: 이미 처리된 요청입니다. eventId={}", eventId);
        }

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

            updateNewHistory(newFlowCode, nickname, BusinessPhase.RETURN);

            // 미수액 변경
            AccountDto.updateCurrentBalance dto = AccountDto.updateCurrentBalance.builder()
                    .eventId(eventId)
                    .tenantId(tenantId)
                    .saleType(sale.getSaleStatus().name())
                    .type("STORE")
                    .id(storeId)
                    .name(storeName)
                    .pureGoldBalance(pureGoldWeight.negate())
                    .moneyBalance(totalLaborCost * -1)
                    .build();

            publishAccountEvent(eventId, tenantId, storeId, dto);
        } else if (PAYMENT_STATUSES.contains(SaleStatus.valueOf(type))) {
            SalePayment payment = salePaymentRepository.findByFlowCode(newFlowCode)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            Sale sale = payment.getSale();
            AccountDto.updateCurrentBalance dto = AccountDto.updateCurrentBalance.builder()
                    .eventId(eventId)
                    .tenantId(tenantId)
                    .saleType(payment.getSaleStatus().name())
                    .type("STORE")
                    .id(sale.getAccountId())
                    .name(sale.getAccountName())
                    .pureGoldBalance(payment.getPureGoldWeight().negate())
                    .moneyBalance(payment.getCashAmount() * -1)
                    .build();

            publishAccountEvent(eventId, tenantId, sale.getAccountId(), dto);
            salePaymentRepository.delete(payment);
        } else {
            throw new IllegalArgumentException("처리할 수 없는 취소 타입입니다: " + type);
        }
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

        LocalDateTime saleDate = LocalDateTime.now();
        Long storeId = stock.getStoreId();
        String storeName = stock.getStoreName();
        BigDecimal storeHarry = stock.getStoreHarry();
        String grade = stock.getStoreGrade();

        createOrAddToSale(stock, saleDate, storeId, storeName, storeHarry, grade, createNewSheet);
        stock.updateOrderStatus(OrderStatus.SALE);

        updateNewHistory(stock.getFlowCode(), nickname, BusinessPhase.SALE);

        BigDecimal pureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), stockDto.getMaterialName().toUpperCase(), storeHarry);

        AccountDto.updateCurrentBalance dto = AccountDto.updateCurrentBalance.builder()
                .eventId(eventId)
                .tenantId(tenantId)
                .saleType(SaleStatus.SALE.name())
                .type("STORE")
                .id(storeId)
                .name(storeName)
                .pureGoldBalance(pureGoldWeight)
                .moneyBalance(stock.getTotalStoneLaborCost())
                .build();

        publishAccountEvent(eventId, tenantId, storeId, dto);
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

        updateStockStoneInfo(updateDto.getStoneInfos(), stock);

        int[] countStoneCost = countStoneCost(orderStones);
        stock.updateStoneCost(countStoneCost[0], countStoneCost[1], countStoneCost[2], countStoneCost[3], updateDto.getStoneAddLaborCost());

        Long assistantId = Long.valueOf(updateDto.getAssistantStoneId());
        if (!stock.getProduct().getAssistantStoneId().equals(assistantId)) {
            OffsetDateTime assistantStoneCreateAt = null;
            if (StringUtils.hasText(updateDto.getAssistantStoneCreateAt())) {
                assistantStoneCreateAt = DateConversionUtil.StringToOffsetDateTime(updateDto.getAssistantStoneCreateAt());
            }
            stock.getProduct().updateAssistantStone(updateDto.isAssistantStone(), assistantId, updateDto.getAssistantStoneName(), assistantStoneCreateAt);
        }

        product.updateProductAddCost(updateDto.getProductAddLaborCost());
        product.updateProductWeightAndSize(updateDto.getProductSize(), new BigDecimal(updateDto.getGoldWeight()), new BigDecimal(updateDto.getStoneWeight()));

        updateNewHistory(flowCode, nickname, BusinessPhase.SALE);

        int newTotalMoney = stock.getTotalStoneLaborCost() +
                stock.getStoneAddLaborCost() +
                stock.getProduct().getProductLaborCost() +
                stock.getProduct().getProductAddLaborCost();

        int moneyBalanceDelta = newTotalMoney - oldTotalMoney;

        BigDecimal pureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(updateDto.getGoldWeight(), stock.getProduct().getMaterialName().toUpperCase(), stock.getStoreHarry());

        AccountDto.updateCurrentBalance dto = AccountDto.updateCurrentBalance.builder()
                .eventId(eventId)
                .tenantId(tenantId)
                .saleType(SaleStatus.SALE.name())
                .type("STORE")
                .id(stock.getStoreId())
                .name(stock.getStoreName())
                .pureGoldBalance(pureGoldWeight)
                .moneyBalance(moneyBalanceDelta)
                .build();

        publishAccountEvent(eventId, tenantId, stock.getStoreId(), dto);
    }

    private void publishAccountEvent(String eventId, String tenantId, Long accountId, AccountDto.updateCurrentBalance dto) {
        try {
            String payload = objectMapper.writeValueAsString(dto);

            OutboxEvent outboxEvent = new OutboxEvent(
                    "current-balance-update",
                    accountId.toString(),
                    payload
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

    private void createOrAddToSale(Stock stock, LocalDateTime saleDate, Long storeId, String storeName, BigDecimal storeHarry, String grade, boolean createNewSheet) {
        Sale sale = getSale(saleDate, storeId, storeName, storeHarry, grade, createNewSheet);

        SaleItem saleItem = SaleItem.builder()
                .flowCode(stock.getFlowCode())
                .build();

        sale.addItem(saleItem);
        saleItem.setStock(stock);
        saleItemRepository.save(saleItem);
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
            saleStatus = SaleStatus.fromDisplayName(saleDto.getOrderStatus())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown payment type: " + saleDto.getOrderStatus()));
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

    private void updateNewHistory(Long flowCode, String nickname, BusinessPhase businessPhase) {
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory = StatusHistory.phaseChange(
                flowCode,
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                businessPhase,
                nickname
        );

        statusHistoryRepository.save(statusHistory);
    }

    public String checkBeforeSale(Long accountId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        String saleCodeByAccountIdAndDate = saleRepository.findSaleCodeByAccountIdAndDate(accountId, startOfDay, endOfDay)
                .map(String::valueOf)
                .orElse("");

        return saleCodeByAccountIdAndDate;
    }
}
