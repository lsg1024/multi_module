package com.msa.order.local.domain.stock.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.global.kafka.dto.KafkaStockRequest;
import com.msa.order.local.domain.order.entity.OrderProduct;
import com.msa.order.local.domain.order.entity.OrderStone;
import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.order.entity.StatusHistory;
import com.msa.order.local.domain.order.entity.order_enum.OrderStatus;
import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import com.msa.order.local.domain.stock.dto.StockDto;
import com.msa.order.local.domain.stock.repository.CustomStockRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StockService {

    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final OrdersRepository ordersRepository;
    private final CustomStockRepository customStockRepository;

    public StockService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, OrdersRepository ordersRepository, CustomStockRepository customStockRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.ordersRepository = ordersRepository;
        this.customStockRepository = customStockRepository;
    }

    // 재고 관리 get 전체 재고 + 주문, 수리, 대여 관련
    @Transactional(readOnly = true)
    public CustomPage<StockDto.Response> getStocks(String inputSearch, StockDto.StockCondition condition, Pageable pageable) {
        return customStockRepository.findByStockProducts(inputSearch, condition, pageable);
    }

    // 재고 등록
    public void saveStock(String accessToken, String orderType, StockDto.OrderStockRequest stockDto) {
        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = TenantContext.getTenant();

        Long productId = Long.valueOf(stockDto.getProductId());
        Long storeId = Long.valueOf(stockDto.getStoreId());
        Long factoryId = Long.valueOf(stockDto.getFactoryId());
        Long materialId = Long.valueOf(stockDto.getMaterialId());
        Long classificationId = Long.valueOf(stockDto.getClassificationId());
        Long colorId = Long.valueOf(stockDto.getColorId());


        Orders order = Orders.builder()
                .orderNote(stockDto.getStockNote())
                .productStatus(ProductStatus.valueOf(orderType))
                .orderStatus(OrderStatus.NONE)
                .orderMainStoneNote(stockDto.getMainStoneNote())
                .orderAssistanceStoneNote(stockDto.getAssistanceStoneNote())
                .orderDate(null)
                .orderExpectDate(null)
                .build();

        OrderProduct orderProduct = OrderProduct.builder()
                .productId(productId)
                .productWeight(stockDto.getProductWeight())
                .stoneWeight(stockDto.getStoneWeight())
                .productAddLaborCost(stockDto.getProductAddLaborCost())
                .productSize(stockDto.getProductSize())
                .build();

        order.addOrderProduct(orderProduct);

        StatusHistory statusHistory = StatusHistory.builder()
                .productStatus(ProductStatus.valueOf(stockDto.getProductStatus()))
                .orderStatus(OrderStatus.NONE)
                .createAt(OffsetDateTime.now())
                .userName(nickname)
                .build();

        order.addStatusHistory(statusHistory);

        List<Long> stoneIds = new ArrayList<>();
        List<ProductDetailDto.StoneInfo> stoneInfos = stockDto.getStoneInfos();
        for (ProductDetailDto.StoneInfo stoneInfo : stoneInfos) {
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(Long.valueOf(stoneInfo.getStoneId()))
                    .originStoneName(stoneInfo.getStoneName())
                    .originStoneWeight(new BigDecimal(stoneInfo.getStoneWeight()))
                    .stonePurchasePrice(stoneInfo.getPurchaseCost())
                    .stoneLaborCost(stoneInfo.getLaborCost())
                    .stoneQuantity(stoneInfo.getQuantity())
                    .productStoneMain(stoneInfo.isProductStoneMain())
                    .includeQuantity(stoneInfo.isIncludeQuantity())
                    .includeWeight(stoneInfo.isIncludeWeight())
                    .includeLabor(stoneInfo.isIncludeLabor())
                    .build();

            stoneIds.add(Long.valueOf(stoneInfo.getStoneId()));
            order.addOrderStone(orderStone);
        }

        ordersRepository.save(order);

        KafkaStockRequest stockRequest = KafkaStockRequest.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
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

        order.addOrderCode(String.format("J%07d", order.getOrderId()));
        ordersRepository.save(order);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.stockDetailAsync(stockRequest);
            }
        });
    }

    // 재고 -> 주문

    // 재고 -> 판매

    // 재고 -> 대여

    // 재고 -> 삭제
}
