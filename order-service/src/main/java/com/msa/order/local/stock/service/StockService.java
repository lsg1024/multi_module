package com.msa.order.local.stock.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.global.kafka.dto.KafkaStockRequest;
import com.msa.order.global.util.DateConversionUtil;
import com.msa.order.local.order.dto.StoreDto;
import com.msa.order.local.order.entity.OrderProduct;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.*;
import com.msa.order.local.order.external_client.AssistantStoneClient;
import com.msa.order.local.order.external_client.StoreClient;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.msa.order.global.exception.ExceptionMessage.*;
import static com.msa.order.local.order.util.StoneUtil.*;

@Slf4j
@Service
@Transactional
public class StockService {
    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final StoreClient storeClient;
    private final AssistantStoneClient assistantStoneClient;
    private final StockRepository stockRepository;
    private final OrdersRepository ordersRepository;
    private final CustomStockRepository customStockRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public StockService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, StoreClient storeClient, AssistantStoneClient assistantStoneClient, StockRepository stockRepository, OrdersRepository ordersRepository, CustomStockRepository customStockRepository, StatusHistoryRepository statusHistoryRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.storeClient = storeClient;
        this.assistantStoneClient = assistantStoneClient;
        this.stockRepository = stockRepository;
        this.ordersRepository = ordersRepository;
        this.customStockRepository = customStockRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    // 재고 상세 조회
    @Transactional(readOnly = true)
    public StockDto.ResponseDetail getDetailStock(Long flowCode) {
        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        StatusHistory statusHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        int mainStoneQuantity = 0;
        int assistanceStoneQuantity = 0;
        List<OrderStone> orderStones = stock.getOrderStones();
        countStoneQuantity(orderStones, mainStoneQuantity, assistanceStoneQuantity);

        return StockDto.ResponseDetail.builder()
                .flowCode(stock.getFlowCode().toString())
                .createAt(stock.getStockCreateAt().toString())
                .originalProductStatus(statusHistory.getSourceType().getDisplayName())
                .classificationName(stock.getProduct().getClassificationName())
                .productName(stock.getProduct().getProductName())
                .storeName(stock.getStoreName())
                .materialName(stock.getProduct().getMaterialName())
                .colorName(stock.getProduct().getColorName())
                .mainStoneNote(stock.getStockMainStoneNote())
                .assistanceStoneNote(stock.getStockAssistanceStoneNote())
                .productSize(stock.getProduct().getSize())
                .stockNote(stock.getStockNote())
                .productLaborCost(stock.getProduct().getLaborCost())
                .productAddLaborCost(stock.getProduct().getAddLaborCost())
                .mainStoneLaborCost(stock.getMainStoneLaborCost())
                .assistanceStoneLaborCost(stock.getAssistanceStoneLaborCost())
                .mainStoneQuantity(mainStoneQuantity)
                .assistanceStoneQuantity(assistanceStoneQuantity)
                .goldWeight(stock.getProduct().getGoldWeight().toPlainString())
                .stoneWeight(stock.getProduct().getStoneWeight().toPlainString())
                .productPurchaseCost(stock.getProduct().getProductPurchaseCost())
                .stonePurchaseCost(stock.getTotalStonePurchaseCost()).build();
    }

    // 재고 관리  주문, 수리, 대여 관련
    @Transactional(readOnly = true)
    public CustomPage<StockDto.Response> getStocks(String inputSearch, String orderType, StockDto.StockCondition condition, Pageable pageable) {
        return customStockRepository.findByStockProducts(inputSearch, orderType, condition, pageable);
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

        BigDecimal totalStoneWeight = stockDto.getStoneInfos().stream()
                .filter(StoneDto.StoneInfo::isIncludeStone)
                .map(StoneDto.StoneInfo::getStoneWeight)
                .filter(Objects::nonNull)
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal goldWeight = new BigDecimal(stockDto.getGoldWeight());

        OrderProduct orderProduct = order.getOrderProduct();
        ProductSnapshot product = ProductSnapshot.builder()
                .id(orderProduct.getProductId()) //변경 불가
                .productName(orderProduct.getProductName())
                .productFactoryName(orderProduct.getProductFactoryName())
                .size(stockDto.getProductSize())
                .classificationName(orderProduct.getClassificationName())
                .isProductWeightSale(stockDto.isProductWeightSale())
                .laborCost(orderProduct.getProductLaborCost())
                .addLaborCost(stockDto.getProductAddLaborCost())
                .productPurchaseCost(stockDto.getProductPurchaseCost())
                .materialName(orderProduct.getMaterialName())
                .setTypeName(orderProduct.getSetTypeName())
                .classificationName(orderProduct.getClassificationName())
                .colorName(orderProduct.getColorName())
                .assistantStone(stockDto.isAssistantStone())
                .assistantStoneId(assistantStoneInfo.getAssistantStoneId())
                .assistantStoneName(assistantStoneInfo.getAssistantStoneName())
                .assistantStoneCreateAt(assistantStoneCreateAt)
                .goldWeight(goldWeight)
                .stoneWeight(totalStoneWeight)
                .build();

        // 전체 비용 + 스톤 비용 + 추가 스톤 비용 계산
        int totalStonePurchaseCost = stockDto.getStoneInfos().stream()
                .filter(StoneDto.StoneInfo::isIncludeStone)
                .map(StoneDto.StoneInfo::getPurchaseCost)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        int totalStoneLaborCost = stockDto.getStoneInfos().stream()
                .filter(StoneDto.StoneInfo::isIncludeStone)
                .map(StoneDto.StoneInfo::getLaborCost)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        Stock stock = Stock.builder()
                .orders(order)
                .flowCode(order.getFlowCode())
                .storeId(order.getStoreId())
                .storeName(order.getStoreName())
                .storeHarry(order.getStoreHarry())
                .factoryId(order.getFactoryId())
                .factoryName(order.getFactoryName())
                .factoryHarry(order.getFactoryHarry()) // 수정 가능
                .stockMainStoneNote(stockDto.getMainStoneNote()) // 수정 가능
                .stockAssistanceStoneNote(stockDto.getAssistanceStoneNote()) // 수정 가능
                .stockNote(stockDto.getOrderNote()) // 수정 가능
                .totalStonePurchaseCost(totalStonePurchaseCost) // 수정 가능
                .totalStoneLaborCost(totalStoneLaborCost) // 수정 가능
                .stoneAddLaborCost(stockDto.getStoneAddLaborCost())
                .orderStones(new ArrayList<>())
                .orderStatus(OrderStatus.valueOf(orderType))
                .product(product)
                .build();

        stock.setOrder(order);
        stockRepository.save(stock);

        List<OrderStone> orderStones = order.getOrderStones();
        updateStockStoneInfo(stockDto.getStoneInfos(), stock, orderStones);
        updateStoneCostAndPurchase(stock);

        order.updateOrderStatus(OrderStatus.valueOf(orderType));
        order.updateProductStatus(ProductStatus.DELIVERY);

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory = StatusHistory.phaseChange(
                order.getFlowCode(),
                lastHistory.getSourceType(),
                lastHistory.getPhase(),
                BusinessPhase.valueOf(orderType),
                nickname
        );

        statusHistoryRepository.save(statusHistory);
    }

    //재고 등록 -> 생성 시 발생하는 토큰 저장 후 고유 stock_code 발급 후 저장
    public void saveStock(String accessToken, String orderType, StockDto.createStockRequest stockDto) {
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
                .size(stockDto.getProductSize())
                .productPurchaseCost(stockDto.getProductPurchaseCost())
                .addLaborCost(stockDto.getAddProductLaborCost())
                .goldWeight(stockDto.getGoldWeight())
                .stoneWeight(stockDto.getStoneWeight())
                .build();

        // 자체 재고는 store에서 자신을 선택해야함
        Stock stock = Stock.builder()
                .stockNote(stockDto.getStockNote())
                .orderStatus(OrderStatus.NORMAL)
                .stockMainStoneNote(stockDto.getMainStoneNote())
                .stockAssistanceStoneNote(stockDto.getAssistanceStoneNote())
                .product(product)
                .orderStones(new ArrayList<>())
                .build();

        List<Long> stoneIds = new ArrayList<>();
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

            stoneIds.add(Long.valueOf(stoneInfo.getStoneId()));
            stock.addStockStone(orderStone);
        }

        stockRepository.save(stock);

        StatusHistory statusHistory = StatusHistory.create(
                stock.getStockCode(),
                SourceType.valueOf(orderType),
                BusinessPhase.valueOf(orderType),
                Kind.CREATE,
                nickname
        );

        statusHistoryRepository.save(statusHistory);

        OffsetDateTime assistantStoneCreateAt = DateConversionUtil.StringToOffsetDateTime(stockDto.getAssistantStoneCreateAt());

        //
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
                .addProductLaborCost(stockDto.getAddProductLaborCost())
                .addStoneLaborCost(stockDto.getStoneAddLaborCost())
                .assistantStone(stockDto.isAssistantStone())
                .assistantStoneId(Long.valueOf(stockDto.getAssistantStoneId()))
                .assistantStoneCreateAt(assistantStoneCreateAt)
                .stoneIds(stoneIds)
                .stoneInfos(stockDto.getStoneInfos())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.stockDetailAsync(stockRequest);
            }
        });
    }

    //재고 -> 대여
    public void stockToRental(String accessToken, Long flowCode, StockDto.StockRentalRequest stockRentalDto) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        String nickname = jwtUtil.getNickname(accessToken);
        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (stock.getOrderStatus() == OrderStatus.NORMAL || stock.getOrderStatus() == OrderStatus.STOCK) {

            StatusHistory beforeStatusHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(flowCode)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            StoreDto.Response storeInfo = storeClient.getStoreInfo(tenantId, Long.valueOf(stockRentalDto.getStoreId()));
            stock.updateStore(storeInfo);

            // 1) stone Update 필요 여부
            List<OrderStone> orderStones = stock.getOrderStones();
            updateStockStoneInfo(stockRentalDto.getStoneInfos(), stock, orderStones);

            // 2) stone Cost
            int totalStonePurchaseCost = 0;
            int mainStoneCost = 0;
            int assistanceStoneCost = 0;
            countStoneCost(stock.getOrderStones(), mainStoneCost, assistanceStoneCost, totalStonePurchaseCost);

            stock.updateStoneCost(totalStonePurchaseCost, mainStoneCost, assistanceStoneCost);

            stock.moveToRental(stockRentalDto);

            StatusHistory statusHistory = StatusHistory.phaseChange(
                    stock.getFlowCode(),
                    beforeStatusHistory.getSourceType(),
                    BusinessPhase.valueOf(beforeStatusHistory.getFromValue()),
                    BusinessPhase.RENTAL,
                    nickname
            );

            statusHistoryRepository.save(statusHistory);
            return;
        }
         throw new IllegalArgumentException("일반, 재고 상품만 대여 전환이 가능합니다.");
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

        StatusHistory orderStatusHistory = StatusHistory.phaseChange(
                beforeFlowCode,
                lastHistory.getSourceType(),
                lastHistory.getPhase(),
                BusinessPhase.WAITING,
                nickname
        );

        statusHistoryRepository.save(orderStatusHistory);

        List<StatusHistory> allByFlowCode = statusHistoryRepository.findAllByFlowCode(beforeFlowCode);
        for (StatusHistory statusHistory : allByFlowCode) {
            statusHistory.updateFlowCode(stock.getFlowCode());
        }

        StatusHistory deleteStockHistory = StatusHistory.phaseChange(
                stock.getFlowCode(),
                lastHistory.getSourceType(),
                lastHistory.getPhase(),
                BusinessPhase.DELETED,
                nickname
        );

        allByFlowCode.add(deleteStockHistory);

        statusHistoryRepository.saveAll(allByFlowCode);
    }

    // 대여 -> 반납 (재고) RETURN -> STOCK

    // 삭제 -> 재고 DELETE -> STOCK
    public void recoveryStock(String accessToken, Long flowCode, String orderType) {
        String nickname = jwtUtil.getNickname(accessToken);
        Stock stock = stockRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        OrderStatus target = OrderStatus.valueOf(orderType);

        if (target != OrderStatus.RETURN && target != OrderStatus.DELETED) {
            throw new IllegalArgumentException(WRONG_STATUS);
        }
        stock.updateOrderStatus(OrderStatus.STOCK);
        StatusHistory orderStatusHistory = StatusHistory.phaseChange(
                flowCode,
                lastHistory.getSourceType(),
                lastHistory.getPhase(),
                BusinessPhase.STOCK,
                nickname
        );
        statusHistoryRepository.save(orderStatusHistory);
    }

}
