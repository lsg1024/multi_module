package com.msa.jewelry.order.internal.order.service;

import com.msa.jewelry.account.api.FactoryFinder;
import com.msa.jewelry.account.api.FactoryView;
import com.msa.jewelry.account.api.StoreFinder;
import com.msa.jewelry.account.api.StoreView;
import com.msa.jewelry.order.internal.global.kafka_dto_legacy.OrderAsyncRequested;
import com.msa.jewelry.order.internal.global.kafka_dto_legacy.OrderUpdateRequest;
import com.msa.jewelry.order.internal.global.util.SafeParse;
import com.msa.jewelry.product.api.AssistantStoneFinder;
import com.msa.jewelry.product.api.AssistantStoneView;
import com.msa.jewelry.product.api.ColorFinder;
import com.msa.jewelry.product.api.MaterialFinder;
import com.msa.jewelry.product.api.ProductDetailView;
import com.msa.jewelry.product.api.ProductFinder;
import com.msa.jewelry.product.api.StoneFinder;
import com.msa.jewelry.order.internal.order.entity.OrderProduct;
import com.msa.jewelry.order.internal.order.entity.Orders;
import com.msa.jewelry.order.internal.order.entity.StatusHistory;
import com.msa.jewelry.order.internal.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.order.internal.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.order.internal.order.entity.order_enum.ProductStatus;
import com.msa.jewelry.order.internal.order.repository.OrdersRepository;
import com.msa.jewelry.order.internal.order.repository.StatusHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.msa.jewelry.order.internal.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.jewelry.order.internal.global.exception.ExceptionMessage.NOT_FOUND_STONE;

@Slf4j
@Service
@Transactional
public class KafkaOrderService {

    private final StoreFinder storeFinder;
    private final StoneFinder stoneFinder;
    private final ProductFinder productFinder;
    private final FactoryFinder factoryFinder;
    private final MaterialFinder materialFinder;
    private final ColorFinder colorFinder;
    private final AssistantStoneFinder assistantStoneFinder;
    private final OrdersRepository ordersRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public KafkaOrderService(StoreFinder storeFinder, StoneFinder stoneFinder, ProductFinder productFinder, FactoryFinder factoryFinder, MaterialFinder materialFinder, ColorFinder colorFinder, AssistantStoneFinder assistantStoneFinder, OrdersRepository ordersRepository, StatusHistoryRepository statusHistoryRepository) {
        this.storeFinder = storeFinder;
        this.stoneFinder = stoneFinder;
        this.productFinder = productFinder;
        this.factoryFinder = factoryFinder;
        this.materialFinder = materialFinder;
        this.colorFinder = colorFinder;
        this.assistantStoneFinder = assistantStoneFinder;
        this.ordersRepository = ordersRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public void createHandle(OrderAsyncRequested evt) {
        final String token = evt.getToken();

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
            // 2026-05 P4: Orders 에 storeName/factoryName 컬럼 제거.
            //              storeId/factoryId 변경 시점에만 스냅샷(harry, grade) 갱신.
            StoreView latestStoreInfo = storeFinder.getStoreInfo(evt.getStoreId());
            if (!Objects.equals(evt.getStoreId(), order.getStoreId())) {
                order.updateStore(
                        latestStoreInfo.storeId(),
                        latestStoreInfo.storeGrade(),
                        SafeParse.toBigDecimalOrNull(latestStoreInfo.storeHarry())
                );
            }

            FactoryView latestFactoryInfo = factoryFinder.getFactoryInfo(evt.getFactoryId());
            if (!Objects.equals(evt.getFactoryId(), order.getFactoryId())) {
                order.updateFactory(
                        latestFactoryInfo.factoryId(),
                        SafeParse.toBigDecimalOrNull(latestFactoryInfo.goldHarryLoss())
                );
            }

            ProductDetailView latestProductInfo = productFinder.getProductDetail(evt.getProductId(), order.getStoreGrade());
            String latestMaterialName = materialFinder.getMaterialName(evt.getMaterialId());
            String latestColorName = colorFinder.getColorName(evt.getColorId());

            if (!Objects.equals(latestProductInfo.productName(), orderProduct.getProductName()) ||
                    !Objects.equals(latestProductInfo.productFactoryName(), orderProduct.getProductFactoryName()) ||
                    !Objects.equals(latestMaterialName, orderProduct.getMaterialName()) ||
                    !Objects.equals(latestColorName, orderProduct.getColorName()) ||
                    !Objects.equals(latestProductInfo.classificationName(), orderProduct.getClassificationName()) ||
                    !Objects.equals(latestProductInfo.setTypeName(), orderProduct.getSetTypeName()) ||
                    !evt.getMaterialId().equals(orderProduct.getMaterialId()) ||
                    !evt.getColorId().equals(orderProduct.getColorId())) {

                orderProduct.updateOrderProduct(
                        latestProductInfo.productName(),
                        latestProductInfo.productFactoryName(),
                        evt.getMaterialId(),
                        latestMaterialName,
                        evt.getColorId(),
                        latestColorName,
                        latestProductInfo.classificationId(),
                        latestProductInfo.classificationName(),
                        latestProductInfo.setTypeId(),
                        latestProductInfo.setTypeName());
            }

            // 보조석 정보 업데이트 - assistantStone 플래그와 관계없이 값이 있으면 설정
            AssistantStoneView assistantStoneInfo = assistantStoneFinder.getAssistantStone(evt.getAssistantStoneId());
            orderProduct.updateOrderProductAssistantStone(
                    evt.isAssistantStone(),
                    assistantStoneInfo.assistantStoneId(),
                    assistantStoneInfo.assistantStoneName(),
                    evt.getAssistantStoneCreateAt()
            );

            List<Long> stoneIds = evt.getStoneIds();
            for (Long stoneId : stoneIds) {
                if (!stoneFinder.existsStoneId(stoneId)) {
                    throw new IllegalArgumentException(NOT_FOUND_STONE);
                }
            }

            order.updateOrderStatus(OrderStatus.valueOf(evt.getOrderStatus()));
            ordersRepository.save(order);

        } catch (Exception e) {
            log.error("주문 비동기 처리 실패. flowCode={}, storeId={}, factoryId={}, productId={}, err={}",
                    evt.getFlowCode(), evt.getStoreId(), evt.getFactoryId(), evt.getProductId(), e.getMessage(), e);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    BusinessPhase.valueOf(lastHistory.getToValue()),
                    BusinessPhase.valueOf(evt.getOrderStatus()),
                    "재고 등록 실패: " + e.getMessage(),
                    evt.getNickname()
            );
            statusHistoryRepository.save(statusHistory);
        }
    }

    public void updateHandle(OrderUpdateRequest updateRequest) {

        // 멀티테넌시 컨텍스트 전파
        final String token = updateRequest.getToken();

        Orders order = ordersRepository.findByFlowCode(updateRequest.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StatusHistory statusHistory;

        StoreView storeInfoForGrade = null;
        try {

            if (updateRequest.getStoreId() != null) {
                StoreView storeInfo = storeFinder.getStoreInfo(updateRequest.getStoreId());
                order.updateStore(
                        storeInfo.storeId(),
                        storeInfo.storeGrade(),
                        SafeParse.toBigDecimalOrNull(storeInfo.storeHarry())
                );
                storeInfoForGrade = storeInfo;
            }

            if (updateRequest.getFactoryId() != null) {
                FactoryView factoryInfo = factoryFinder.getFactoryInfo(updateRequest.getFactoryId());
                order.updateFactory(
                        updateRequest.getFactoryId(),
                        SafeParse.toBigDecimalOrNull(factoryInfo.goldHarryLoss())
                );
            }

            String materialName = null;
            if (updateRequest.getMaterialId() != null) {
                materialName = materialFinder.getMaterialName(updateRequest.getMaterialId());
            }

            String colorName = null;
            if (updateRequest.getColorId() != null) {
                colorName = colorFinder.getColorName(updateRequest.getColorId());
            }

            ProductDetailView productInfo = null;
            if (updateRequest.getProductId() != null || updateRequest.getStoreId() != null) {

                Long targetProductId = updateRequest.getProductId() != null
                        ? updateRequest.getProductId()
                        : order.getOrderProduct().getProductId();

                String targetGrade = storeInfoForGrade != null
                        ? storeInfoForGrade.storeGrade()
                        : order.getStoreGrade();

                productInfo = productFinder.getProductDetail(targetProductId, targetGrade);
            }

            // 보조석 정보 조회 - assistantStone 플래그와 관계없이 assistantStoneId가 있으면 조회
            AssistantStoneView assistantStoneInfo = null;
            if (updateRequest.getAssistantStoneId() != null) {
                assistantStoneInfo = assistantStoneFinder.getAssistantStone(updateRequest.getAssistantStoneId());
            }

            OrderProduct orderProduct = order.getOrderProduct();
            orderProduct.updateDetails(
                    productInfo != null ? productInfo.productName() : null,
                    productInfo != null ? productInfo.classificationName() : null,
                    productInfo != null ? productInfo.setTypeName() : null,
                    materialName,
                    colorName,
                    updateRequest.isAssistantStone(),
                    updateRequest.getAssistantStoneId(),
                    assistantStoneInfo != null ? assistantStoneInfo.assistantStoneName() : null,
                    updateRequest.getAssistantStoneCreateAt()
            );

            if (order.getOrderStatus() == OrderStatus.WAIT) {
                order.updateOrderStatus(OrderStatus.valueOf(updateRequest.getOrderStatus()));
            }

            ordersRepository.save(order);

        } catch (Exception e) {
            log.error("Async failed. orderId={}, err={}", updateRequest.getFlowCode(), e.getMessage(), e);
            order.updateProductStatus(ProductStatus.CHANG_FAILED);

            statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    lastHistory.getPhase(),
                    BusinessPhase.FAIL,
                    "주문 수정 실패",
                    updateRequest.getNickname()
            );

            statusHistoryRepository.save(statusHistory);
        }
    }
}