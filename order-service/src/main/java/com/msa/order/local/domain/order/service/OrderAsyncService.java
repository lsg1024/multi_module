package com.msa.order.local.domain.order.service;

import com.msa.common.global.aop.Retry;
import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.local.domain.order.dto.FactoryDto;
import com.msa.order.local.domain.order.dto.StoreDto;
import com.msa.order.local.domain.order.entity.OrderProduct;
import com.msa.order.local.domain.order.entity.OrderStatus;
import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.order.entity.StatusHistory;
import com.msa.order.local.domain.order.external_client.*;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND_STONE;

@Slf4j
@Service
public class OrderAsyncService {
    private final StoreClient storeClient;
    private final StoneClient stoneClient;
    private final ProductClient productClient;
    private final FactoryClient factoryClient;
    private final MaterialClient materialClient;
    private final ClassificationClient classificationClient;
    private final ColorClient colorClient;
    private final OrdersRepository ordersRepository;

    public OrderAsyncService(StoreClient storeClient, StoneClient stoneClient, ProductClient productClient, FactoryClient factoryClient, MaterialClient materialClient, ClassificationClient classificationClient, ColorClient colorClient, OrdersRepository ordersRepository) {
        this.storeClient = storeClient;
        this.stoneClient = stoneClient;
        this.productClient = productClient;
        this.factoryClient = factoryClient;
        this.materialClient = materialClient;
        this.classificationClient = classificationClient;
        this.colorClient = colorClient;
        this.ordersRepository = ordersRepository;
    }

    @Retry(value = 3)
    @Transactional
    public void handle(OrderAsyncRequested evt) {

        // 멀티테넌시 컨텍스트 전파
        final String tenantId = evt.getTenantId();

        Orders order = ordersRepository.findAggregate(evt.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + evt.getOrderId()));

        // 멱등 처리: 이미 AWAIT이 아니면 스킵
        if (order.getOrderStatus() != OrderStatus.ORDER_AWAIT) {
            log.info("Skip Sync. status={}, orderId={}", order.getOrderStatus(), order.getOrderId());
            return;
        }

        try {
            // 1) 외부 조회 (순차/병렬 선택 가능)
            StoreDto.Request storeInfo = storeClient.getStoreInfo(tenantId, evt.getStoreId());
            String factoryName = factoryClient.getFactoryInfo(tenantId, evt.getFactoryId()).getFactoryName();
            String materialName = materialClient.getMaterialInfo(tenantId, evt.getMaterialId());
            String classificationName = classificationClient.getClassificationInfo(tenantId, evt.getClassificationId());
            String colorName = colorClient.getColorInfo(tenantId, evt.getColorId());
            ProductDetailDto productInfo = productClient.getProductInfo(tenantId, evt.getProductId(), storeInfo.getGrade());

            OrderProduct orderProduct = order.getOrderProduct();
            orderProduct.updateOrder(
                    productInfo.getProductName(),
                    productInfo.getLaborCost(),
                    materialName,
                    classificationName,
                    colorName
            );

            List<Long> stoneIds = evt.getStoneIds();
            for (Long stoneId : stoneIds) {
                Boolean existStoneId = stoneClient.getExistStoneId(tenantId, stoneId);
                if (!existStoneId) {
                    throw new IllegalArgumentException(NOT_FOUND_STONE);
                }
            }

            order.updateStoreName(StoreDto.Request.builder().storeId(evt.getStoreId()).storeName(storeInfo.getStoreName()).build());
            order.updateFactoryName(new FactoryDto.Request(evt.getFactoryId(), factoryName));

            order.updateStatus(OrderStatus.ORDER);
            order.addStatusHistory(StatusHistory.builder()
                    .orderStatus(OrderStatus.ORDER)
                    .createAt(OffsetDateTime.now())
                    .userName(evt.getNickname())
                    .build());

            ordersRepository.save(order);

        } catch (Exception e) {
            log.error("Async failed. orderId={}, err={}", evt.getOrderId(), e.getMessage(), e);
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
