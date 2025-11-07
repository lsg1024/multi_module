package com.msa.order.local.stock.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.global.kafka.dto.KafkaStockRequest;
import com.msa.order.global.util.DateConversionUtil;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.entity.OrderProduct;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.Kind;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.SourceType;
import com.msa.order.local.order.external_client.AssistantStoneClient;
import com.msa.order.local.order.external_client.dto.AssistantStoneDto;
import com.msa.order.local.order.repository.OrdersRepository;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.entity.ProductSnapshot;
import com.msa.order.local.stock.entity.Stock;
import com.msa.order.local.stock.repository.CustomStockRepository;
import com.msa.order.local.stock.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.msa.order.global.exception.ExceptionMessage.*;
import static com.msa.order.local.order.util.StoneUtil.*;

@Slf4j
@Service
@Transactional
public class StockService {
    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final AssistantStoneClient assistantStoneClient;
    private final StockRepository stockRepository;
    private final OrdersRepository ordersRepository;
    private final CustomStockRepository customStockRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public StockService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, AssistantStoneClient assistantStoneClient, StockRepository stockRepository, OrdersRepository ordersRepository, CustomStockRepository customStockRepository, StatusHistoryRepository statusHistoryRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.assistantStoneClient = assistantStoneClient;
        this.stockRepository = stockRepository;
        this.ordersRepository = ordersRepository;
        this.customStockRepository = customStockRepository;
        this.statusHistoryRepository = statusHistoryRepository;
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
                    .assistantStoneCreateAt(String.valueOf(stock.getProduct().getAssistantStoneCreateAt()))
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

        return customStockRepository.findByStockProducts(inputCondition, condition, pageable);
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
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        stock.updateStockNote(updateStock.getMainStoneNote(), updateStock.getAssistanceStoneNote(), updateStock.getStockNote());
        stock.getProduct().updateProductCost(updateStock.getProductPurchaseCost(), updateStock.getProductLaborCost(), updateStock.getProductAddLaborCost());
        stock.getProduct().updateProductWeightAndSize(updateStock.getProductSize(), new BigDecimal(updateStock.getGoldWeight()), new BigDecimal(updateStock.getStoneWeight()));

        int[] countStoneCost = countStoneCost(stock.getOrderStones());
        stock.updateStoneCost(countStoneCost[0], countStoneCost[1], countStoneCost[2], countStoneCost[3], updateStock.getStoneAddLaborCost());

        Long assistantId = Long.valueOf(updateStock.getAssistantStoneId());
        if (!stock.getProduct().getAssistantStoneId().equals(assistantId)) {
            OffsetDateTime assistantStoneCreateAt = null;
            if (!StringUtils.hasText(updateStock.getAssistantStoneCreateAt())) {
                assistantStoneCreateAt = DateConversionUtil.StringToOffsetDateTime(updateStock.getAssistantStoneCreateAt());
            }
            stock.getProduct().updateAssistantStone(updateStock.isAssistantStone(), assistantId, updateStock.getAssistantStoneName(), assistantStoneCreateAt);
        }

        updateStockStoneInfo(updateStock.getStoneInfos(), stock);

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(stock.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory = StatusHistory.phaseChange(
                stock.getFlowCode(),
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                BusinessPhase.STOCK,
                nickname
        );

        statusHistoryRepository.save(statusHistory);
    }

    //주문 -> 재고 변경
    public void updateOrderToStock(String accessToken, Long flowCode, String orderType, StockDto.StockRegisterRequest stockDto) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        String nickname = jwtUtil.getNickname(accessToken);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (stockRepository.existsByOrder(order)) {
            throw new IllegalArgumentException(READY_TO_EXPECT);
        }

        if (stockRepository.existsByFlowCode(order.getFlowCode())) {
            throw new IllegalArgumentException(READY_TO_EXPECT);
        }

        AssistantStoneDto.Response assistantStoneInfo = assistantStoneClient.getAssistantStoneInfo(tenantId, Long.valueOf(stockDto.getAssistantStoneId()));

        OffsetDateTime assistantStoneCreateAt = DateConversionUtil.StringToOffsetDateTime(stockDto.getAssistantStoneCreateAt());

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
                .assistantStoneCreateAt(assistantStoneCreateAt)
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
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory = StatusHistory.phaseChange(
                order.getFlowCode(),
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                BusinessPhase.valueOf(orderType),
                nickname
        );

        statusHistoryRepository.save(statusHistory);
    }

    //재고 등록 -> 생성 시 발생하는 토큰 저장 후 고유 stock_code 발급 후 저장
    public void saveStock(String accessToken, String orderType, StockDto.Request stockDto) {
        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = TenantContext.getTenant();

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

        StatusHistory statusHistory = StatusHistory.create(
                stock.getStockCode(),
                SourceType.valueOf(orderType),
                BusinessPhase.WAITING,
                Kind.CREATE,
                nickname
        );

        statusHistoryRepository.save(statusHistory);

        OffsetDateTime assistantStoneCreateAt = DateConversionUtil.StringToOffsetDateTime(stockDto.getAssistantStoneCreateAt());

        KafkaStockRequest stockRequest = KafkaStockRequest.builder()
                .eventId(UUID.randomUUID().toString())
                .flowCode(stock.getStockCode())
                .tenantId(tenantId)
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

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.stockSave(stockRequest);
            }
        });
    }

    //재고 -> 대여
    public void stockToRental(String accessToken, Long flowCode, StockDto.StockRentalRequest stockRentalDto) {
        String nickname = jwtUtil.getNickname(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (stock.getOrderStatus() != OrderStatus.NORMAL && stock.getOrderStatus() != OrderStatus.STOCK) {
            throw new IllegalArgumentException("일반 또는 재고 상태의 상품만 대여로 전환할 수 있습니다. (시리얼: " + stock.getFlowCode() + ")");
        }

        StatusHistory beforeStatusHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(stock.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));


        updateStockStoneInfo(stockRentalDto.getStoneInfos(), stock);

        int[] countStoneCost = updateStoneCosts(stockRentalDto.getStoneInfos());
        stock.updateStoneCost(countStoneCost[0], countStoneCost[1], countStoneCost[2], countStoneCost[3], stockRentalDto.getStoneAddLaborCost());                stock.moveToRental(stockRentalDto);

        StatusHistory statusHistory = StatusHistory.phaseChange(
                stock.getFlowCode(),
                beforeStatusHistory.getSourceType(),
                BusinessPhase.valueOf(beforeStatusHistory.getToValue()),
                BusinessPhase.RENTAL,
                nickname
        );
        statusHistoryRepository.save(statusHistory);

    }

    // 재고 -> 주문 (삭제)
    public void stockToDelete(String accessToken, Long flowCode) {
        String nickname = jwtUtil.getNickname(accessToken);
        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        Long beforeFlowCode = stock.getFlowCode();
        // 일반인 경우 그냥 삭제
        // 주문인 경우 연관관계 제거 후 주문 상태는 WAIT로 변경
        // -> orderStone, history들은 스냅샷 후 별도 저장
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(beforeFlowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        stock.removeOrder(); // order, orderProduct 연관성 제거
        stock.updateOrderStatus(OrderStatus.DELETED);

        List<StatusHistory> allByFlowCode = statusHistoryRepository.findAllByFlowCode(beforeFlowCode);
        for (StatusHistory statusHistory : allByFlowCode) {
            statusHistory.updateFlowCode(stock.getFlowCode());
        }

        StatusHistory deleteStockHistory = StatusHistory.phaseChange(
                stock.getFlowCode(),
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                BusinessPhase.DELETED,
                nickname
        );

        allByFlowCode.add(deleteStockHistory);

        statusHistoryRepository.saveAll(allByFlowCode);
    }

    // 대여 -> 반납
    public void rentalToReturn(String accessToken, Long flowCode, String orderType) {
        String nickname = jwtUtil.getNickname(accessToken);

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (stock.getOrderStatus() != OrderStatus.RENTAL) {
            throw new IllegalArgumentException(WRONG_STATUS);
        }

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(stock.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        stock.updateOrderStatus(OrderStatus.RETURN);
        StatusHistory orderStatusHistory = StatusHistory.phaseChange(
                stock.getFlowCode(),
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                BusinessPhase.RETURN,
                nickname
        );
        statusHistoryRepository.save(orderStatusHistory);
    }

    // 반납 && 삭제 -> 재고
    public void rollBackStock(String accessToken, Long flowCode, String orderType) {
        String nickname = jwtUtil.getNickname(accessToken);

        OrderStatus target = OrderStatus.valueOf(orderType);
        if (target != OrderStatus.RETURN && target != OrderStatus.DELETED) {
            throw new IllegalArgumentException(WRONG_STATUS);
        }

        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(stock.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        stock.updateOrderStatus(OrderStatus.STOCK);
        StatusHistory orderStatusHistory = StatusHistory.phaseChange(
                stock.getFlowCode(),
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                BusinessPhase.STOCK,
                nickname
        );
        statusHistoryRepository.save(orderStatusHistory);
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
}
