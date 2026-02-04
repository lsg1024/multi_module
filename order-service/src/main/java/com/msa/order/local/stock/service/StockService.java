package com.msa.order.local.stock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.OutboxCreatedEvent;
import com.msa.order.global.dto.StatusHistoryDto;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.feign_client.client.AssistantStoneClient;
import com.msa.order.global.feign_client.dto.AssistantStoneDto;
import com.msa.order.global.kafka.dto.KafkaStockRequest;
import com.msa.order.global.util.DateConversionUtil;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.entity.OrderProduct;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.msa.order.local.order.entity.order_enum.SourceType;
import com.msa.order.local.order.repository.OrdersRepository;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.order.util.ChangeTracker;
import com.msa.order.local.order.util.StatusHistoryHelper;
import com.msa.order.local.outbox.domain.entity.OutboxEvent;
import com.msa.order.local.outbox.repository.OutboxEventRepository;
import com.msa.order.local.stock.dto.InventoryDto;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.entity.ProductSnapshot;
import com.msa.order.local.stock.entity.Stock;
import com.msa.order.local.stock.repository.CustomStockRepository;
import com.msa.order.local.stock.repository.StockRepository;
import com.msa.order.global.exception.StockNotFoundException;
import com.msa.order.global.exception.OrderNotFoundException;
import com.msa.order.global.exception.InvalidOrderStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.msa.order.global.exception.ExceptionMessage.*;
import static com.msa.order.global.util.DateConversionUtil.StringToOffsetDateTime;
import static com.msa.order.local.order.util.StoneUtil.*;

@Slf4j
@Service
@Transactional
public class StockService {
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AssistantStoneClient assistantStoneClient;
    private final StockRepository stockRepository;
    private final OrdersRepository ordersRepository;
    private final CustomStockRepository customStockRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StatusHistoryHelper statusHistoryHelper;

    public StockService(JwtUtil jwtUtil, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher, AssistantStoneClient assistantStoneClient, StockRepository stockRepository, OrdersRepository ordersRepository, CustomStockRepository customStockRepository, StatusHistoryRepository statusHistoryRepository, OutboxEventRepository outboxEventRepository, StatusHistoryHelper statusHistoryHelper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.assistantStoneClient = assistantStoneClient;
        this.stockRepository = stockRepository;
        this.ordersRepository = ordersRepository;
        this.customStockRepository = customStockRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.statusHistoryHelper = statusHistoryHelper;
    }

    // 재고 상세 조회
    @Transactional(readOnly = true)
    public List<StockDto.ResponseDetail> getDetailStock(List<Long> flowCodes) {
        List<Stock> stocks = stockRepository.findByFlowCodeIn(flowCodes);
        List<StatusHistory> statusHistories = statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(flowCodes);

        List<StockDto.ResponseDetail> responseDetails = new ArrayList<>();
        for (int i = 0; i < stocks.size(); i++) {
            Stock stock = stocks.get(i);
            StatusHistory statusHistory = statusHistories.get(i);

            List<OrderStone> orderStones = stock.getOrderStones();
            List<StoneDto.StoneInfo> stonesDtos = toStoneDtoList(orderStones);

            StockDto.ResponseDetail stockDetail = StockDto.ResponseDetail.builder()
                    .createAt(stock.getCreateDate().toString())
                    .flowCode(stock.getFlowCode().toString())
                    .originalProductStatus(statusHistory.getSourceType().getDisplayName())
                    .storeId(String.valueOf(stock.getStoreId()))
                    .storeName(stock.getStoreName())
                    .storeHarry(stock.getStoreHarry().toPlainString())
                    .storeGrade(stock.getStoreGrade())
                    .factoryId(String.valueOf(stock.getFactoryId()))
                    .factoryName(stock.getFactoryName())
                    .productId(String.valueOf(stock.getProduct().getId()))
                    .productName(stock.getProduct().getProductName())
                    .productSize(stock.getProduct().getSize())
                    .colorId(String.valueOf(stock.getProduct().getColorId()))
                    .colorName(stock.getProduct().getColorName())
                    .materialId(String.valueOf(stock.getProduct().getMaterialId()))
                    .materialName(stock.getProduct().getMaterialName())
                    .note(stock.getStockNote())
                    .isProductWeightSale(stock.getProduct().isProductWeightSale())
                    .productPurchaseCost(stock.getProduct().getProductPurchaseCost())
                    .productLaborCost(stock.getProduct().getProductLaborCost())
                    .productAddLaborCost(stock.getProduct().getProductAddLaborCost())
                    .goldWeight(stock.getProduct().getGoldWeight().toPlainString())
                    .stoneWeight(stock.getProduct().getStoneWeight().toPlainString())
                    .mainStoneNote(stock.getStockMainStoneNote())
                    .assistanceStoneNote(stock.getStockAssistanceStoneNote())
                    .assistantStone(stock.getProduct().isAssistantStone())
                    .assistantStoneId(String.valueOf(stock.getProduct().getAssistantStoneId()))
                    .assistantStoneName(stock.getProduct().getAssistantStoneName())
                    .assistantStoneCreateAt(stock.getProduct().getAssistantStoneCreateAt())
                    .stoneInfos(stonesDtos)
                    .stoneAddLaborCost(stock.getStoneAddLaborCost())
                    .build();

            responseDetails.add(stockDetail);
        }
        return responseDetails;
    }

    // 재고 관리  주문, 수리, 대여 관련
    @Transactional(readOnly = true)
    public CustomPage<StockDto.Response> getStocks(String input, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String sortField, String sort, String orderStatus, Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.SortCondition sortCondition = new OrderDto.SortCondition(sortField, sort);
        StockDto.StockCondition condition = new StockDto.StockCondition(startAt, endAt, optionCondition, sortCondition, orderStatus);

        CustomPage<StockDto.Response> stockDtoPage = customStockRepository.findByStockProducts(inputCondition, condition, pageable);

        List<StockDto.Response> content = stockDtoPage.getContent();

        List<Long> flowCodes = content.stream()
                .map(dto -> Long.valueOf(dto.getFlowCode()))
                .toList();

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        content
                .forEach(dto -> {
                    Long flowCode = Long.valueOf(dto.getFlowCode());
                    List<StatusHistory> statusHistories = historyMap.getOrDefault(flowCode, new ArrayList<>());

                    List<StatusHistoryDto> statusHistoryDtos = statusHistories.stream()
                            .map(StatusHistory::toDto)
                            .toList();

                    dto.updateHistory(statusHistoryDtos);
                });

        return stockDtoPage;
    }

    @Transactional(readOnly = true)
    public CustomPage<StockDto.Response> getPastRentalHistory(String input, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String sortField, String sort,  Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.SortCondition sortCondition = new OrderDto.SortCondition(sortField, sort);
        BusinessPhase historicalPhase = BusinessPhase.RENTAL;
        StockDto.HistoryCondition historyCondition = new StockDto.HistoryCondition(startAt, endAt, historicalPhase, optionCondition, sortCondition);

        CustomPage<StockDto.Response> stocksByHistoricalPhase = customStockRepository.findStocksByHistoricalPhase(inputCondition, historyCondition, pageable);
        for (StockDto.Response response : stocksByHistoricalPhase) {
            String originStatus = response.getOriginStatus();
            String currentStatus = response.getCurrentStatus();
            response.updateStatus(SourceType.valueOf(originStatus).getDisplayName(), OrderStatus.valueOf(currentStatus).getDisplayName());
        }

        return stocksByHistoricalPhase;
    }

    // 재고 업데이트
    public void updateStock(String accessToken, Long flowCode, StockDto.updateStockRequest updateStock) {
        String nickname = jwtUtil.getNickname(accessToken);
        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new StockNotFoundException(flowCode));

        // 변경 전 값 저장
        ChangeTracker tracker = new ChangeTracker("재고 수정");
        ProductSnapshot product = stock.getProduct();

        // 사이즈/중량 변경 추적
        tracker.track("사이즈", product.getSize(), updateStock.getProductSize());
        tracker.track("금중량", product.getGoldWeight(), new BigDecimal(updateStock.getGoldWeight()));
        tracker.track("스톤중량", product.getStoneWeight(), new BigDecimal(updateStock.getStoneWeight()));

        // 비용 관련 변경 추적
        tracker.track("매입비용", product.getProductPurchaseCost(), updateStock.getProductPurchaseCost());
        tracker.track("공임비", product.getProductLaborCost(), updateStock.getProductLaborCost());
        tracker.track("추가공임", product.getProductAddLaborCost(), updateStock.getProductAddLaborCost());
        tracker.track("스톤추가공임", stock.getStoneAddLaborCost(), updateStock.getStoneAddLaborCost());

        // 메모 관련 변경 추적
        tracker.track("메인스톤메모", stock.getStockMainStoneNote(), updateStock.getMainStoneNote());
        tracker.track("보조스톤메모", stock.getStockAssistanceStoneNote(), updateStock.getAssistanceStoneNote());
        tracker.track("재고메모", stock.getStockNote(), updateStock.getStockNote());

        // 보조석 변경 추적 (ID로 비교, 이름으로 표시)
        Long assistantId = Long.valueOf(updateStock.getAssistantStoneId());
        if (!product.getAssistantStoneId().equals(assistantId)) {
            tracker.track("보조석", product.getAssistantStoneName(), updateStock.getAssistantStoneName());
        }

        // 실제 업데이트 수행
        stock.updateStockNote(updateStock.getMainStoneNote(), updateStock.getAssistanceStoneNote(), updateStock.getStockNote());
        product.updateProductCost(updateStock.getProductPurchaseCost(), updateStock.getProductLaborCost(), updateStock.getProductAddLaborCost());
        product.updateProductWeightAndSize(updateStock.getProductSize(), new BigDecimal(updateStock.getGoldWeight()), new BigDecimal(updateStock.getStoneWeight()));

        int[] countStoneCost = countStoneCost(stock.getOrderStones());
        stock.updateStoneCost(countStoneCost[0], countStoneCost[1], countStoneCost[2], countStoneCost[3], updateStock.getStoneAddLaborCost());

        if (!product.getAssistantStoneId().equals(assistantId)) {
            OffsetDateTime assistantStoneCreateAt = null;
            if (StringUtils.hasText(updateStock.getAssistantStoneCreateAt())) {
                assistantStoneCreateAt = DateConversionUtil.StringToOffsetDateTime(updateStock.getAssistantStoneCreateAt());
            }
            product.updateAssistantStone(updateStock.isAssistantStone(), assistantId, updateStock.getAssistantStoneName(), assistantStoneCreateAt);
        }

        updateStockStoneInfo(updateStock.getStoneInfos(), stock);

        // statusHistory 추가 (변경 내용 포함)
        statusHistoryHelper.savePhaseChangeFromLast(
                stock.getFlowCode(),
                BusinessPhase.STOCK,
                tracker.buildContent(),
                nickname
        );
    }

    //주문 -> 재고 변경
    public void updateOrderToStock(String accessToken, Long flowCode, String orderType, StockDto.StockRegisterRequest stockDto) {
        String nickname = jwtUtil.getNickname(accessToken);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new OrderNotFoundException(flowCode));

        if (stockRepository.existsByOrder(order)) {
            throw new IllegalArgumentException(READY_TO_EXPECT);
        }

        if (stockRepository.existsByFlowCode(order.getFlowCode())) {
            throw new IllegalArgumentException(READY_TO_EXPECT);
        }

        AssistantStoneDto.Response assistantStoneInfo = assistantStoneClient.getAssistantStoneInfo(accessToken, Long.valueOf(stockDto.getAssistantStoneId()));

        BigDecimal goldWeight = new BigDecimal(stockDto.getGoldWeight());
        BigDecimal stoneWeight = new BigDecimal(stockDto.getStoneWeight());

        OrderProduct orderProduct = order.getOrderProduct();
        ProductSnapshot product = ProductSnapshot.builder()
                .id(orderProduct.getProductId())
                .productName(orderProduct.getProductName())
                .productFactoryName(orderProduct.getProductFactoryName())
                .size(stockDto.getProductSize())
                .isProductWeightSale(stockDto.isProductWeightSale())
                .productLaborCost(orderProduct.getProductLaborCost())
                .productAddLaborCost(stockDto.getProductAddLaborCost())
                .productPurchaseCost(stockDto.getProductPurchaseCost())
                .materialId(orderProduct.getMaterialId())
                .materialName(orderProduct.getMaterialName())
                .setTypeId(orderProduct.getSetTypeId())
                .setTypeName(orderProduct.getSetTypeName())
                .classificationId(orderProduct.getClassificationId())
                .classificationName(orderProduct.getClassificationName())
                .colorId(orderProduct.getColorId())
                .colorName(orderProduct.getColorName())
                .assistantStone(stockDto.isAssistantStone())
                .assistantStoneId(assistantStoneInfo.getAssistantStoneId())
                .assistantStoneName(assistantStoneInfo.getAssistantStoneName())
                .assistantStoneCreateAt(stockDto.isAssistantStone() ? StringToOffsetDateTime(stockDto.getAssistantStoneCreateAt()) : null)
                .goldWeight(goldWeight)
                .stoneWeight(stoneWeight)
                .build();

        Stock stock = Stock.builder()
                .orders(order)
                .flowCode(order.getFlowCode())
                .storeId(order.getStoreId())
                .storeName(order.getStoreName())
                .storeHarry(order.getStoreHarry())
                .storeGrade(order.getStoreGrade())
                .factoryId(order.getFactoryId())
                .factoryName(order.getFactoryName())
                .stockMainStoneNote(stockDto.getMainStoneNote()) // 수정 가능
                .stockAssistanceStoneNote(stockDto.getAssistanceStoneNote()) // 수정 가능
                .stockNote(stockDto.getOrderNote()) // 수정 가능
                .stoneAddLaborCost(stockDto.getStoneAddLaborCost())
                .orderStones(new ArrayList<>())
                .orderStatus(OrderStatus.valueOf(orderType))
                .product(product)
                .build();

        stock.setOrder(order);
        stockRepository.save(stock);

        List<StoneDto.StoneInfo> stoneInfos = stockDto.getStoneInfos();
        int[] stoneCosts = updateStoneCosts(stoneInfos);
        int totalStonePurchaseCost = stoneCosts[0];
        int totalStoneLaborCost = stoneCosts[1];
        int mainStoneCost = stoneCosts[2];
        int assistanceStoneCost = stoneCosts[3];

        stock.updateStoneCost(totalStonePurchaseCost, totalStoneLaborCost, mainStoneCost, assistanceStoneCost, stock.getStoneAddLaborCost());

        updateToStockStoneInfo(stockDto.getStoneInfos(), stock);

        order.updateOrderStatus(OrderStatus.valueOf(orderType));

        // statusHistory 추가
        statusHistoryHelper.savePhaseChangeFromLast(
                order.getFlowCode(),
                BusinessPhase.valueOf(orderType),
                "재고 등록",
                nickname
        );
    }

    //재고 등록 -> 생성 시 발생하는 토큰 저장 후 고유 stock_code 발급 후 저장
    public void saveStock(String accessToken, String orderType, StockDto.Request stockDto) {
        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);

        Long productId = Long.valueOf(stockDto.getProductId());
        Long storeId = Long.valueOf(stockDto.getStoreId());
        Long factoryId = Long.valueOf(stockDto.getFactoryId());
        Long materialId = Long.valueOf(stockDto.getMaterialId());
        Long classificationId = Long.valueOf(stockDto.getClassificationId());
        Long colorId = Long.valueOf(stockDto.getColorId());
        Long setTypeId = Long.valueOf(stockDto.getSetTypeId());

        ProductSnapshot product = ProductSnapshot.builder()
                .id(productId)
                .productName(stockDto.getProductName())
                .productFactoryName(stockDto.getProductFactoryName())
                .size(stockDto.getProductSize())
                .classificationId(classificationId)
                .classificationName(stockDto.getClassificationName())
                .setTypeId(setTypeId)
                .setTypeName(stockDto.getSetTypeName())
                .colorId(colorId)
                .colorName(stockDto.getColorName())
                .materialId(materialId)
                .materialName(stockDto.getMaterialName())
                .isProductWeightSale(stockDto.getIsProductWeightSale())
                .goldWeight(stockDto.getGoldWeight())
                .stoneWeight(stockDto.getStoneWeight())
                .productPurchaseCost(stockDto.getProductPurchaseCost())
                .productLaborCost(stockDto.getProductLaborCost())
                .productAddLaborCost(stockDto.getProductAddLaborCost())
                .assistantStoneId(Long.valueOf(stockDto.getAssistantStoneId()))
                .assistantStoneName(stockDto.getAssistantStoneName())
                .build();

        int[] stoneCosts = updateStoneCosts(stockDto.getStoneInfos());
        stoneCosts[1] += stockDto.getStoneAddLaborCost();

        Stock stock = Stock.builder()
                .storeId(storeId)
                .storeName(stockDto.getStoreName())
                .storeGrade(stockDto.getStoreGrade())
                .storeHarry(new BigDecimal(stockDto.getStoreHarry()))
                .factoryId(factoryId)
                .factoryName(stockDto.getFactoryName())
                .stockNote(stockDto.getStockNote())
                .orderStatus(OrderStatus.WAIT)
                .stockMainStoneNote(stockDto.getMainStoneNote())
                .stockAssistanceStoneNote(stockDto.getAssistanceStoneNote())
                .totalStoneLaborCost(stoneCosts[1])
                .totalStonePurchaseCost(stoneCosts[0])
                .stoneMainLaborCost(stoneCosts[2])
                .stoneAssistanceLaborCost(stoneCosts[3])
                .stoneAddLaborCost(stockDto.getStoneAddLaborCost())
                .product(product)
                .orderStones(new ArrayList<>())
                .build();

        List<StoneDto.StoneInfo> stoneInfos = stockDto.getStoneInfos();
        for (StoneDto.StoneInfo stoneInfo : stoneInfos) {
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(Long.valueOf(stoneInfo.getStoneId()))
                    .originStoneName(stoneInfo.getStoneName())
                    .originStoneWeight(new BigDecimal(stoneInfo.getStoneWeight()))
                    .stonePurchaseCost(stoneInfo.getPurchaseCost())
                    .stoneLaborCost(stoneInfo.getLaborCost())
                    .stoneQuantity(stoneInfo.getQuantity())
                    .mainStone(stoneInfo.isMainStone())
                    .includeStone(stoneInfo.isIncludeStone())
                    .build();

            stock.addStockStone(orderStone);
        }

        stockRepository.save(stock);

        // statusHistory 추가
        statusHistoryHelper.saveCreate(
                stock.getStockCode(),
                SourceType.valueOf(orderType),
                BusinessPhase.WAITING,
                "재고 등록",
                nickname
        );

        OffsetDateTime assistantStoneCreateAt = null;
        if (StringUtils.hasText(stockDto.getAssistantStoneCreateAt())) {
            assistantStoneCreateAt = DateConversionUtil.StringToOffsetDateTime(stockDto.getAssistantStoneCreateAt());
        }

        KafkaStockRequest stockRequest = KafkaStockRequest.builder()
                .eventId(UUID.randomUUID().toString())
                .flowCode(stock.getStockCode())
                .tenantId(tenantId)
                .token(accessToken)
                .storeId(storeId)
                .factoryId(factoryId)
                .productId(productId)
                .materialId(materialId)
                .classificationId(classificationId)
                .colorId(colorId)
                .setTypeId(setTypeId)
                .nickname(nickname)
                .assistantStone(stockDto.isAssistantStone())
                .assistantStoneId(Long.valueOf(stockDto.getAssistantStoneId()))
                .assistantStoneCreateAt(assistantStoneCreateAt)
                .build();

        try {
            OutboxEvent outboxEvent = new OutboxEvent(
                    "stock.async.requested",
                    stock.getStockCode().toString(),
                    objectMapper.writeValueAsString(stockRequest),
                    "STOCK_CREATED"
            );

            outboxEventRepository.save(outboxEvent);

            log.info("재고 생성 및 Outbox 저장 완료. StockCode: {}, EventID: {}",
                    stock.getStockCode(), outboxEvent.getId());

            eventPublisher.publishEvent(new OutboxCreatedEvent(tenantId));

        } catch (Exception e) {
            log.error("Outbox 저장 실패. StockCode: {}", stock.getStockCode(), e);
            throw new IllegalStateException("재고 생성 이벤트 저장 실패", e);
        }
    }

    //재고 -> 대여
    public void stockToRental(String accessToken, Long flowCode, StockDto.StockRentalRequest stockRentalDto) {
        String nickname = jwtUtil.getNickname(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new StockNotFoundException(flowCode));

        if (stock.getOrderStatus() != OrderStatus.NORMAL && stock.getOrderStatus() != OrderStatus.STOCK) {
            throw new IllegalArgumentException("일반 또는 재고 상태의 상품만 대여로 전환할 수 있습니다. (시리얼: " + stock.getFlowCode() + ")");
        }

        updateStockStoneInfo(stockRentalDto.getStoneInfos(), stock);

        int[] countStoneCost = updateStoneCosts(stockRentalDto.getStoneInfos());
        stock.updateStoneCost(countStoneCost[0], countStoneCost[1], countStoneCost[2], countStoneCost[3], stockRentalDto.getStoneAddLaborCost());                stock.moveToRental(stockRentalDto);

        // statusHistory 추가
        statusHistoryHelper.savePhaseChangeFromLast(
                stock.getFlowCode(),
                BusinessPhase.RENTAL,
                "재고 대여",
                nickname
        );

    }

    // 재고 -> 주문 (삭제)
    // 삭제 시 statusHistory를 분기하여 삭제 재고와 주문 제품 모두 이력을 유지합니다.
    public void stockToDelete(String accessToken, Long flowCode) {
        String nickname = jwtUtil.getNickname(accessToken);
        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new StockNotFoundException(flowCode));

        Long originalFlowCode = stock.getFlowCode();

        // 마지막 상태 이력 조회 (삭제 이력 추가를 위해)
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(originalFlowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        // 삭제 재고용 신규 flowCode 생성
        stock.updateFlowCode();
        Long newDeletedFlowCode = stock.getFlowCode();

        statusHistoryHelper.copyAllHistories(originalFlowCode, newDeletedFlowCode);

        // 삭제 재고에 삭제 이력 추가
        statusHistoryHelper.savePhaseChange(
                newDeletedFlowCode,
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                BusinessPhase.DELETED,
                "재고 삭제",
                nickname
        );

        // Order 연결 해제 및 상태 업데이트 (Order는 기존 flowCode 유지)
        Orders associatedOrder = stock.getOrder();
        if (associatedOrder != null) {
            associatedOrder.updateProductStatus(ProductStatus.WAITING);
            associatedOrder.updateOrderStatus(OrderStatus.ORDER);
        }

        stock.unlinkOrder();
        stock.updateOrderStatus(OrderStatus.DELETED);
    }

    // 대여 -> 반납
    public void rentalToReturn(String accessToken, Long flowCode, String orderType) {
        String nickname = jwtUtil.getNickname(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new StockNotFoundException(flowCode));

        if (stock.getOrderStatus() != OrderStatus.RENTAL) {
            throw new InvalidOrderStatusException(WRONG_STATUS);
        }

        stock.updateOrderStatus(OrderStatus.RETURN);

        // statusHistory 추가
        statusHistoryHelper.savePhaseChangeFromLast(
                stock.getFlowCode(),
                BusinessPhase.RETURN,
                "대여 반납",
                nickname
        );
    }

    // 반납 && 삭제 -> 재고
    public void rollBackStock(String accessToken, Long flowCode, String orderType) {
        String nickname = jwtUtil.getNickname(accessToken);

        OrderStatus target = OrderStatus.valueOf(orderType);
        if (target != OrderStatus.RETURN && target != OrderStatus.DELETED) {
            throw new InvalidOrderStatusException(WRONG_STATUS);
        }

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new StockNotFoundException(flowCode));

        stock.updateOrderStatus(OrderStatus.STOCK);

        // statusHistory 추가
        statusHistoryHelper.savePhaseChangeFromLast(
                stock.getFlowCode(),
                BusinessPhase.STOCK,
                "반납 재고",
                nickname
        );
    }

    @Transactional(readOnly = true)
    public List<String> getFilterFactories(String startAt, String endAt, String orderStatus) {
        StockDto.StockCondition condition = new StockDto.StockCondition(startAt, endAt, orderStatus);
        return customStockRepository.findByFilterFactories(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterStores(String startAt, String endAt, String orderStatus) {
        StockDto.StockCondition condition = new StockDto.StockCondition(startAt, endAt, orderStatus);
        return customStockRepository.findByFilterStores(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterSetType(String startAt, String endAt, String orderStatus) {
        StockDto.StockCondition condition = new StockDto.StockCondition(startAt, endAt, orderStatus);
        return customStockRepository.findByFilterSetType(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterColors(String startAt, String endAt, String orderStatus) {
        StockDto.StockCondition condition = new StockDto.StockCondition(startAt, endAt, orderStatus);
        return customStockRepository.findByFilterColor(condition);
    }

    // ==================== 재고 조사 관련 메서드 ====================

    /**
     * 재고 조사 목록 조회
     */
    @Transactional(readOnly = true)
    public CustomPage<InventoryDto.Response> getInventoryStocks(
            String searchField, String searchValue,
            String sortField, String sortOrder,
            String stockChecked, String orderStatus,
            String materialName, Pageable pageable) {

        InventoryDto.Condition condition = InventoryDto.Condition.builder()
                .searchField(searchField)
                .searchValue(searchValue)
                .sortField(sortField)
                .sortOrder(sortOrder)
                .stockChecked(stockChecked)
                .orderStatus(orderStatus)
                .materialName(materialName)
                .build();

        return customStockRepository.findInventoryStocks(condition, pageable);
    }

    /**
     * 재고 조사 재질 필터 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getInventoryMaterials() {
        return customStockRepository.findInventoryMaterials();
    }

    /**
     * 재고 조사 준비 (초기화)
     * 모든 재고의 조사 상태를 초기화합니다.
     */
    public InventoryDto.ResetResponse prepareInventoryCheck() {
        int resetCount = customStockRepository.resetAllStockChecks();

        return InventoryDto.ResetResponse.builder()
                .resetCount(resetCount)
                .message("재고 조사가 초기화되었습니다. 총 " + resetCount + "건의 재고가 준비되었습니다.")
                .build();
    }

    /**
     * 재고 조사 처리 (바코드 스캔)
     * flowCode로 재고를 조회하여 재고 조사 처리합니다.
     */
    public InventoryDto.CheckResponse checkStock(Long flowCode) {
        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElse(null);

        // 재고가 존재하지 않는 경우
        if (stock == null) {
            return InventoryDto.CheckResponse.builder()
                    .flowCode(flowCode.toString())
                    .productName(null)
                    .status("NOT_FOUND")
                    .message("해당 시리얼의 재고를 찾을 수 없습니다.")
                    .stockCheckedAt(null)
                    .build();
        }

        String productName = stock.getProduct() != null ? stock.getProduct().getProductName() : "알 수 없음";

        // 재고 조사 가능 상태 확인 (STOCK, RENTAL, RETURN, NORMAL)
        OrderStatus status = stock.getOrderStatus();
        if (status != OrderStatus.STOCK && status != OrderStatus.RENTAL &&
            status != OrderStatus.RETURN && status != OrderStatus.NORMAL) {
            return InventoryDto.CheckResponse.builder()
                    .flowCode(flowCode.toString())
                    .productName(productName)
                    .status("NOT_CHECKABLE")
                    .message("재고 조사 불가 상태입니다. 현재 상태: " + status.name())
                    .stockCheckedAt(null)
                    .build();
        }

        // 이미 재고 조사가 완료된 경우
        if (Boolean.TRUE.equals(stock.getStockChecked())) {
            return InventoryDto.CheckResponse.builder()
                    .flowCode(flowCode.toString())
                    .productName(productName)
                    .status("ALREADY_CHECKED")
                    .message("이미 재고 조사가 완료된 상품입니다.")
                    .stockCheckedAt(stock.getStockCheckedAt() != null ? stock.getStockCheckedAt().toString() : null)
                    .build();
        }

        // 재고 조사 처리
        stock.markAsChecked();

        return InventoryDto.CheckResponse.builder()
                .flowCode(flowCode.toString())
                .productName(productName)
                .status("SUCCESS")
                .message("재고 조사가 완료되었습니다.")
                .stockCheckedAt(stock.getStockCheckedAt().toString())
                .build();
    }

    /**
     * 재고 조사 통계 조회
     * 검사한 재고와 검사하지 않은 재고의 재질별 통계를 반환합니다.
     */
    @Transactional(readOnly = true)
    public InventoryDto.StatisticsResponse getInventoryStatistics() {
        // 미검사 재고 통계
        List<InventoryDto.MaterialStatistics> uncheckedStats = customStockRepository.findInventoryStatistics(false);
        // 검사 재고 통계
        List<InventoryDto.MaterialStatistics> checkedStats = customStockRepository.findInventoryStatistics(true);

        // 미검사 합계 계산
        InventoryDto.StatisticsSummary uncheckedSummary = calculateSummary(uncheckedStats);
        // 검사 합계 계산
        InventoryDto.StatisticsSummary checkedSummary = calculateSummary(checkedStats);

        return InventoryDto.StatisticsResponse.builder()
                .uncheckedStatistics(uncheckedStats)
                .checkedStatistics(checkedStats)
                .uncheckedSummary(uncheckedSummary)
                .checkedSummary(checkedSummary)
                .build();
    }

    private InventoryDto.StatisticsSummary calculateSummary(List<InventoryDto.MaterialStatistics> statistics) {
        double totalWeight = 0.0;
        int totalQuantity = 0;
        long totalPurchaseCost = 0;

        for (InventoryDto.MaterialStatistics stat : statistics) {
            if (stat.getTotalGoldWeight() != null && !stat.getTotalGoldWeight().isEmpty()) {
                try {
                    totalWeight += Double.parseDouble(stat.getTotalGoldWeight());
                } catch (NumberFormatException ignored) {}
            }
            if (stat.getQuantity() != null) {
                totalQuantity += stat.getQuantity();
            }
            if (stat.getTotalPurchaseCost() != null) {
                totalPurchaseCost += stat.getTotalPurchaseCost();
            }
        }

        return InventoryDto.StatisticsSummary.builder()
                .totalGoldWeight(String.format("%.3f", totalWeight))
                .totalQuantity(totalQuantity)
                .totalPurchaseCost(totalPurchaseCost)
                .build();
    }
}
