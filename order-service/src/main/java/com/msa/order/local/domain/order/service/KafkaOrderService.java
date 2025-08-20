package com.msa.order.local.domain.order.service;

import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.local.domain.order.dto.FactoryDto;
import com.msa.order.local.domain.order.dto.StoreDto;
import com.msa.order.local.domain.order.entity.OrderProduct;
import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.order.entity.StatusHistory;
import com.msa.order.local.domain.order.entity.order_enum.OrderStatus;
import com.msa.order.local.domain.order.external_client.*;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import com.msa.order.local.domain.order.repository.StatusHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND_STONE;
import static com.msa.order.local.domain.order.entity.order_enum.ProductStatus.RECEIPT_FAILED;

@Slf4j
@Service
public class KafkaOrderService {
    private final StoreClient storeClient;
    private final StoneClient stoneClient;
    private final ProductClient productClient;
    private final FactoryClient factoryClient;
    private final MaterialClient materialClient;
    private final ClassificationClient classificationClient;
    private final ColorClient colorClient;
    private final OrdersRepository ordersRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public KafkaOrderService(StoreClient storeClient, StoneClient stoneClient, ProductClient productClient, FactoryClient factoryClient, MaterialClient materialClient, ClassificationClient classificationClient, ColorClient colorClient, OrdersRepository ordersRepository, StatusHistoryRepository statusHistoryRepository) {
        this.storeClient = storeClient;
        this.stoneClient = stoneClient;
        this.productClient = productClient;
        this.factoryClient = factoryClient;
        this.materialClient = materialClient;
        this.classificationClient = classificationClient;
        this.colorClient = colorClient;
        this.ordersRepository = ordersRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    public void handle(OrderAsyncRequested evt) {

        // 멀티테넌시 컨텍스트 전파
        final String tenantId = evt.getTenantId();

        Orders order = ordersRepository.findAggregate(evt.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (order.getOrderStatus() != OrderStatus.ORDER && order.getOrderStatus() != OrderStatus.FIX) {
            return;
        }

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        try {
            // 1) 외부 조회 (순차/병렬 선택 가능)
            StoreDto.Response storeInfo = storeClient.getStoreInfo(tenantId, evt.getStoreId());
            String factoryName = factoryClient.getFactoryInfo(tenantId, evt.getFactoryId()).getFactoryName();
            String materialName = materialClient.getMaterialInfo(tenantId, evt.getMaterialId());
            String classificationName = classificationClient.getClassificationInfo(tenantId, evt.getClassificationId());
            String colorName = colorClient.getColorInfo(tenantId, evt.getColorId());
            ProductDetailDto productInfo = productClient.getProductInfo(tenantId, evt.getProductId(), storeInfo.getGrade());

            OrderProduct orderProduct = order.getOrderProduct();
            orderProduct.updateOrder(
                    productInfo.getProductName(),
                    productInfo.getPurchaseCost(),
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

            order.updateStore(new StoreDto.Response(evt.getStoreId(), storeInfo.getStoreName()));
            order.updateFactory(new FactoryDto.Response(evt.getFactoryId(), factoryName));

            ordersRepository.save(order);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    StatusHistory.BusinessPhase.ORDER,
                    evt.getNickname()
            );

            statusHistoryRepository.save(statusHistory);

        } catch (Exception e) {
            log.error("Async failed. orderId={}, err={}", evt.getFlowCode(), e.getMessage(), e);
            order.updateProductStatus(RECEIPT_FAILED);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    StatusHistory.BusinessPhase.ORDER_FAIL,
                    evt.getNickname()
            );

            statusHistoryRepository.save(statusHistory);
        }
    }
}
