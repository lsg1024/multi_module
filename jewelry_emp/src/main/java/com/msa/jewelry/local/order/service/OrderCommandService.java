package com.msa.jewelry.local.order.service;

import com.msa.jewelry.global.util.SafeParse;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneView;
import com.msa.jewelry.local.assistant_stone.service.AssistantStoneService;
import com.msa.jewelry.local.order.dto.OrderDto;
import com.msa.jewelry.local.order.dto.StoneDto;
import com.msa.jewelry.local.order.entity.OrderProduct;
import com.msa.jewelry.local.order.entity.OrderStone;
import com.msa.jewelry.local.order.entity.Orders;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.ProductStatus;
import com.msa.jewelry.local.order.repository.OrdersRepository;
import com.msa.jewelry.local.priority.entity.Priority;
import com.msa.jewelry.local.priority.repository.PriorityRepository;
import com.msa.jewelry.local.stone.service.StoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.jewelry.global.exception.ExceptionMessage.NOT_FOUND_STONE;
import static com.msa.jewelry.global.util.DateConversionUtil.StringToLocalDateTime;
import static com.msa.jewelry.local.order.util.StoneUtil.updateOrderStoneInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrdersRepository ordersRepository;
    private final PriorityRepository priorityRepository;
    private final AssistantStoneService assistantStoneService;
    private final StoneService stoneService;

    @Transactional(propagation = Propagation.MANDATORY)
    public Orders createOrder(String orderStatusName, OrderDto.Request orderDto) {

        Long storeId = SafeParse.toLongOrNull(orderDto.getStoreId());
        Long factoryId = SafeParse.toLongOrNull(orderDto.getFactoryId());
        Long productId = SafeParse.toLongOrNull(orderDto.getProductId());
        Long materialId = SafeParse.toLongOrNull(orderDto.getMaterialId());
        Long colorId = SafeParse.toLongOrNull(orderDto.getColorId());
        Long assistantId = SafeParse.toLongOrNull(orderDto.getAssistantStoneId());
        boolean assistantStone = orderDto.isAssistantStone();

        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        LocalDateTime createAt = StringToLocalDateTime(orderDto.getCreateAt());
        LocalDateTime shippingAt = StringToLocalDateTime(orderDto.getShippingAt());

        Orders order = Orders.builder()
                .storeId(storeId)
                .storeGrade(orderDto.getStoreGrade())
                .storeHarry(SafeParse.toBigDecimalOrNull(orderDto.getStoreHarry()))
                .factoryId(factoryId)
                .factoryHarry(SafeParse.toBigDecimalOrNull(orderDto.getFactoryHarry()))
                .orderNote(orderDto.getOrderNote())
                .productStatus(ProductStatus.RECEIPT)
                .orderStatus(OrderStatus.WAIT)
                .createAt(createAt)
                .shippingAt(shippingAt)
                .build();

        // OrderStone 추가
        List<Long> stoneIds = new ArrayList<>();
        for (StoneDto.StoneInfo stoneInfo : orderDto.getStoneInfos()) {
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
            if (stoneId != null) stoneIds.add(stoneId);
            order.addOrderStone(orderStone);
        }

        // OrderProduct 추가
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

        // Stone 마스터 존재 검증 — 잘못된 stoneId 가 들어왔는지 보장.
        for (Long stoneId : stoneIds) {
            if (!stoneService.existsStoneId(stoneId)) {
                throw new IllegalArgumentException(NOT_FOUND_STONE);
            }
        }

        // 보조석 ID 가 있고 이름이 비어있으면 한 번 fetch 해서 채움
        if (assistantId != null && !StringUtils.hasText(orderProduct.getAssistantStoneName())) {
            try {
                AssistantStoneView v = assistantStoneService.getAssistantStoneView(assistantId);
                orderProduct.updateOrderProductAssistantStone(
                        assistantStone, v.assistantStoneId(), v.assistantStoneName(),
                        StringUtils.hasText(orderDto.getAssistantStoneCreateAt())
                                ? StringToLocalDateTime(orderDto.getAssistantStoneCreateAt()) : null);
            } catch (Exception e) {
                log.warn("보조석({}) fetch 실패 — FE 값 유지. {}", assistantId, e.getMessage());
            }
        }

        // 상태 전이 — orderStatusName 이 정상 enum 이면 적용, 아니면 WAIT 유지.
        if (StringUtils.hasText(orderStatusName)) {
            try {
                order.updateOrderStatus(OrderStatus.valueOf(orderStatusName.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 orderStatus={} — WAIT 유지", orderStatusName);
            }
        }

        log.info("주문 생성 완료. flowCode={}", order.getFlowCode());
        return order;
    }

    /**
     * 주문 수정.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Orders updateOrder(Long flowCode, String orderStatusName, OrderDto.Request orderDto) {

        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName())
                .orElseThrow(() -> new IllegalArgumentException("등급: " + NOT_FOUND));

        LocalDateTime createAt = StringToLocalDateTime(orderDto.getCreateAt());
        LocalDateTime shippingAt = StringToLocalDateTime(orderDto.getShippingAt());

        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException("주문 수정: " + NOT_FOUND));

        order.updateOrderNote(orderDto.getOrderNote());
        order.updateCreateDate(createAt);
        order.updateShippingDate(shippingAt);
        order.addPriority(priority);

        // 거래처/제조사 변경
        Long newStoreId = SafeParse.toLongOrNull(orderDto.getStoreId());
        Long newFactoryId = SafeParse.toLongOrNull(orderDto.getFactoryId());
        Long newMaterialId = SafeParse.toLongOrNull(orderDto.getMaterialId());
        Long newColorId = SafeParse.toLongOrNull(orderDto.getColorId());
        Long newAssistantId = SafeParse.toLongOrNull(orderDto.getAssistantStoneId());

        if (newStoreId != null) {
            order.updateStore(newStoreId, orderDto.getStoreGrade(),
                    SafeParse.toBigDecimalOrNull(orderDto.getStoreHarry()));
        }
        if (newFactoryId != null) {
            order.updateFactory(newFactoryId, SafeParse.toBigDecimalOrNull(orderDto.getFactoryHarry()));
        }

        List<OrderStone> orderStones = order.getOrderStones();
        updateOrderStoneInfo(orderDto.getStoneInfos(), order, orderStones);

        OrderProduct orderProduct = order.getOrderProduct();
        Long currentProductId = orderProduct.getProductId();
        Long newProductId = SafeParse.toLongOrNull(orderDto.getProductId());
        if (newProductId == null || Objects.equals(newProductId, currentProductId)) {
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

        orderProduct.updateOrderProduct(
                orderDto.getProductName(),
                orderDto.getProductFactoryName(),
                newMaterialId,
                orderDto.getMaterialName(),
                newColorId,
                orderDto.getColorName(),
                SafeParse.toLongOrNull(orderDto.getClassificationId()),
                orderDto.getClassificationName(),
                SafeParse.toLongOrNull(orderDto.getSetTypeId()),
                orderDto.getSetTypeName()
        );

        OrderProduct currentOrderProduct = order.getOrderProduct();
        LocalDateTime assistantCreateAtParsed = StringUtils.hasText(orderDto.getAssistantStoneCreateAt())
                ? StringToLocalDateTime(orderDto.getAssistantStoneCreateAt()) : null;
        String resolvedAssistantName = orderDto.getAssistantStoneName();
        if (newAssistantId != null && !StringUtils.hasText(resolvedAssistantName)) {
            try {
                AssistantStoneView v = assistantStoneService.getAssistantStoneView(newAssistantId);
                resolvedAssistantName = v.assistantStoneName();
            } catch (Exception e) {
                log.warn("보조석({}) fetch 실패 — FE 값 유지. {}", newAssistantId, e.getMessage());
            }
        }
        currentOrderProduct.updateOrderProductAssistantStone(
                orderDto.isAssistantStone(), newAssistantId, resolvedAssistantName, assistantCreateAtParsed);

        // 상태 전이 — orderStatusName 이 정상 enum 이면 적용.
        if (order.getOrderStatus() == OrderStatus.WAIT && StringUtils.hasText(orderStatusName)) {
            try {
                order.updateOrderStatus(OrderStatus.valueOf(orderStatusName.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 orderStatus={} — WAIT 유지", orderStatusName);
            }
        }

        ordersRepository.save(order);

        log.info("주문 수정 완료. flowCode={}", order.getFlowCode());
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
        order.deletedOrder(LocalDateTime.now());
    }

}
