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
import java.util.Objects;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND_STONE;

@Slf4j
@Service
@Transactional
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

    public void createHandle(OrderAsyncRequested evt) {
        final String tenantId = evt.getTenantId();

        Orders order = ordersRepository.findByFlowCode(evt.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (order.getOrderStatus() != OrderStatus.WAIT) {
            return;
        }

        OrderProduct orderProduct = order.getOrderProduct();
        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        try {
            StoreDto.Response latestStoreInfo = storeClient.getStoreInfo(tenantId, evt.getStoreId());
            if (!Objects.equals(latestStoreInfo.getStoreName(), order.getStoreName())) {

                order.updateStore(StoreDto.Response.builder()
                        .storeId(latestStoreInfo.getStoreId())
                        .storeName(latestStoreInfo.getStoreName())
                        .storeHarry(latestStoreInfo.getStoreHarry())
                        .grade(latestStoreInfo.getGrade())
                        .build());
            }

            FactoryDto.Response latestFactoryInfo = factoryClient.getFactoryInfo(tenantId, evt.getFactoryId());
            if (!Objects.equals(latestFactoryInfo.getFactoryName(), order.getFactoryName())) {

                order.updateFactory(
                        latestFactoryInfo.getFactoryId(),
                        latestFactoryInfo.getFactoryName()
                );
            }

            ProductDetailDto latestProductInfo = productClient.getProductInfo(tenantId, evt.getProductId(), order.getStoreGrade());
            String latestMaterialName = materialClient.getMaterialInfo(tenantId, evt.getMaterialId());
            String latestColorName = colorClient.getColorInfo(tenantId, evt.getColorId());

            if (!Objects.equals(latestProductInfo.getProductName(), orderProduct.getProductName()) ||
                    !Objects.equals(latestProductInfo.getProductFactoryName(), orderProduct.getProductFactoryName()) ||
                    !Objects.equals(latestProductInfo.getPurchaseCost(), orderProduct.getProductPurchaseCost()) ||
                    !Objects.equals(latestProductInfo.getLaborCost(), orderProduct.getProductLaborCost()) ||
                    !Objects.equals(latestMaterialName, orderProduct.getMaterialName()) ||
                    !Objects.equals(latestColorName, orderProduct.getColorName()) ||
                    !Objects.equals(latestProductInfo.getClassificationName(), orderProduct.getClassificationName()) ||
                    !Objects.equals(latestProductInfo.getSetTypeName(), orderProduct.getSetTypeName()) ||
                    !evt.getMaterialId().equals(orderProduct.getMaterialId()) ||
                    !evt.getColorId().equals(orderProduct.getColorId())) {

                orderProduct.updateOrderProduct(
                        latestProductInfo.getProductName(),
                        latestProductInfo.getProductFactoryName(),
                        latestProductInfo.getPurchaseCost(),
                        latestProductInfo.getLaborCost(),
                        evt.getMaterialId(),
                        latestMaterialName,
                        evt.getColorId(),
                        latestColorName,
                        latestProductInfo.getClassificationId(),
                        latestProductInfo.getClassificationName(),
                        latestProductInfo.getSetTypeId(),
                        latestProductInfo.getSetTypeName());
            }

            AssistantStoneDto.Response assistantStoneInfo = assistantStoneClient.getAssistantStoneInfo(tenantId, evt.getAssistantStoneId());
            if (evt.isAssistantStone()) {
                orderProduct.updateOrderProductAssistantStone(
                        true,
                        assistantStoneInfo.getAssistantStoneId(),
                        assistantStoneInfo.getAssistantStoneName(),
                        evt.getAssistantStoneCreateAt()
                );
            } else {
                orderProduct.updateOrderProductAssistantStoneFail(
                        false,
                        assistantStoneInfo.getAssistantStoneId(),
                        assistantStoneInfo.getAssistantStoneName()
                );
            }

            List<Long> stoneIds = evt.getStoneIds();
            for (Long stoneId : stoneIds) {
                if (!stoneClient.getExistStoneId(tenantId, stoneId)) {
                    throw new IllegalArgumentException(NOT_FOUND_STONE);
                }
            }

            order.updateOrderStatus(OrderStatus.valueOf(evt.getOrderStatus()));
            ordersRepository.save(order);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    BusinessPhase.valueOf(lastHistory.getToValue()),
                    BusinessPhase.valueOf(evt.getOrderStatus()),
                    evt.getNickname()
            );
            statusHistoryRepository.save(statusHistory);

        } catch (Exception e) {
            order.updateProductStatus(ProductStatus.RECEIPT_FAILED);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    BusinessPhase.valueOf(lastHistory.getToValue()),
                    BusinessPhase.valueOf(evt.getOrderStatus()),
                    evt.getNickname()
            );
            statusHistoryRepository.save(statusHistory);
        }
    }

    public void updateHandle(OrderUpdateRequest updateRequest) {

        // 멀티테넌시 컨텍스트 전파
        final String tenantId = updateRequest.getTenantId();

        Orders order = ordersRepository.findByFlowCode(updateRequest.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (order.getOrderStatus() != OrderStatus.WAIT) {
            return;
        }

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        StoreDto.Response storeInfoForGrade = null;
        try {

            if (updateRequest.getStoreId() != null) {
                StoreDto.Response storeInfo = storeClient.getStoreInfo(tenantId, updateRequest.getStoreId());
                order.updateStore(StoreDto.Response.builder()
                        .storeId(storeInfo.getStoreId())
                        .storeName(storeInfo.getStoreName())
                        .storeHarry(storeInfo.getStoreHarry())
                        .grade(storeInfo.getGrade())
                        .build());

                storeInfoForGrade = storeInfo;
            }

            if (updateRequest.getFactoryId() != null) {
                FactoryDto.Response factoryInfo = factoryClient.getFactoryInfo(tenantId, updateRequest.getFactoryId());
                order.updateFactory(updateRequest.getFactoryId(), factoryInfo.getFactoryName());
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
            if (updateRequest.getProductId() != null || updateRequest.getStoreId() != null) {

                Long targetProductId = updateRequest.getProductId() != null
                        ? updateRequest.getProductId()
                        : order.getOrderProduct().getProductId();


                String targetGrade = storeInfoForGrade != null
                        ? storeInfoForGrade.getGrade()
                        : order.getStoreGrade();

                productInfo = productClient.getProductInfo(tenantId, targetProductId, targetGrade);
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
                    assistantStoneInfo != null ? assistantStoneInfo.getAssistantStoneName() : null,
                    updateRequest.getAssistantStoneCreateAt()
            );

            ordersRepository.save(order);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    BusinessPhase.valueOf(lastHistory.getToValue()),
                    BusinessPhase.ORDER,
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
                    BusinessPhase.FAIL,
                    updateRequest.getNickname()
            );

            statusHistoryRepository.save(statusHistory);
        }
    }
}