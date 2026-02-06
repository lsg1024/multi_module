package com.msa.order.local.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.order.global.dto.OutboxCreatedEvent;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.global.kafka.dto.OrderUpdateRequest;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

        Long storeId = Long.valueOf(orderDto.getStoreId());
        Long factoryId = Long.valueOf(orderDto.getFactoryId());
        Long productId = Long.valueOf(orderDto.getProductId());
        Long materialId = Long.valueOf(orderDto.getMaterialId());
        Long colorId = Long.valueOf(orderDto.getColorId());
        Long assistantId = Long.valueOf(orderDto.getAssistantStoneId());
        boolean assistantStone = orderDto.isAssistantStone();

        // priority 추가
        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        OffsetDateTime createAt = StringToOffsetDateTime(orderDto.getCreateAt());
        OffsetDateTime shippingAt = StringToOffsetDateTime(orderDto.getShippingAt());

        Orders order = Orders.builder()
                .storeId(Long.parseLong(orderDto.getStoreId()))
                .storeName(orderDto.getStoreName())
                .storeGrade(orderDto.getStoreGrade())
                .storeHarry(new BigDecimal(orderDto.getStoreHarry()))
                .factoryId(Long.parseLong(orderDto.getFactoryId()))
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
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(Long.valueOf(stoneInfo.getStoneId()))
                    .originStoneName(stoneInfo.getStoneName())
                    .originStoneWeight(new BigDecimal(stoneInfo.getStoneWeight()))
                    .stonePurchaseCost(stoneInfo.getPurchaseCost())
                    .stoneLaborCost(stoneInfo.getLaborCost())
                    .stoneAddLaborCost(stoneInfo.getAddLaborCost())
                    .stoneQuantity(stoneInfo.getQuantity())
                    .mainStone(stoneInfo.isMainStone())
                    .includeStone(stoneInfo.isIncludeStone())
                    .build();

            stoneIds.add(Long.valueOf(stoneInfo.getStoneId()));
            order.addOrderStone(orderStone);
        }

        // orderProduct 추가
        OrderProduct orderProduct = OrderProduct.builder()
                .productId(productId)
                .productName(orderDto.getProductName())
                .productSize(orderDto.getProductSize())
                .productFactoryName(orderDto.getProductFactoryName())
                .classificationId(Long.valueOf(orderDto.getClassificationId()))
                .classificationName(orderDto.getClassificationName())
                .setTypeId(Long.valueOf(orderDto.getSetTypeId()))
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

        // 스톤 값을 업데이트 (기존 그대로,추가,삭제)
        List<OrderStone> orderStones = order.getOrderStones();
        updateOrderStoneInfo(orderDto.getStoneInfos(), order, orderStones);

        Long productId = Long.valueOf(orderDto.getProductId());
        OrderProduct orderProduct = order.getOrderProduct();
        Long newProductId = Long.parseLong(orderDto.getProductId());
        if (newProductId.equals(productId)) {
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
            orderProduct.updateOrderProductInfo(
                    productId,
                    orderDto.getStoneWeight(),
                    orderDto.getProductPurchaseCost(),
                    orderDto.getProductLaborCost(),
                    orderDto.getProductAddLaborCost(),
                    orderDto.getMainStoneNote(),
                    orderDto.getAssistanceStoneNote(),
                    orderDto.getProductSize()
            );
        }

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

        Long storeId = Long.valueOf(orderDto.getStoreId());
        Long factoryId = Long.valueOf(orderDto.getFactoryId());
        Long productId = Long.valueOf(orderDto.getProductId());
        Long materialId = Long.valueOf(orderDto.getMaterialId());
        Long colorId = Long.valueOf(orderDto.getColorId());
        Long assistantId = Long.valueOf(orderDto.getAssistantStoneId());

        OrderUpdateRequest.OrderUpdateRequestBuilder updateRequestBuilder = OrderUpdateRequest.builder()
                .eventId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .token(accessToken)
                .orderStatus(orderStatus)
                .flowCode(order.getFlowCode())
                .nickname(nickname);

        // productId 변경 시에만 추가
        Long newProductId = Long.parseLong(orderDto.getProductId());
        if (!newProductId.equals(productId)) {
            updateRequestBuilder.productId(productId);
        }

        // storeId 변경 확인
        if (!storeId.equals(order.getStoreId())) {
            updateRequestBuilder.storeId(storeId);
        }

        // factoryId 변경 확인
        if (!factoryId.equals(order.getFactoryId())) {
            updateRequestBuilder.factoryId(factoryId);
        }

        // materialId 변경 확인
        if (!materialId.equals(orderProduct.getMaterialId())) {
            updateRequestBuilder.materialId(materialId);
        }

        // colorId 변경 확인
        if (!colorId.equals(orderProduct.getColorId())) {
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
