package com.msa.order.local.domain.order.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.order.dto.OrderDto;
import com.msa.order.local.domain.order.dto.QOrderDto_Response;
import com.msa.order.local.domain.order.entity.OrderStatus;
import com.msa.order.local.domain.order.entity.OrderStone;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.msa.order.local.domain.order.entity.QOrderProduct.orderProduct;
import static com.msa.order.local.domain.order.entity.QOrderStone.orderStone;
import static com.msa.order.local.domain.order.entity.QOrders.orders;
import static com.msa.order.local.domain.order.entity.QStatusHistory.statusHistory;
import static com.msa.order.local.domain.priority.entitiy.QPriority.priority;

@Repository
public class OrderRepositoryImpl implements CustomOrderRepository {

    private final JPAQueryFactory query;

    public OrderRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public OrderDto.ResponseDetail findByOrderId(Long orderId) {
        // 1. ORDER_STONE의 main/sub 집계 자바에서 계산
        List<OrderStone> orderStones = query
                .selectFrom(orderStone)
                .where(orderStone.order.orderId.eq(orderId))
                .fetch();

        int mainCostSum = orderStones.stream()
                .filter(OrderStone::getProductStoneMain)
                .mapToInt(stone -> stone.getStoneLaborCost() * stone.getStoneQuantity())
                .sum();

        int subSum = orderStones.stream()
                .filter(stone -> !stone.getProductStoneMain())
                .mapToInt(stone -> stone.getStoneLaborCost() * stone.getStoneQuantity())
                .sum();

        int mainQuantitySum = orderStones.stream()
                .filter(OrderStone::getProductStoneMain)
                .mapToInt(OrderStone::getStoneQuantity)
                .sum();

        int subQuantitySum = orderStones.stream()
                .filter(stone -> !stone.getProductStoneMain())
                .mapToInt(OrderStone::getStoneQuantity)
                .sum();

        // 2. 주문 기본 정보는 Tuple로 select
        Tuple tuple = query
                .select(
                        statusHistory.createAt,             // 0: OffsetDateTime
                        priority.priorityDate,              // 1: Integer
                        orders.orderId,                     // 2: Long
                        orders.orderCode,                   // 3: String
                        orders.storeName,                   // 4: String
                        orders.orderProduct.productLaborCost,     // 6: Integer
                        orders.orderProduct.productAddLaborCost,  // 7: Integer
                        orders.orderProduct.productName,     // 8: String
                        orderProduct.classificationName,     // 9: String
                        orders.orderProduct.materialName,    // 10: String
                        orderProduct.colorName,              // 11: String
                        orderProduct.productSize,            // 12: String
                        orders.orderNote,                    // 13: String
                        orders.orderProduct.factoryName,     // 14: String
                        priority.priorityName,               // 15: String
                        orders.orderStatus                   // 16: Enum(OrderStatus)
                )
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .join(orders.statusHistory, statusHistory)
                .join(orders.priority, priority)
                .where(orders.orderId.eq(orderId))
                .fetchOne();

        if (tuple == null) return null; // or throw new NotFoundException();

        // 3. Java에서 날짜 계산
        OffsetDateTime createAt = tuple.get(0, OffsetDateTime.class);
        Integer priorityDate = tuple.get(1, Integer.class);
        OffsetDateTime deliveryAt = (createAt != null && priorityDate != null)
                ? createAt.plusDays(priorityDate)
                : null;

        // 4. DTO 직접 생성 (String 변환)
        return new OrderDto.ResponseDetail(
                createAt != null ? createAt.toString() : null,                // createAt
                deliveryAt != null ? deliveryAt.toString() : null,            // deliveryAt
                tuple.get(2, Long.class) != null ? tuple.get(2, Long.class).toString() : null, // orderId
                tuple.get(3, String.class),                                   // orderCode
                tuple.get(4, String.class),                                   // storeName
                tuple.get(5, Integer.class) != null ? tuple.get(5, Integer.class).toString() : null, // productLaborCost
                tuple.get(6, Integer.class) != null ? tuple.get(6, Integer.class).toString() : null, // productAddLaborCost
                String.valueOf(mainCostSum),                                  // productStoneMainLaborCost
                String.valueOf(subSum),                                       // productStoneAssistanceLaborCost
                String.valueOf(mainQuantitySum),                              // productStoneMainQuantity
                String.valueOf(subQuantitySum),                               // productStoneAssistanceQuantity
                tuple.get(7, String.class),                                   // productName
                tuple.get(8, String.class),                                   // classification
                tuple.get(9, String.class),                                  // materialName
                tuple.get(10, String.class),                                  // colorName
                tuple.get(11, String.class),                                  // productSize
                tuple.get(12, String.class),                                  // orderNote
                tuple.get(13, String.class),                                  // factoryName
                tuple.get(14, String.class),                                  // priority
                tuple.get(15, OrderStatus.class) != null
                        ? tuple.get(15, OrderStatus.class).name() : null          // orderStatus (enum → String)
        );
    }

    @Override
    public CustomPage<OrderDto.Response> findByOrders(OrderDto.Condition condition, Pageable pageable) {

        String startAt = condition.getStartAt();
        String endAt = condition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneOffset.of("+09:00"));
        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        BooleanExpression booleanDate = statusHistory.orderStatus.eq(OrderStatus.ORDER)
                .and(statusHistory.createAt.between(startDateTime, endDateTime));

        BooleanBuilder booleanInput = new BooleanBuilder();

        String searchInput = condition.getSearchInput();
        if (StringUtils.hasText(searchInput)) {
            booleanInput.and(orderProduct.productName.containsIgnoreCase(searchInput));
            booleanInput.or(orders.storeName.containsIgnoreCase(searchInput));
            booleanInput.or(orderProduct.factoryName.containsIgnoreCase(searchInput));
        }


        List<OrderDto.Response> content = query
                .select(new QOrderDto_Response(
                        orders.orderId.stringValue(),
                        orders.orderCode,
                        orders.storeName,
                        orders.orderProduct.productName,
                        orderProduct.productSize,
                        orders.orderNote,
                        orderProduct.factoryName,
                        orderProduct.materialName,
                        orderProduct.colorName,
                        priority.priorityName,
                        statusHistory.createAt.stringValue(),
                        orders.orderStatus.stringValue()
                ))
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .join(orders.statusHistory, statusHistory)
                .join(orders.priority, priority)
                .where(
                        booleanDate,
                        booleanInput
                )
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(orders.count())
                .from(orders);

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }
}
