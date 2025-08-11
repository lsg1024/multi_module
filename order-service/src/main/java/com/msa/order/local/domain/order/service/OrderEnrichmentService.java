package com.msa.order.local.domain.order.service;

import com.msa.order.global.kafka.dto.OrderEnrichmentRequested;
import com.msa.order.local.domain.order.dto.FactoryDto;
import com.msa.order.local.domain.order.dto.StoreDto;
import com.msa.order.local.domain.order.entity.*;
import com.msa.order.local.domain.order.external_client.*;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
public class OrderEnrichmentService {
    private final StoreClient storeClient;
    private final ProductClient productClient;
    private final FactoryClient factoryClient;
    private final MaterialClient materialClient;
    private final ClassificationClient classificationClient;
    private final ColorClient colorClient;
    private final OrdersRepository ordersRepository;

    public OrderEnrichmentService(StoreClient storeClient, ProductClient productClient, FactoryClient factoryClient, MaterialClient materialClient, ClassificationClient classificationClient, ColorClient colorClient, OrdersRepository ordersRepository) {
        this.storeClient = storeClient;
        this.productClient = productClient;
        this.factoryClient = factoryClient;
        this.materialClient = materialClient;
        this.classificationClient = classificationClient;
        this.colorClient = colorClient;
        this.ordersRepository = ordersRepository;
    }

    @Transactional
    public void handle(OrderEnrichmentRequested evt) {

        // 멀티테넌시 컨텍스트 전파
        final String tenantId = evt.getTenantId();

        Orders order = ordersRepository.findAggregate(evt.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + evt.getOrderId()));

        // 멱등 처리: 이미 AWAIT이 아니면 스킵
        if (order.getOrderStatus() != OrderStatus.ORDER_AWAIT) {
            log.info("Skip enrichment. status={}, orderId={}", order.getOrderStatus(), order.getOrderId());
            return;
        }

        try {
            // 1) 외부 조회 (순차/병렬 선택 가능)
            StoreClient.StoreInfo storeInfo = storeClient.getStoreInfo(tenantId, evt.getStoreId());
            String factoryName        = factoryClient.getFactoryInfo(tenantId, evt.getFactoryId());
            String materialName       = materialClient.getMaterialInfo(tenantId, evt.getMaterialId());
            String classificationName = classificationClient.getClassificationInfo(tenantId, evt.getClassificationId());
            String colorName          = colorClient.getColorInfo(tenantId, evt.getColorId());
            ProductDetailDto productInfo = productClient.getProductInfo(tenantId, evt.getProductId(), storeInfo.getGrade());

            // 2) OrderProduct 보강
            OrderProduct orderProduct = order.getOrderProduct();
            orderProduct.updateOrder(
                    productInfo.getProductName(),
                    productInfo.getLaborCost(),
                    materialName,
                    classificationName,
                    colorName
            );

            order.getOrderStones().clear();
            for (ProductDetailDto.StoneInfo stoneInfo : productInfo.getStoneInfos()) {
                OrderStone os = OrderStone.builder()
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
                order.addOrderStone(os);
            }

            // 3) 본문 보강 + 상태 전이
            order.updateStoreName(new StoreDto.UpdateRequest(evt.getStoreId(), storeInfo.getStoreName()));
            order.updateFactoryName(new FactoryDto.UpdateRequest(evt.getFactoryId(), factoryName)); // 아래 메서드 추가 예시

            order.updateStatus(OrderStatus.ORDER);
            order.addStatusHistory(StatusHistory.builder()
                    .orderStatus(OrderStatus.ORDER)
                    .createAt(OffsetDateTime.now())
                    .userName(evt.getNickname())
                    .build());

            ordersRepository.save(order);

        } catch (Exception e) {
            log.error("Enrichment failed. orderId={}, err={}", evt.getOrderId(), e.getMessage(), e);
            order.updateStatus(OrderStatus.ORDER_AWAIT_FAILED);
            order.addStatusHistory(StatusHistory.builder()
                    .orderStatus(OrderStatus.ORDER_AWAIT_FAILED)
                    .createAt(OffsetDateTime.now())
                    .userName(evt.getNickname())
                    .build());
            ordersRepository.save(order);
        }
    }
}
