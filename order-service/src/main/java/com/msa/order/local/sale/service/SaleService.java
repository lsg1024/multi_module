package com.msa.order.local.sale.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.OutboxCreatedEvent;
import com.msa.order.global.dto.StatusHistoryDto;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.excel.dto.SaleExcelDto;
import com.msa.order.global.excel.util.SaleExcelUtil;
import com.msa.order.global.feign_client.client.ProductClient;
import com.msa.order.global.feign_client.client.StoreClient;
import com.msa.order.global.feign_client.dto.ProductImageDto;
import com.msa.order.global.kafka.dto.AccountDto;
import com.msa.order.global.util.DateConversionUtil;
import com.msa.order.global.util.GoldUtils;
import com.msa.order.global.util.SafeParse;
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

import java.io.IOException;
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

/**
 * 판매 관리 서비스.
 *
 * *재고 또는 주문을 판매로 전환하고, 판매 수정·취소·결제를 처리한다.
 * 모든 잔액 변동은 {@link OutboxEvent}를 통해 account-service로 발행되며,
 * Transactional Outbox 패턴으로 이벤트 유실을 방지한다.
 *
 * *주요 의존성:
 *
 *   - {@link StockRepository} — 재고 조회 및 상태 전이
 *   - {@link SaleRepository} / {@link SaleItemRepository} / {@link SalePaymentRepository} — 판매 데이터 저장
 *   - {@link OutboxEventRepository} + {@link ApplicationEventPublisher} — 잔액 변동 이벤트 발행
 *   - {@link GoldUtils} — 순금 중량 계산
 * 
 * 
 *
 * *핵심 흐름:
 *
 *   - 재고/주문 → 판매 전환 ({@code stockToSale}, {@code orderToSale})
 *   - 판매 수정 ({@code updateSale})
 *   - 판매·결제 취소 ({@code cancelSale})
 *   - 미수금 결제 ({@code createStorePayment})
 * 
 * 
 */
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

    /** 결제 취소 시 처리 가능한 SaleStatus 집합 (PAYMENT, WG, DISCOUNT, PAYMENT_TO_BANK). */
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

    /**
     * 판매 상품 수정.
     *
     * *델타(차이) 기반으로 순금 중량 변동분({@code pureGoldWeightDelta})과
     * 현금 잔액 변동분({@code moneyBalanceDelta})만 계산하여 OutboxEvent를 발행한다.
     * 즉, 기존 금액을 취소하고 새 금액을 재발행하는 방식이 아니라
     * 변동분 하나만 발행하므로 account-service의 잔액이 이중 반영되지 않는다.
     *
     * @param accessToken JWT 액세스 토큰 (tenantId, 닉네임 추출용)
     * @param eventId     멱등성 키 (중복 처리 방지)
     * @param flowCode    수정 대상 재고의 고유 식별 코드
     * @param updateDto   수정 내용 (중량, 원가, 스톤 정보 등)
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
     * 판매처 미수금 결제 처리.
     *
     * *결제 유형이 {@code WG}(금 수거)인 경우 {@code cashAmount ÷ marketPrice}로
     * 금 중량을 역산한다 ({@link GoldUtils#calculateWeightFromPrice}).
     * 그 외 결제 유형은 {@code weight × purity × harry}로 순금 중량을 계산한다.
     *
     * *멱등성 키({@code eventId}) 중복 시 {@link DataIntegrityViolationException}을 조용히
     * 무시하여 동일 요청이 두 번 처리되지 않도록 보장한다.
     *
     * @param accessToken    JWT 액세스 토큰 (tenantId 추출용)
     * @param eventId        멱등성 키 — {@code SALE_PAYMENT.EVENT_ID} 유니크 제약으로 중복 방지
     * @param saleDto        결제 정보 (거래처 ID/명, 금 시세, 중량, 결제 유형 등)
     * @param createNewSheet 당일 기존 주문장에 추가({@code false}) 또는 신규 주문장 생성({@code true})
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
     * 재고 → 판매 전환.
     *
     * *재고({@link Stock}) 상태를 {@code SALE}로 전이하고,
     * 판매처(STORE)와 공장(FACTORY) 각각에 대해 {@link OutboxEvent}를 발행한다.
     *
     *
     *   - STORE 이벤트: {@code SaleStatus.SALE} — 판매가 기준 (매장 해리 적용)
     *   - FACTORY 이벤트: {@code SaleStatus.PURCHASE} — 매입가 기준 (공장 해리 1.10 고정)
     * 
     *
     * *멱등성: {@code SaleItem}이 이미 존재하면 중복 처리를 방지하고 즉시 반환한다.
     *
     * @param accessToken    JWT 액세스 토큰 (tenantId, 닉네임 추출용)
     * @param eventId        멱등성 키
     * @param flowCode       재고 고유 식별 코드
     * @param stockDto       판매 등록 요청 DTO (중량, 스톤 정보, 원가 등)
     * @param createNewSheet 당일 기존 주문장에 추가({@code false}) 또는 신규 주문장 생성({@code true})
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

        Long assistantId = SafeParse.toLongOrNull(stockDto.getAssistantStoneId());
        if (assistantId != null && !stock.getProduct().getAssistantStoneId().equals(assistantId)) {
            OffsetDateTime assistantStoneCreateAt = null;
            if (StringUtils.hasText(stockDto.getAssistantStoneCreateAt())) {
                assistantStoneCreateAt = DateConversionUtil.StringToOffsetDateTime(stockDto.getAssistantStoneCreateAt());
            }
            stock.getProduct().updateAssistantStone(stockDto.isAssistantStone(), assistantId, stockDto.getAssistantStoneName(), assistantStoneCreateAt);
        }

        product.updateProductAddCost(stockDto.getAddProductLaborCost());
        product.updateProductWeightAndSize(stockDto.getProductSize(), SafeParse.toBigDecimalOrNull(stockDto.getGoldWeight()), SafeParse.toBigDecimalOrNull(stockDto.getStoneWeight()));

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

    /**
     * 주문 → 판매 전환.
     *
     * *{@link #stockToSale}과 동일한 패턴으로 동작하지만,
     * 스톤 정보 갱신 없이 주문({@link Stock}) 상태를 {@code SALE}로 전이한다.
     * STORE(판매가) 및 FACTORY(매입가) OutboxEvent 2건을 발행한다.
     *
     * @param accessToken    JWT 액세스 토큰
     * @param eventId        멱등성 키
     * @param flowCode       재고 고유 식별 코드
     * @param stockDto       판매 등록 요청 DTO
     * @param createNewSheet 당일 기존 주문장에 추가({@code false}) 또는 신규 주문장 생성({@code true})
     */
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

    /**
     * 판매 또는 결제 취소.
     *
     * *취소 유형에 따라 처리 방식이 다르다:
     *
     *   - {@code SALE} 타입: 상품 반품 — 재고를 {@code STOCK} 상태로 복원하고,
     *       순금 중량과 현금 금액을 음수로 발행하여 잔액을 감소시킨다.
     *   - {@code PAYMENT_STATUSES}(PAYMENT/WG/DISCOUNT/PAYMENT_TO_BANK) 타입:
     *       결제 취소 — 기존 결제 레코드의 순금 중량·현금 금액이 이미 음수로 저장되어 있으므로,
     *       다시 음수를 취하면(이중 부정) 양수가 되어 미수금이 원상 복귀된다.
     * 
     *
     * @param accessToken JWT 액세스 토큰 (권한 및 tenantId 확인용)
     * @param eventId     멱등성 키
     * @param type        취소 대상 SaleStatus 이름 (예: "SALE", "PAYMENT", "WG")
     * @param flowCode    취소 대상 항목의 flowCode
     */
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

    public List<SaleDto.SaleDetailDto> findSaleProductNameAndMaterial(String accessToken, Long storeId, Long productId, String materialName) {
        StoreDto.Response storeInfo = storeClient.getStoreInfo(accessToken, storeId);

        if (!storeInfo.isOptionApplyPastSales()) {
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
                    updateDto.getAssistantStoneName(), updateDto.isAssistantStone() ? StringToOffsetDateTime(updateDto.getAssistantStoneCreateAt()) : null);
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
        publishBalanceChange(eventId, sale.getSaleCode().toString(), tenantId, SaleStatus.SALE.name(), "STORE", stock.getStoreId(), stock.getStoreName(), product.getMaterialName(), pureGoldWeightDelta, moneyBalanceDelta, lastModifiedDate);
    }

    /**
     * 잔액 변동 OutboxEvent DTO를 구성하여 {@link #publishAccountEvent}에 위임한다.
     *
     * *호출 시점마다 하나의 잔액 변동 이벤트를 만들어 account-service의
     * {@code current-balance-update} 토픽으로 발행할 페이로드를 조립한다.
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

    /**
     * OutboxEvent 엔티티를 저장하고 Spring 애플리케이션 이벤트를 발행한다.
     *
     * *DTO를 JSON으로 직렬화하여 {@link OutboxEvent}에 페이로드로 저장한 뒤,
     * {@link ApplicationEventPublisher}로 {@link OutboxCreatedEvent}를 발행하면
     * {@code OutboxRelayService}가 즉시 Kafka로 릴레이한다.
     * 직렬화 실패 시 트랜잭션 전체가 롤백되도록 {@link IllegalStateException}을 던진다.
     *
     * @param eventId   멱등성 키 (로깅용)
     * @param tenantId  테넌트 식별자 (릴레이 라우팅용)
     * @param accountId Kafka 메시지 키로 사용될 거래 대상 ID
     * @param dto       잔액 변동 페이로드 DTO
     */
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

        final Integer cashAmount = saleDto.getPayAmount();
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

        return new SaleDto.PastSaleRequest(sale.getSaleCode().toString(), sale.getDisplayCode(), sale.getAccountGoldPrice());
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
