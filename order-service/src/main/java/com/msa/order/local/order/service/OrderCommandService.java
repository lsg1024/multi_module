package com.msa.order.local.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.order.global.dto.OutboxCreatedEvent;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.global.kafka.dto.OrderUpdateRequest;
import com.msa.order.global.util.SafeParse;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.entity.OrderProduct;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.msa.order.local.order.repository.OrdersRepository;
import com.msa.order.local.outbox.domain.entity.OutboxEvent;
import com.msa.order.local.outbox.repository.OutboxEventRepository;
import com.msa.order.local.priority.entitiy.Priority;
import com.msa.order.local.priority.repository.PriorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.msa.order.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.util.DateConversionUtil.StringToOffsetDateTime;
import static com.msa.order.local.order.util.StoneUtil.updateOrderStoneInfo;

/**
 * 주문 생성/수정/삭제를 담당하는 서비스
 * OrdersService Facade에서 위임받아 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrdersRepository ordersRepository;
    private final PriorityRepository priorityRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 생성
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Orders createOrder(String tenantId, String accessToken, String orderStatus,
                             OrderDto.Request orderDto, String nickname) {

        Long storeId = SafeParse.toLongOrNull(orderDto.getStoreId());
        Long factoryId = SafeParse.toLongOrNull(orderDto.getFactoryId());
        Long productId = SafeParse.toLongOrNull(orderDto.getProductId());
        Long materialId = SafeParse.toLongOrNull(orderDto.getMaterialId());
        Long colorId = SafeParse.toLongOrNull(orderDto.getColorId());
        Long assistantId = SafeParse.toLongOrNull(orderDto.getAssistantStoneId());
        boolean assistantStone = orderDto.isAssistantStone();

        // priority 추가
        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        OffsetDateTime createAt = StringToOffsetDateTime(orderDto.getCreateAt());
        OffsetDateTime shippingAt = StringToOffsetDateTime(orderDto.getShippingAt());

        Orders order = Orders.builder()
                .storeId(storeId)
                .storeName(orderDto.getStoreName())
                .storeGrade(orderDto.getStoreGrade())
                .storeHarry(SafeParse.toBigDecimalOrNull(orderDto.getStoreHarry()))
                .factoryId(factoryId)
                .factoryName(orderDto.getFactoryName())
                .orderNote(orderDto.getOrderNote())
                .productStatus(ProductStatus.RECEIPT)
                .orderStatus(OrderStatus.WAIT)
                .createAt(createAt)
                .shippingAt(shippingAt)
                .build();

        // orderStone 추가
        List<Long> stoneIds = new ArrayList<>();
        List<StoneDto.StoneInfo> storeInfos = orderDto.getStoneInfos();
        for (StoneDto.StoneInfo stoneInfo : storeInfos) {
            Long stoneId = SafeParse.toLongOrNull(stoneInfo.getStoneId());
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(stoneId)
                    .originStoneName(stoneInfo.getStoneName())
                    .originStoneWeight(SafeParse.toBigDecimalOrNull(stoneInfo.getStoneWeight()))
                    .stonePurchaseCost(stoneInfo.getPurchaseCost())
                    .stoneLaborCost(stoneInfo.getLaborCost())
                    .stoneAddLaborCost(stoneInfo.getAddLaborCost())
                    .stoneQuantity(stoneInfo.getQuantity())
                    .mainStone(stoneInfo.isMainStone())
                    .includeStone(stoneInfo.isIncludeStone())
                    .build();

            if (stoneId != null) {
                stoneIds.add(stoneId);
            }
            order.addOrderStone(orderStone);
        }

        // orderProduct 추가
        OrderProduct orderProduct = OrderProduct.builder()
                .productId(productId)
                .productName(orderDto.getProductName())
                .productSize(orderDto.getProductSize())
                .productFactoryName(orderDto.getProductFactoryName())
                .classificationId(SafeParse.toLongOrNull(orderDto.getClassificationId()))
                .classificationName(orderDto.getClassificationName())
                .setTypeId(SafeParse.toLongOrNull(orderDto.getSetTypeId()))
                .setTypeName(orderDto.getSetTypeName())
                .colorId(colorId)
                .colorName(orderDto.getColorName())
                .materialId(materialId)
                .materialName(orderDto.getMaterialName())
                .isProductWeightSale(orderDto.getIsProductWeightSale())
                .productPurchaseCost(orderDto.getProductPurchaseCost())
                .productLaborCost(orderDto.getProductLaborCost())
                .productAddLaborCost(orderDto.getProductAddLaborCost())
                .goldWeight(new BigDecimal(BigInteger.ZERO))
                .stoneWeight(orderDto.getStoneWeight())
                .orderMainStoneNote(orderDto.getMainStoneNote())
                .orderAssistanceStoneNote(orderDto.getAssistanceStoneNote())
                .assistantStoneId(assistantId)
                .stoneAddLaborCost(orderDto.getStoneAddLaborCost())
                .build();

        order.addOrderProduct(orderProduct);
        order.addPriority(priority);

        ordersRepository.save(order);

        // Outbox 이벤트 발행
        publishOrderCreateEvent(order, tenantId, accessToken, storeId, factoryId,
                                productId, materialId, colorId, nickname, stoneIds,
                                assistantStone, assistantId, orderStatus, orderDto);

        return order;
    }

    /**
     * 주문 수정
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Orders updateOrder(String tenantId, String accessToken, Long flowCode,
                             String orderStatus, OrderDto.Request orderDto, String nickname) {

        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName())
                .orElseThrow(() -> new IllegalArgumentException("등급: " + NOT_FOUND));

        OffsetDateTime createAt = StringToOffsetDateTime(orderDto.getCreateAt());
        OffsetDateTime shippingAt = StringToOffsetDateTime(orderDto.getShippingAt());

        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException("주문 수정: " + NOT_FOUND));

        order.updateOrderNote(orderDto.getOrderNote());
        order.updateCreateDate(createAt);
        order.updateShippingDate(shippingAt);
        order.addPriority(priority);

        // 거래처(스토어) 및 제조사(팩토리) 업데이트 (변경되었을 경우)
        if (StringUtils.hasText(orderDto.getStoreId())) {
            order.updateStore(
                SafeParse.toLongOrNull(orderDto.getStoreId()),
                orderDto.getStoreName(),
                orderDto.getStoreGrade(),
                SafeParse.toBigDecimalOrNull(orderDto.getStoreHarry())
            );
        }
        if (StringUtils.hasText(orderDto.getFactoryId())) {
            order.updateFactory(
                SafeParse.toLongOrNull(orderDto.getFactoryId()),
                orderDto.getFactoryName(),
                null
            );
        }

        // 스톤 값을 업데이트 (기존 그대로,추가,삭제)
        List<OrderStone> orderStones = order.getOrderStones();
        updateOrderStoneInfo(orderDto.getStoneInfos(), order, orderStones);

        // 변경 감지: DB 의 현재 productId 와 payload 의 신규 productId 를 비교한다.
        // (이전 구현은 동일 orderDto.getProductId() 를 두 번 파싱해 비교가 항상 true 였음 — 버그)
        OrderProduct orderProduct = order.getOrderProduct();
        Long currentProductId = orderProduct.getProductId();
        Long newProductId = SafeParse.toLongOrNull(orderDto.getProductId());
        if (newProductId == null || Objects.equals(newProductId, currentProductId)) {
            // productId 가 변하지 않았으므로 상세값만 갱신
            orderProduct.updateOrderProductInfo(
                    orderDto.getStoneWeight(),
                    orderDto.getProductPurchaseCost(),
                    orderDto.getProductLaborCost(),
                    orderDto.getProductAddLaborCost(),
                    orderDto.getMainStoneNote(),
                    orderDto.getAssistanceStoneNote(),
                    orderDto.getProductSize()
            );
        } else {
            // productId 가 변경되었으므로 함께 갱신
            orderProduct.updateOrderProductInfo(
                    newProductId,
                    orderDto.getStoneWeight(),
                    orderDto.getProductPurchaseCost(),
                    orderDto.getProductLaborCost(),
                    orderDto.getProductAddLaborCost(),
                    orderDto.getMainStoneNote(),
                    orderDto.getAssistanceStoneNote(),
                    orderDto.getProductSize()
            );
        }

        // 상품 속성(재질/색상/분류/세트타입)도 수정 반영
        // setTypeId/classificationId는 선택 필드이므로 null 허용
        orderProduct.updateOrderProduct(
                orderDto.getProductName(),
                orderDto.getProductFactoryName(),
                SafeParse.toLongOrNull(orderDto.getMaterialId()),
                orderDto.getMaterialName(),
                SafeParse.toLongOrNull(orderDto.getColorId()),
                orderDto.getColorName(),
                SafeParse.toLongOrNull(orderDto.getClassificationId()),
                orderDto.getClassificationName(),
                SafeParse.toLongOrNull(orderDto.getSetTypeId()),
                orderDto.getSetTypeName()
        );

        ordersRepository.save(order);

        // Outbox 이벤트 발행
        publishOrderUpdateEvent(order, tenantId, accessToken, orderStatus, orderProduct, orderDto, nickname);

        return order;
    }

    /**
     * 주문 삭제
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteOrder(Long flowCode, String role) {
        if (!role.equals("ADMIN") && !role.equals("USER")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException("flowCode: " + NOT_FOUND));

        order.updateOrderStatus(OrderStatus.DELETED);
        order.deletedOrder(OffsetDateTime.now());
    }

    /**
     * 주문 생성 Outbox 이벤트 발행
     */
    private void publishOrderCreateEvent(Orders order, String tenantId, String accessToken,
                                        Long storeId, Long factoryId, Long productId,
                                        Long materialId, Long colorId, String nickname,
                                        List<Long> stoneIds, boolean assistantStone,
                                        Long assistantId, String orderStatus, OrderDto.Request orderDto) {

        OrderAsyncRequested.OrderAsyncRequestedBuilder orderAsyncRequestedBuilder = OrderAsyncRequested.builder()
                .eventId(UUID.randomUUID().toString())
                .flowCode(order.getFlowCode())
                .tenantId(tenantId)
                .token(accessToken)
                .storeId(storeId)
                .factoryId(factoryId)
                .productId(productId)
                .materialId(materialId)
                .colorId(colorId)
                .nickname(nickname)
                .stoneIds(stoneIds)
                .assistantStone(assistantStone)
                .assistantStoneId(assistantId)
                .orderStatus(orderStatus);

        // 보조석 관련 - assistantStone 플래그와 관계없이 값이 있으면 설정
        if (orderDto.getAssistantStoneCreateAt() != null && !orderDto.getAssistantStoneCreateAt().isEmpty()) {
            OffsetDateTime assistantStoneCreateAt = StringToOffsetDateTime(orderDto.getAssistantStoneCreateAt());
            orderAsyncRequestedBuilder.assistantStoneCreateAt(assistantStoneCreateAt);
        }

        try {
            OutboxEvent outboxEvent = new OutboxEvent(
                    "order.create.requested",
                    order.getFlowCode().toString(),
                    objectMapper.writeValueAsString(orderAsyncRequestedBuilder.build()),
                    "ORDER_CREATE"
            );

            outboxEventRepository.save(outboxEvent);

            log.info("주문 생성 및 Outbox 저장 완료. OrderFlowCode: {}, EventID: {}",
                    order.getFlowCode(), outboxEvent.getId());

            eventPublisher.publishEvent(new OutboxCreatedEvent(tenantId));

        } catch (Exception e) {
            log.error("Outbox 저장 실패. OrderFlowCode: {}", order.getFlowCode(), e);
            throw new IllegalStateException("주문 생성 이벤트 저장 실패", e);
        }
    }

    /**
     * 주문 수정 Outbox 이벤트 발행
     */
    private void publishOrderUpdateEvent(Orders order, String tenantId, String accessToken,
                                        String orderStatus, OrderProduct orderProduct,
                                        OrderDto.Request orderDto, String nickname) {

        Long storeId = SafeParse.toLongOrNull(orderDto.getStoreId());
        Long factoryId = SafeParse.toLongOrNull(orderDto.getFactoryId());
        Long productId = SafeParse.toLongOrNull(orderDto.getProductId());
        Long materialId = SafeParse.toLongOrNull(orderDto.getMaterialId());
        Long colorId = SafeParse.toLongOrNull(orderDto.getColorId());
        Long assistantId = SafeParse.toLongOrNull(orderDto.getAssistantStoneId());

        OrderUpdateRequest.OrderUpdateRequestBuilder updateRequestBuilder = OrderUpdateRequest.builder()
                .eventId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .token(accessToken)
                .orderStatus(orderStatus)
                .flowCode(order.getFlowCode())
                .nickname(nickname);

        // productId 변경 시에만 추가
        Long newProductId = SafeParse.toLongOrNull(orderDto.getProductId());
        if (newProductId != null && productId != null && !newProductId.equals(productId)) {
            updateRequestBuilder.productId(productId);
        }

        // storeId 변경 확인 (null-safe 비교)
        if (!Objects.equals(storeId, order.getStoreId())) {
            updateRequestBuilder.storeId(storeId);
        }

        // factoryId 변경 확인 (null-safe 비교)
        if (!Objects.equals(factoryId, order.getFactoryId())) {
            updateRequestBuilder.factoryId(factoryId);
        }

        // materialId 변경 확인 (null-safe 비교)
        if (!Objects.equals(materialId, orderProduct.getMaterialId())) {
            updateRequestBuilder.materialId(materialId);
        }

        // colorId 변경 확인 (null-safe 비교)
        if (!Objects.equals(colorId, orderProduct.getColorId())) {
            updateRequestBuilder.colorId(colorId);
        }

        // assistantStone 처리 - 플래그와 관계없이 값이 있으면 설정
        boolean assistantStone = orderDto.isAssistantStone();
        updateRequestBuilder
                .assistantStone(assistantStone)
                .assistantStoneId(assistantId);

        if (orderDto.getAssistantStoneCreateAt() != null && !orderDto.getAssistantStoneCreateAt().isEmpty()) {
            OffsetDateTime assistantStoneCreateAt = StringToOffsetDateTime(orderDto.getAssistantStoneCreateAt());
            updateRequestBuilder.assistantStoneCreateAt(assistantStoneCreateAt);
        }

        try {
            OutboxEvent outboxEvent = new OutboxEvent(
                    "order.update.requested",
                    order.getFlowCode().toString(),
                    objectMapper.writeValueAsString(updateRequestBuilder.build()),
                    "ORDER_UPDATE"
            );

            outboxEventRepository.save(outboxEvent);

            log.info("주문 수정 및 Outbox 저장 완료. OrderFlowCode: {}, EventID: {}",
                    order.getFlowCode(), outboxEvent.getId());

            eventPublisher.publishEvent(new OutboxCreatedEvent(tenantId));

        } catch (Exception e) {
            log.error("Outbox 저장 실패. OrderFlowCode: {}", order.getFlowCode(), e);
            throw new IllegalStateException("주문 수정 이벤트 저장 실패", e);
        }
    }
}
