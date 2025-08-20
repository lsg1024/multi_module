package com.msa.order.local.domain.stock.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.global.kafka.dto.KafkaStockRequest;
import com.msa.order.local.domain.order.dto.StoreDto;
import com.msa.order.local.domain.order.entity.OrderProduct;
import com.msa.order.local.domain.order.entity.OrderStone;
import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.order.entity.StatusHistory;
import com.msa.order.local.domain.order.entity.order_enum.OrderStatus;
import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import com.msa.order.local.domain.order.external_client.StoreClient;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import com.msa.order.local.domain.order.repository.StatusHistoryRepository;
import com.msa.order.local.domain.stock.dto.StockDto;
import com.msa.order.local.domain.stock.entity.domain.ProductSnapshot;
import com.msa.order.local.domain.stock.entity.domain.Stock;
import com.msa.order.local.domain.stock.repository.CustomStockRepository;
import com.msa.order.local.domain.stock.repository.StockRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.READY_TO_EXPECT;
import static com.msa.order.local.domain.order.util.StoneUtil.*;

@Service
@Transactional
public class StockService {

    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final StoreClient storeClient;
    private final StockRepository stockRepository;
    private final OrdersRepository ordersRepository;
    private final CustomStockRepository customStockRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public StockService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, StoreClient storeClient, StockRepository stockRepository, OrdersRepository ordersRepository, CustomStockRepository customStockRepository, StatusHistoryRepository statusHistoryRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.storeClient = storeClient;
        this.stockRepository = stockRepository;
        this.ordersRepository = ordersRepository;
        this.customStockRepository = customStockRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    // 재고 관리 get 전체 재고 + 주문, 수리, 대여 관련
    @Transactional(readOnly = true)
    public CustomPage<StockDto.Response> getStocks(String inputSearch, StockDto.StockCondition condition, Pageable pageable) {
        return customStockRepository.findByStockProducts(inputSearch, condition, pageable);
    }

    //주문 -> 재고 변경
    public void updateOrderStatusToStock(String accessToken, Long flowCode, StockDto.orderStockRequest stockDto) {
        String nickname = jwtUtil.getNickname(accessToken);
        Orders order = ordersRepository.findAggregate(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (stockRepository.existsByOrder(order)) {
            throw new IllegalArgumentException(READY_TO_EXPECT);
        }

        if (stockRepository.existsByFlowCode(order.getFlowCode())) {
            throw new IllegalArgumentException(READY_TO_EXPECT);
        }

        OrderProduct orderProduct = order.getOrderProduct();
        ProductSnapshot product = ProductSnapshot.builder()
                .id(orderProduct.getProductId())
                .name(orderProduct.getProductName())
                .size(stockDto.getProductSize())
                .classificationName(orderProduct.getClassificationName())
                .isProductWeightSale(stockDto.isProductWeightSale())
                .laborCost(orderProduct.getProductLaborCost())
                .addLaborCost(stockDto.getAddProductLaborCost())
                .productPurchaseCost(stockDto.getProductPurchaseCost())
                .materialName(orderProduct.getMaterialName())
                .classificationName(orderProduct.getClassificationName())
                .colorName(orderProduct.getColorName())
                .productWeight(stockDto.getProductWeight())
                .stoneWeight(stockDto.getStoneWeight())
                .build();

        // 전체 비용 + 스톤 비용 + 추가 스톤 비용 계산
        Stock stock = Stock.builder()
                .orders(order)
                .flowCode(order.getFlowCode())
                .storeId(order.getStoreId())
                .storeName(order.getStoreName())
                .factoryId(order.getFactoryId())
                .factoryName(order.getFactoryName())
                .stockMainStoneNote(stockDto.getMainStoneNote())
                .stockAssistanceStoneNote(stockDto.getAssistanceStoneNote())
                .stockNote(stockDto.getStockNote())
                .stonePurchaseCost(stockDto.getStonePurchaseCost())
                .addStoneLaborCost(stockDto.getAddStoneLaborCost())
                .orderStones(new ArrayList<>())
                .orderStatus(OrderStatus.STOCK)
                .product(product)
                .build();

        stock.setOrder(order);
        stockRepository.save(stock);

        updateStoneInfo(stockDto, order, stock);
        updateStoneCostAndPurchase(stock);

        order.updateOrderStatus(OrderStatus.STOCK);
        order.updateProductStatus(ProductStatus.EXPECT);

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory = StatusHistory.phaseChange(
                order.getFlowCode(),
                lastHistory.getSourceType(),
                lastHistory.getPhase(),
                StatusHistory.BusinessPhase.STOCK,
                nickname
        );

        statusHistoryRepository.save(statusHistory);
    }

    // 재고 등록 -> 생성 시 발생하는 토큰 저장 후 고유 stock_code 발급 후 저장
    public void saveStock(String accessToken, String orderType, StockDto.createStockRequest stockDto) {
        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = TenantContext.getTenant();

        Long productId = Long.valueOf(stockDto.getProductId());
        Long storeId = Long.valueOf(stockDto.getStoreId());
        Long factoryId = Long.valueOf(stockDto.getFactoryId());
        Long materialId = Long.valueOf(stockDto.getMaterialId());
        Long classificationId = Long.valueOf(stockDto.getClassificationId());
        Long colorId = Long.valueOf(stockDto.getColorId());

        ProductSnapshot product = ProductSnapshot.builder()
                .id(productId)
                .size(stockDto.getProductSize())
                .addLaborCost(stockDto.getProductAddLaborCost())
                .productWeight(stockDto.getProductWeight())
                .stoneWeight(stockDto.getStoneWeight())
                .build();

        Stock stock = Stock.builder()
                .stockNote(stockDto.getStockNote())
                .orderStatus(OrderStatus.valueOf(orderType))
                .stockMainStoneNote(stockDto.getMainStoneNote())
                .stockAssistanceStoneNote(stockDto.getAssistanceStoneNote())
                .product(product)
                .orderStones(new ArrayList<>())
                .build();

        List<Long> stoneIds = new ArrayList<>();
        List<StockDto.StoneInfo> stoneInfos = stockDto.getStoneInfos();
        for (StockDto.StoneInfo stoneInfo : stoneInfos) {
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(Long.valueOf(stoneInfo.getStoneId()))
                    .originStoneName(stoneInfo.getStoneName())
                    .originStoneWeight(new BigDecimal(stoneInfo.getStoneWeight()))
                    .stonePurchaseCost(stoneInfo.getPurchaseCost())
                    .stoneLaborCost(stoneInfo.getLaborCost())
                    .stoneQuantity(stoneInfo.getQuantity())
                    .productStoneMain(stoneInfo.isProductStoneMain())
                    .includeQuantity(stoneInfo.isIncludeQuantity())
                    .includeWeight(stoneInfo.isIncludeWeight())
                    .includeLabor(stoneInfo.isIncludeLabor())
                    .build();

            stoneIds.add(Long.valueOf(stoneInfo.getStoneId()));
            stock.addStockStone(orderStone);
        }

        stockRepository.save(stock);

        StatusHistory statusHistory = StatusHistory.create(
                stock.getStockCode(),
                StatusHistory.SourceType.NORMAL,
                StatusHistory.BusinessPhase.NORMAL,
                StatusHistory.Kind.CREATE,
                nickname
        );

        statusHistoryRepository.save(statusHistory);

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
                .nickname(nickname)
                .productStatus(orderType)
                .stoneIds(stoneIds)
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.stockDetailAsync(stockRequest);
            }
        });
    }

    // 재고 -> 대여
    public void stockToRental(String accessToken, Long flowCode, StockDto.StockRentalRequest stockRentalDto) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        String nickname = jwtUtil.getNickname(accessToken);
        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory beforeStatusHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StoreDto.Response storeInfo = storeClient.getStoreInfo(tenantId, Long.valueOf(stockRentalDto.getStoreId()));
        stock.updateStore(storeInfo);

        // 1) stone Update 필요 여부
        Map<Long, OrderStone> existingById = stock.getOrderStones().stream()
                .filter(s -> s.getOrderStoneId() != null)
                .collect(Collectors.toMap(OrderStone::getOriginStoneId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        for (StockDto.StoneInfo stoneInfo : stockRentalDto.getStoneInfos()) {
            Long id = Long.valueOf(stoneInfo.getStoneId());
            if (id != null && existingById.containsKey(id)) {
                existingById.get(id).updateFrom(stoneInfo);
                keepIds.add(id);
            } else {
                OrderStone orderStone = OrderStone.builder()
                        .originStoneId(Long.valueOf(stoneInfo.getStoneId()))
                        .originStoneName(stoneInfo.getStoneName())
                        .originStoneWeight(new BigDecimal(stoneInfo.getStoneWeight()))
                        .stonePurchaseCost(stoneInfo.getPurchaseCost())
                        .stoneLaborCost(stoneInfo.getLaborCost())
                        .stoneQuantity(stoneInfo.getQuantity())
                        .productStoneMain(stoneInfo.isProductStoneMain())
                        .includeQuantity(stoneInfo.isIncludeQuantity())
                        .includeWeight(stoneInfo.isIncludeWeight())
                        .includeLabor(stoneInfo.isIncludeLabor())
                        .build();

                orderStone.setStock(stock);
                stock.addStockStone(orderStone);
                if (orderStone.getOrderStoneId() != null) keepIds.add(orderStone.getOrderStoneId());
            }
        }

        stock.getOrderStones().removeIf(os ->
                os.getOrderStoneId() != null && !keepIds.contains(os.getOrderStoneId()));

        // 2) stone Cost
        int sum = 0;
        sum += (stockRentalDto.getAddStoneLaborCost() != null ? stockRentalDto.getAddStoneLaborCost() : 0);
        for (OrderStone orderStone : stock.getOrderStones()) {
            if (Boolean.TRUE.equals(orderStone.getIncludeLabor())) {
                sum += (orderStone.getStoneLaborCost() != null ? orderStone.getStoneLaborCost() : 0);
            }
        }

        stock.moveToRental(stockRentalDto);

        statusHistoryRepository.save(StatusHistory.phaseChange(
                stock.getFlowCode(),
                beforeStatusHistory.getSourceType(),
                StatusHistory.BusinessPhase.valueOf(beforeStatusHistory.getFromValue()),
                StatusHistory.BusinessPhase.RENTAL,
                nickname
        ));
    }

    // 재고 -> 주문 (삭제)

    // 삭제 재고 -> 재고

    // 재고 -> 판매

    private static void updateStoneInfo(StockDto.orderStockRequest stockDto, Orders order, Stock stock) {
        Map<Long, OrderStone> stoneId = order.getOrderStones().stream()
                .filter(os -> os.getOrderStoneId() != null)
                .collect(Collectors.toMap(OrderStone::getOrderStoneId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        if (stockDto.getStoneInfos() != null) {
            for (StockDto.StoneInfo s : stockDto.getStoneInfos()) {
                Long osId = parseLongOrNull(s.getStoneId());
                if (osId != null) {
                    OrderStone os = stoneId.get(osId);
                    if (os == null) {
                        throw new IllegalArgumentException("잘못된 orderStoneId: " + osId);
                    }
                    if (isChanged(os, s)) {
                        os.updateFrom(s); // 필드 일괄 업데이트
                    }
                    os.setStock(stock);
                    stock.addStockStone(os);
                    keepIds.add(osId);
                } else {
                    // 신규 스톤 추가(주문에는 없었던 항목)
                    OrderStone os = OrderStone.builder()
                            .originStoneId(Long.valueOf(s.getStoneId()))
                            .originStoneName(s.getStoneName())
                            .originStoneWeight(new BigDecimal(s.getStoneWeight()))
                            .stonePurchaseCost(s.getPurchaseCost())
                            .stoneLaborCost(s.getLaborCost())
                            .stoneQuantity(s.getQuantity())
                            .productStoneMain(s.isProductStoneMain())
                            .includeQuantity(s.isIncludeQuantity())
                            .includeWeight(s.isIncludeWeight())
                            .includeLabor(s.isIncludeLabor())
                            .build();
                    os.setStock(stock);
                    stock.addStockStone(os);
                }
            }
        }

        stock.getOrderStones().removeIf(os ->
                os.getOrderStoneId() != null && !keepIds.contains(os.getOrderStoneId()));
    }

    private static void updateStoneCostAndPurchase(Stock stock) {
        int totalStonePurchaseCost = 0;
        int mainStoneCost = 0;
        int assistanceStoneCost = 0;

        for (OrderStone os : stock.getOrderStones()) {
            int qty = nvl(os.getStoneQuantity());
            int labor = nvl(os.getStoneLaborCost());
            int purchase = nvl(os.getStonePurchaseCost());
            if (Boolean.TRUE.equals(os.getIncludeQuantity())) {
                if (Boolean.TRUE.equals(os.getProductStoneMain())) {
                    mainStoneCost += labor * qty;
                } else {
                    assistanceStoneCost += labor * qty;
                }
                totalStonePurchaseCost += purchase * qty;
            }
        }
        stock.updateStonePurchaseCost(totalStonePurchaseCost, mainStoneCost, assistanceStoneCost);
    }

}
