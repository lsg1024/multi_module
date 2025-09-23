package com.msa.order.local.order.service;

import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.global.kafka.dto.OrderUpdateRequest;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.StoreDto;
import com.msa.order.local.order.entity.OrderProduct;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.msa.order.local.order.external_client.*;
import com.msa.order.local.order.external_client.dto.AssistantStoneDto;
import com.msa.order.local.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.order.repository.OrdersRepository;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND_STONE;

@Slf4j
@Service
public class KafkaOrderService {

    private final StoreClient storeClient;
    private final StoneClient stoneClient;
    private final ProductClient productClient;
    private final FactoryClient factoryClient;
    private final MaterialClient materialClient;
    private final ColorClient colorClient;
    private final AssistantStoneClient assistantStoneClient;
    private final OrdersRepository ordersRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public KafkaOrderService(StoreClient storeClient, StoneClient stoneClient, ProductClient productClient, FactoryClient factoryClient, MaterialClient materialClient, ColorClient colorClient, AssistantStoneClient assistantStoneClient, OrdersRepository ordersRepository, StatusHistoryRepository statusHistoryRepository) {
        this.storeClient = storeClient;
        this.stoneClient = stoneClient;
        this.productClient = productClient;
        this.factoryClient = factoryClient;
        this.materialClient = materialClient;
        this.colorClient = colorClient;
        this.assistantStoneClient = assistantStoneClient;
        this.ordersRepository = ordersRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    public void createHandle(OrderAsyncRequested evt) {

        log.info("createHandle = {}", evt.toString());

        // 멀티테넌시 컨텍스트 전파
        final String tenantId = evt.getTenantId();

        Orders order = ordersRepository.findByFlowCode(evt.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (order.getOrderStatus() != OrderStatus.ORDER && order.getOrderStatus() != OrderStatus.FIX) {
            return;
        }

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        try {
            StoreDto.Response storeInfo = storeClient.getStoreInfo(tenantId, evt.getStoreId());
            String factoryName = factoryClient.getFactoryInfo(tenantId, evt.getFactoryId()).getFactoryName();
            String materialName = materialClient.getMaterialInfo(tenantId, evt.getMaterialId());
            String colorName = colorClient.getColorInfo(tenantId, evt.getColorId());
            ProductDetailDto productInfo = productClient.getProductInfo(tenantId, evt.getProductId(), storeInfo.getGrade());

            log.info("");

            AssistantStoneDto.Response assistantStoneInfo;
            OrderProduct orderProduct = order.getOrderProduct();
            if (evt.isAssistantStone()) {
                assistantStoneInfo = assistantStoneClient.getAssistantStoneInfo(tenantId, evt.getAssistantStoneId());
                orderProduct.updateOrderProduct(
                        productInfo.getProductName(),
                        productInfo.getPurchaseCost(),
                        productInfo.getLaborCost(),
                        evt.getMaterialId(),
                        materialName,
                        evt.getColorId(),
                        colorName,
                        productInfo.getClassificationId(),
                        productInfo.getClassificationName(),
                        productInfo.getSetTypeId(),
                        productInfo.getSetTypeName(),
                        evt.isAssistantStone(),
                        assistantStoneInfo.getAssistantName(),
                        evt.getAssistantStoneCreateAt()
                );
            } else {
                orderProduct.updateOrderProduct(
                        productInfo.getProductName(),
                        productInfo.getPurchaseCost(),
                        productInfo.getLaborCost(),
                        evt.getMaterialId(),
                        materialName,
                        evt.getColorId(),
                        colorName,
                        productInfo.getClassificationId(),
                        productInfo.getClassificationName(),
                        productInfo.getSetTypeId(),
                        productInfo.getSetTypeName()
                );
            }

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
                    BusinessPhase.valueOf(evt.getOrderStatus()),
                    evt.getNickname()
            );

            statusHistoryRepository.save(statusHistory);

        } catch (Exception e) {
            log.error("Async failed. orderId={}, err={}", evt.getFlowCode(), e.getMessage(), e);
            order.updateProductStatus(ProductStatus.RECEIPT_FAILED);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    BusinessPhase.ORDER_FAIL,
                    evt.getNickname()
            );

            statusHistoryRepository.save(statusHistory);
        }
    }

    @Transactional
    public void updateHandle(OrderUpdateRequest updateRequest) {

        log.info("updateHandle = {}", updateRequest.toString());

        // 멀티테넌시 컨텍스트 전파
        final String tenantId = updateRequest.getTenantId();

        Orders order = ordersRepository.findByFlowCode(updateRequest.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (order.getOrderStatus() != OrderStatus.ORDER && order.getOrderStatus() != OrderStatus.FIX) {
            return;
        }

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        try {

            if (updateRequest.getStoreId() != null) {
                StoreDto.Response storeInfo = storeClient.getStoreInfo(tenantId, updateRequest.getStoreId());
                order.updateStore(new StoreDto.Response(updateRequest.getStoreId(), storeInfo.getStoreName()));
            }

            if (updateRequest.getFactoryId() != null) {
                String factoryName = factoryClient.getFactoryInfo(tenantId, updateRequest.getFactoryId()).getFactoryName();
                order.updateFactory(new FactoryDto.Response(updateRequest.getFactoryId(), factoryName));
            }

            String materialName = null;
            if (updateRequest.getMaterialId() != null) {
                materialName = materialClient.getMaterialInfo(tenantId, updateRequest.getMaterialId());
            }

            String colorName = null;
            if (updateRequest.getColorId() != null) {
                colorName = colorClient.getColorInfo(tenantId, updateRequest.getColorId());
            }

            ProductDetailDto productInfo = null;
            if (updateRequest.getProductId() != null) {
                Long storeIdForGrade = updateRequest.getStoreId() != null ? updateRequest.getStoreId() : order.getStoreId();
                StoreDto.Response storeInfoForGrade = storeClient.getStoreInfo(tenantId, storeIdForGrade);
                productInfo = productClient.getProductInfo(tenantId, updateRequest.getProductId(), storeInfoForGrade.getGrade());
            }

            AssistantStoneDto.Response assistantStoneInfo = null;
            if (updateRequest.isAssistantStone() && updateRequest.getAssistantStoneId() != null) {
                assistantStoneInfo = assistantStoneClient.getAssistantStoneInfo(tenantId, updateRequest.getAssistantStoneId());
            }

            OrderProduct orderProduct = order.getOrderProduct();
            orderProduct.updateDetails(
                    productInfo != null ? productInfo.getProductName() : null,
                    productInfo != null ? productInfo.getPurchaseCost() : null,
                    productInfo != null ? productInfo.getLaborCost() : null,
                    productInfo != null ? productInfo.getClassificationName() : null,
                    productInfo != null ? productInfo.getSetTypeName() : null,
                    materialName,
                    colorName,
                    updateRequest.isAssistantStone(),
                    assistantStoneInfo != null ? assistantStoneInfo.getAssistantName() : null,
                    updateRequest.getAssistantStoneCreateAt()
            );

            ordersRepository.save(order);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    BusinessPhase.UPDATE,
                    updateRequest.getNickname()
            );
            statusHistoryRepository.save(statusHistory);

        } catch (Exception e) {
            log.error("Async failed. orderId={}, err={}", updateRequest.getFlowCode(), e.getMessage(), e);
            order.updateProductStatus(ProductStatus.CHANG_FAILED);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    BusinessPhase.ORDER_UPDATE_FAIL,
                    updateRequest.getNickname()
            );

            statusHistoryRepository.save(statusHistory);
        }
    }
}