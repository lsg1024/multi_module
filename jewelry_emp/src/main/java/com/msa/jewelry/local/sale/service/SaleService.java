package com.msa.jewelry.local.sale.service;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.excel.dto.SaleExcelDto;
import com.msa.jewelry.global.excel.util.SaleExcelUtil;
import com.msa.jewelry.global.util.DateConversionUtil;
import com.msa.jewelry.global.util.GoldUtils;
import com.msa.jewelry.global.util.SafeParse;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.order.dto.StatusHistoryDto;
import com.msa.jewelry.local.order.dto.StoneDto;
import com.msa.jewelry.local.order.entity.OrderStone;
import com.msa.jewelry.local.order.entity.StatusHistory;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.repository.CustomOrderStoneRepository;
import com.msa.jewelry.local.order.repository.StatusHistoryRepository;
import com.msa.jewelry.local.product.dto.ProductImageView;
import com.msa.jewelry.local.product.service.ProductService;
import com.msa.jewelry.local.sale.dto.SaleDto;
import com.msa.jewelry.local.sale.dto.SaleItemResponse;
import com.msa.jewelry.local.sale.dto.SalePrintResponse;
import com.msa.jewelry.local.sale.entity.Sale;
import com.msa.jewelry.local.sale.entity.SaleItem;
import com.msa.jewelry.local.sale.entity.SalePayment;
import com.msa.jewelry.local.sale.repository.CustomSaleRepository;
import com.msa.jewelry.local.sale.repository.SaleItemRepository;
import com.msa.jewelry.local.sale.repository.SalePaymentRepository;
import com.msa.jewelry.local.sale.repository.SaleRepository;
import com.msa.jewelry.local.stock.dto.StockDto;
import com.msa.jewelry.local.stock.entity.ProductSnapshot;
import com.msa.jewelry.local.stock.entity.Stock;
import com.msa.jewelry.local.stock.repository.StockRepository;
import com.msa.jewelry.local.store.dto.StoreReceivableLogView;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.store.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.jewelry.global.util.DateConversionUtil.StringToLocalDateTime;
import static com.msa.jewelry.local.order.util.StoneUtil.countStoneCost;
import static com.msa.jewelry.local.order.util.StoneUtil.updateStockStoneInfo;

@Slf4j
@Service
@Transactional
public class SaleService {
    private final JwtUtil jwtUtil;
    private final StoreService storeService;
    private final FactoryService factoryService;
    private final StockRepository stockRepository;
    private final SaleRepository saleRepository;
    private final CustomOrderStoneRepository customOrderStoneRepository;
    private final SaleItemRepository saleItemRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final CustomSaleRepository customSaleRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final ProductService productService;

    /** 결제 취소 시 처리 가능한 SaleStatus 집합 (PAYMENT, WG, DISCOUNT, PAYMENT_TO_BANK). */
    private static final EnumSet<SaleStatus> PAYMENT_STATUSES = EnumSet.of(
            SaleStatus.PAYMENT,
            SaleStatus.WG,
            SaleStatus.DISCOUNT,
            SaleStatus.PAYMENT_TO_BANK
    );

    public SaleService(JwtUtil jwtUtil,
                       StoreService storeService,
                       FactoryService factoryService,
                       StockRepository stockRepository,
                       SaleRepository saleRepository,
                       CustomOrderStoneRepository customOrderStoneRepository,
                       SaleItemRepository saleItemRepository,
                       SalePaymentRepository salePaymentRepository,
                       CustomSaleRepository customSaleRepository,
                       StatusHistoryRepository statusHistoryRepository,
                       ProductService productService) {
        this.jwtUtil = jwtUtil;
        this.storeService = storeService;
        this.factoryService = factoryService;
        this.stockRepository = stockRepository;
        this.saleRepository = saleRepository;
        this.customOrderStoneRepository = customOrderStoneRepository;
        this.saleItemRepository = saleItemRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.customSaleRepository = customSaleRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productService = productService;
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
                String originStoneIdStr = orderStone.getOriginStoneId() != null
                        ? orderStone.getOriginStoneId().toString() : null;
                String originStoneWeightStr = orderStone.getOriginStoneWeight() != null
                        ? orderStone.getOriginStoneWeight().toPlainString() : null;
                StoneDto.StoneInfo stoneDto = new StoneDto.StoneInfo(
                        originStoneIdStr,
                        orderStone.getOriginStoneName(),
                        originStoneWeightStr,
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

            String stockStoreName = stock.getStoreId() != null
                    ? storeService.getStoreInfoView(stock.getStoreId()).storeName()
                    : null;
            return SaleDto.Response.builder()
                    .flowCode(saleItem.getFlowCode())
                    .createAt(saleItem.getCreateDate() != null ? saleItem.getCreateDate().toString() : null)
                    .saleType(saleItem.getItemStatus().name())
                    .name(stockStoreName)
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
                    .assistantStoneId(product.getAssistantStoneId() != null ? product.getAssistantStoneId().toString() : null)
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
                .createAt(salePayment.getCreateDate() != null ? salePayment.getCreateDate().toString() : null)
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
     * 판매 전체 목록 조회 (페이징 없이 배열 반환).
     * 메시지 전송 등에서 날짜 범위 내 판매 데이터를 한 번에 조회할 때 사용.
     */
    @Transactional(readOnly = true)
    public List<SaleItemResponse.SaleItem> getAllSales(String startAt, String endAt, String search, String material) {
        SaleDto.Condition condition = new SaleDto.Condition(search, startAt, endAt, material);
        return customSaleRepository.findAllSales(condition);
    }

    /**
     * 날짜 범위 내 판매된 고유한 거래처 목록 조회 (메시지 전송용).
     * 상품 정보 없이 거래처 ID/이름만 반환하여 경량화.
     */
    @Transactional(readOnly = true)
    public List<SaleDto.SaleStoreInfo> getSaleStores(String startAt, String endAt) {
        return customSaleRepository.findSaleStores(startAt, endAt);
    }

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

            applyBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, payment.getSaleStatus().name(), "STORE", storeId, storeName, payment.getMaterial(), payment.getPureGoldWeight(), payment.getCashAmount(), payment.getCreateDate());
        } catch (DataIntegrityViolationException e) {
            log.warn("멱등성 키 중복: 이미 처리된 요청입니다. eventId={}", eventId);
        }
    }

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
        String storeName = storeId != null
                ? storeService.getStoreInfoView(storeId).storeName() : null;
        BigDecimal storeHarry = stock.getStoreHarry();
        String grade = stock.getStoreGrade();

        Long factoryId = stock.getFactoryId();
        String factoryName = factoryId != null
                ? factoryService.getFactoryInfo(factoryId).factoryName() : null;

        Sale sale = createOrAddToSale(stock, transactionDate, storeId, storeName, storeHarry, grade, createNewSheet);
        ProductSnapshot product = stock.getProduct();
        stock.updateOrderStatus(OrderStatus.SALE);
        stock.updateStockNote(stockDto.getMainStoneNote(), stockDto.getAssistanceStoneNote(), stockDto.getStockNote());

        Long assistantId = SafeParse.toLongOrNull(stockDto.getAssistantStoneId());
        if (assistantId != null && !java.util.Objects.equals(stock.getProduct().getAssistantStoneId(), assistantId)) {
            LocalDateTime assistantStoneCreateAt = null;
            if (StringUtils.hasText(stockDto.getAssistantStoneCreateAt())) {
                assistantStoneCreateAt = DateConversionUtil.StringToLocalDateTime(stockDto.getAssistantStoneCreateAt());
            }
            stock.getProduct().updateAssistantStone(stockDto.isAssistantStone(), assistantId, stockDto.getAssistantStoneName(), assistantStoneCreateAt);
        }

        product.updateProductAddCost(stockDto.getAddProductLaborCost());
        product.updateProductWeightAndSize(stockDto.getProductSize(), SafeParse.toBigDecimalOrNull(stockDto.getGoldWeight()), SafeParse.toBigDecimalOrNull(stockDto.getStoneWeight()));

        updateNewHistory(stock.getFlowCode(), nickname, BusinessPhase.SALE, "판매 등록");

        // 재질이 null 이면 빈 문자열로 — calculatePureGoldWeightWithHarry / applyBalanceChange 내부 null 가드와 함께 동작.
        String materialNameUpper = product.getMaterialName() != null ? product.getMaterialName().toUpperCase() : "";
        BigDecimal storePureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), materialNameUpper, storeHarry);
        Integer storeTotalMoney = stock.getTotalStoneLaborCost() + stockDto.getStoneAddLaborCost() +
                product.getProductLaborCost() + stockDto.getAddProductLaborCost();

        // 공장 harry: 주문 시점에 Stock 으로 스냅샷된 값을 우선 사용 (storeHarry 패턴 동일).
        // 과거 데이터 등으로 null 인 경우 기본값 1.10 fallback.
        BigDecimal factoryHarry = stock.getFactoryHarry() != null ? stock.getFactoryHarry() : new BigDecimal("1.10");
        BigDecimal factoryPureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), materialNameUpper, factoryHarry);
        Integer factoryTotalMoney = stock.getTotalStonePurchaseCost() + product.getProductPurchaseCost();

        applyBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.SALE.name(), "STORE", storeId, storeName, product.getMaterialName(), storePureGoldWeight, storeTotalMoney, transactionDate);
        applyBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.PURCHASE.name(), "FACTORY", factoryId, factoryName, product.getMaterialName(), factoryPureGoldWeight, factoryTotalMoney, transactionDate);
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
        String storeName = storeId != null
                ? storeService.getStoreInfoView(storeId).storeName() : null;
        BigDecimal storeHarry = stock.getStoreHarry();
        String grade = stock.getStoreGrade();

        Long factoryId = stock.getFactoryId();
        String factoryName = factoryId != null
                ? factoryService.getFactoryInfo(factoryId).factoryName() : null;

        Sale sale = createOrAddToSale(stock, transactionDate, storeId, storeName, storeHarry, grade, createNewSheet);
        stock.updateOrderStatus(OrderStatus.SALE);

        updateNewHistory(stock.getFlowCode(), nickname, BusinessPhase.SALE, "판매 등록");

        // 판매처 정산 (판매가)
        BigDecimal storePureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), stockDto.getMaterialName().toUpperCase(), storeHarry);
        Integer storeTotalMoney = stock.getTotalStoneLaborCost() + stockDto.getStoneAddLaborCost() +
                stock.getProduct().getProductLaborCost() + stockDto.getProductAddLaborCost();

        // 공장 정산 (매입가) - Stock 에 스냅샷된 factoryHarry 우선 사용. null 이면 1.10 fallback.
        BigDecimal factoryHarry = stock.getFactoryHarry() != null ? stock.getFactoryHarry() : new BigDecimal("1.10");
        BigDecimal factoryPureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(stockDto.getGoldWeight(), stockDto.getMaterialName().toUpperCase(), factoryHarry);
        Integer factoryTotalMoney = stock.getTotalStonePurchaseCost() + stock.getProduct().getProductPurchaseCost();

        applyBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.SALE.name(), "STORE", storeId, storeName, stock.getProduct().getMaterialName(), storePureGoldWeight, storeTotalMoney, transactionDate);
        applyBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.PURCHASE.name(), "FACTORY", factoryId, factoryName, stock.getProduct().getMaterialName(), factoryPureGoldWeight, factoryTotalMoney, transactionDate);
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
            String storeName = storeId != null
                    ? storeService.getStoreInfoView(storeId).storeName() : null;
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
            applyBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.RETURN.name(), "STORE", storeId, storeName, materialName, pureGoldWeight.negate(), totalLaborCost * -1, lastModifiedDate);
        } else if (PAYMENT_STATUSES.contains(inputStatus)) {
            SalePayment payment = salePaymentRepository.findByFlowCode(newFlowCode)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            Sale sale = payment.getSale();
            LocalDateTime lastModifiedDate = sale.getCreateDate();
            applyBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.RETURN.name(), "STORE", sale.getAccountId(), sale.getAccountName(), payment.getMaterial(), payment.getPureGoldWeight().negate(), payment.getCashAmount() * -1, lastModifiedDate);
            salePaymentRepository.delete(payment);
        } else {
            throw new IllegalArgumentException("처리할 수 없는 취소 타입입니다: " + type);
        }
    }

    public List<SaleDto.SaleDetailDto> findSaleProductNameAndMaterial(String accessToken, Long storeId, Long productId, String materialName) {
        StoreView storeInfo = storeService.getStoreInfoView(storeId);

        if (!storeInfo.applyPastSales()) {
            return List.of();
        }

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

        Long assistantId = SafeParse.toLongOrNull(updateDto.getAssistantStoneId());
        if (assistantId != null) {
            stock.getProduct().updateAssistantStone(updateDto.isAssistantStone(), assistantId,
                    updateDto.getAssistantStoneName(), updateDto.isAssistantStone() ? StringToLocalDateTime(updateDto.getAssistantStoneCreateAt()) : null);
        }

        product.updateProductAddCost(updateDto.getProductAddLaborCost());
        product.updateProductWeightAndSize(updateDto.getProductSize(), SafeParse.toBigDecimalOrNull(updateDto.getGoldWeight()), SafeParse.toBigDecimalOrNull(updateDto.getStoneWeight()));

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
        String stockStoreName = stock.getStoreId() != null
                ? storeService.getStoreInfoView(stock.getStoreId()).storeName() : null;
        applyBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.SALE.name(), "STORE", stock.getStoreId(), stockStoreName, product.getMaterialName(), pureGoldWeightDelta, moneyBalanceDelta, lastModifiedDate);
    }

    /**
     * 잔액 변동을 type 에 따라 {@link StoreService#applyDelta} /
     * {@link FactoryService#applyDelta} 로 직접 위임한다.
     *
     * <p>같은 트랜잭션 안에서 in-process 호출로 매장/공장 잔액에 즉시 반영된다.
     * (과거 마이크로서비스 시절에는 account-service 의 {@code current-balance-update}
     * Kafka 토픽으로 발행하던 페이로드였으나, 모놀리식 통합 이후로는 토픽 발행 없이
     * 동일 JVM 안에서 동기 호출로 처리된다.)
     *
     * @param eventId         멱등성 키
     * @param saleCode        판매 코드 (TSID 문자열)
     * @param tenantId        테넌트 식별자
     * @param saleType        거래 유형 (예: "SALE", "PURCHASE", "RETURN")
     * @param type            거래 대상 유형 ("STORE" 또는 "FACTORY")
     * @param id              거래 대상 ID (매장 ID 또는 공장 ID)
     * @param name            거래 대상 이름
     * @param material        소재명 (예: "14K", "18K", "24K")
     * @param pureGoldBalance 순금 중량 변동분 (양수=증가, 음수=감소)
     * @param moneyBalance    현금 잔액 변동분 (양수=증가, 음수=감소)
     * @param saleDate        거래 발생 일시
     */
    private void applyBalanceChange(String eventId, String saleCode, String tenantId, String saleType,
                                      String type, Long id, String name, String material,
                                      BigDecimal pureGoldBalance, Integer moneyBalance, LocalDateTime saleDate) {
        Long moneyDelta = moneyBalance != null ? moneyBalance.longValue() : 0L;
        Long accountSaleCode = SafeParse.toLongOrNull(saleCode);
        String note = String.format("[%s] %s", saleType, name);

        log.info("applyBalanceChange type={} id={} gold={} money={} eventId={}",
                type, id, pureGoldBalance, moneyDelta, eventId);

        if ("STORE".equalsIgnoreCase(type)) {
            storeService.applyDelta(
                    id, pureGoldBalance, moneyDelta,
                    eventId, saleType, material, accountSaleCode, note
            );
        } else if ("FACTORY".equalsIgnoreCase(type)) {
            factoryService.applyDelta(
                    id, pureGoldBalance, moneyDelta,
                    eventId, saleType, material, accountSaleCode, note
            );
        } else {
            throw new IllegalArgumentException("지원하지 않는 잔고 갱신 type: " + type);
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
        String displayCode = generateDisplayCode();

        Sale newSale = Sale.builder()
                .saleStatus(SaleStatus.SALE)
                .accountId(storeId)
                .accountName(storeName)
                .accountHarry(harry)
                .accountGrade(grade)
                .displayCode(displayCode)
                .items(new ArrayList<>())
                .build();

        return saleRepository.save(newSale);
    }

    private String generateDisplayCode() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long todayCount = saleRepository.countByCreateDateBetween(startOfDay, endOfDay);
        int index = (int) todayCount + 1;

        return String.format("%02d%02d%02d%02d",
                today.getYear() % 100, today.getMonthValue(), today.getDayOfMonth(), index);
    }

    @NotNull
    private static SalePayment createSalePayment(SaleDto.Request saleDto) {

        final Integer cashAmountRaw = saleDto.getPayAmount();
        final int cashAmount = cashAmountRaw != null ? cashAmountRaw : 0;
        final String material = saleDto.getMaterial();
        BigDecimal pureGoldWeight = GoldUtils.calculatePureGoldWeightWithHarry(saleDto.getGoldWeight(), material, saleDto.getHarry());
        final BigDecimal goldWeight = SafeParse.toBigDecimalOrNull(saleDto.getGoldWeight());
        final String note = saleDto.getNote();

        SaleStatus saleStatus;
        try {
            saleStatus = SaleStatus.valueOf(saleDto.getOrderStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            saleStatus = SaleStatus.fromDisplayName(saleDto.getOrderStatus());
        }
        if (saleStatus == null) {
            // FE 가 알 수 없는 표시값을 보냈을 때 NPE 가 아닌 명확한 400 으로.
            throw new IllegalArgumentException("지원하지 않는 판매 상태값: " + saleDto.getOrderStatus());
        }

        if (saleStatus.equals(SaleStatus.WG)) {

            Integer accountGoldPrice = saleDto.getAccountGoldPrice();
            pureGoldWeight = GoldUtils.calculateWeightFromPrice(cashAmount, accountGoldPrice);
            BigDecimal pureNeg = pureGoldWeight != null ? pureGoldWeight.negate() : BigDecimal.ZERO;

            return SalePayment.builder()
                    .cashAmount(cashAmount * -1)
                    .pureGoldWeight(pureNeg)
                    .goldWeight(pureNeg)
                    .material("24K")
                    .paymentNote(note)
                    .saleStatus(saleStatus)
                    .build();
        }

        BigDecimal pureNeg = pureGoldWeight != null ? pureGoldWeight.negate() : BigDecimal.ZERO;
        BigDecimal goldNeg = goldWeight != null ? goldWeight.negate() : BigDecimal.ZERO;
        return SalePayment.builder()
                .cashAmount(cashAmount * -1)
                .material(material)
                .pureGoldWeight(pureNeg)
                .goldWeight(goldNeg)
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

        return new SaleDto.PastSaleRequest(sale.getSaleCode().toString(), sale.getDisplayCode(), sale.getAccountGoldPrice());
    }

    public Integer checkAccountGoldPrice(String saleCode) {
        Long saleCodeLong = SafeParse.toLongOrNull(saleCode);
        if (saleCodeLong == null) {
            throw new IllegalArgumentException("판매 코드(saleCode) 가 올바르지 않습니다: " + saleCode);
        }
        Sale sale = saleRepository.findBySaleCode(saleCodeLong)
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
        Long saleCodeLong = SafeParse.toLongOrNull(saleCode);
        if (saleCodeLong == null) {
            throw new IllegalArgumentException("판매 코드(saleCode) 가 올바르지 않습니다: " + saleCode);
        }
        Sale sale = saleRepository.findBySaleCodeAndSalePayments(saleCodeLong)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (sale.isAccountGoldPrice()) {
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

        // 빈 결과 가드 — 잘못된 saleCode / soft-deleted sale 등.
        if (printSales == null || printSales.isEmpty()) {
            throw new NotFoundException(NOT_FOUND);
        }
        SaleItemResponse saleItemResponse = printSales.get(0);

        // TODO: 상품 이미지 매핑 — SaleItem 에 productId 가 없어 storeId 로 잘못 조회하던 코드를
        //       제거. 정확한 매핑을 위해서는 findPrintSales 의 QueryProjection 에 productId 를
        //       포함하거나 flowCode → Stock.productId 별도 조회 후 productImages map 으로 채워야 함.
        //       지금은 imagePath 가 빈 상태로 응답.

        //미수금액 조회
        if (StringUtils.hasText(saleItemResponse.getStoreName())) {
            Long storeIdLong = Long.valueOf(saleItemResponse.getStoreId());
            StoreReceivableLogView storeAttemptDetail = storeService.getReceivableLog(storeIdLong, saleCode);

            return SalePrintResponse.builder()
                    .lastPaymentDate(storeAttemptDetail.lastSaleDate())
                    .previousMoneyBalance(storeAttemptDetail.previousMoneyBalance())
                    .previousGoldBalance(storeAttemptDetail.previousGoldBalance())
                    .afterMoneyBalance(storeAttemptDetail.afterMoneyBalance())
                    .afterGoldBalance(storeAttemptDetail.afterGoldBalance())
                    .saleItemResponses(printSales)
                    .build();
        }

        return SalePrintResponse.builder()
                .saleItemResponses(printSales)
                .build();
    }

    /**
     * 판매 내역 엑셀 다운로드
     * @param input 검색어
     * @param startAt 시작일
     * @param endAt 종료일
     * @param material 재질 필터
     * @return 엑셀 파일 바이트 배열
     */
    @Transactional(readOnly = true)
    public byte[] getSalesExcel(String input, String startAt, String endAt, String material) throws IOException {
        SaleDto.Condition condition = new SaleDto.Condition(input, startAt, endAt, material);
        List<SaleExcelDto> saleExcelDtos = customSaleRepository.findSalesForExcel(condition);
        return SaleExcelUtil.createSaleWorkSheet(saleExcelDtos);
    }
}
