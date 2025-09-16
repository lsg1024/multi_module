package com.msa.order.local.order.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.order.dto.*;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.msa.order.local.order.entity.QOrderProduct.orderProduct;
import static com.msa.order.local.order.entity.QOrders.orders;
import static com.msa.order.local.priority.entitiy.QPriority.priority;
import static com.msa.order.local.stock.entity.QStock.stock;
import static java.util.stream.Collectors.*;

@Repository
public class OrderRepositoryImpl implements CustomOrderRepository {

    private final JPAQueryFactory query;

    public OrderRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public CustomPage<OrderQueryDto> findByOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable) {

        BooleanBuilder conditionBuilder = getSearchBuilder(inputCondition);
        BooleanExpression statusBuilder = getOrdersStatusBuilder(orderCondition);

        return getResponses(pageable, conditionBuilder, statusBuilder, false);
    }

    @Override
    public CustomPage<OrderQueryDto> findByExpectOrders(OrderDto.InputCondition inputCondition, OrderDto.ExpectCondition orderCondition, Pageable pageable) {
        BooleanBuilder conditionBuilder = getSearchBuilder(inputCondition);
        BooleanExpression statusBuilder = getExpectStatusBuilder(orderCondition);

        return getResponses(pageable, conditionBuilder, statusBuilder, false);
    }

    @Override
    public CustomPage<OrderQueryDto> findByDeletedOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable) {
        BooleanBuilder conditionBuilder = getSearchBuilder(inputCondition);
        BooleanExpression statusBuilder = getOrdersDeletedStatusBuilder(orderCondition);

        return getResponses(pageable, conditionBuilder, statusBuilder, true);
    }

    @Override
    public List<String> findByFilterFactories(OrderDto.OrderCondition condition) {

        BooleanExpression ordersStatusBuilder = getOrdersStatusBuilder(condition);

        return query
                .selectDistinct(orders.factoryName)
                .from(orders)
                .where(ordersStatusBuilder)
                .fetch();
    }

    @Override
    public List<String> findByFilterStores(OrderDto.OrderCondition condition) {
        BooleanExpression ordersStatusBuilder = getOrdersStatusBuilder(condition);

        return query
                .selectDistinct(orders.storeName)
                .from(orders)
                .where(ordersStatusBuilder)
                .fetch();
    }

    @Override
    public List<String> findByFilterSetType(OrderDto.OrderCondition condition) {
        BooleanExpression ordersStatusBuilder = getOrdersStatusBuilder(condition);

        return query
                .selectDistinct(orders.orderProduct.setType)
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .where(ordersStatusBuilder)
                .fetch();
    }

    @NotNull
    private CustomPage<OrderQueryDto> getResponses(Pageable pageable, BooleanBuilder conditionBuilder, BooleanExpression statusBuilder, Boolean orderDeleted) {

        JPQLQuery<Integer> stockQty = JPAExpressions
                .select(stock.stockCode.count().intValue())
                .from(stock)
                .where(
                        stock.stockDeleted.isFalse(),
                        stock.orderStatus.eq(OrderStatus.NORMAL),
                        stock.storeId.eq(1L),
                        stock.product.name.eq(orderProduct.productName),
                        stock.product.materialName.eq(orderProduct.materialName),
                        stock.product.colorName.eq(orderProduct.colorName)
                );

        List<OrderQueryDto> content = query
                .select(new QOrderQueryDto(
                        orders.orderProduct.productId,
                        orders.orderDate.stringValue(),
                        orders.orderExpectDate.stringValue(),
                        orders.flowCode.stringValue(),
                        orders.storeName,
                        orders.orderProduct.productName,
                        orders.orderProduct.materialName,
                        orderProduct.colorName,
                        orderProduct.setType,
                        orders.orderProduct.productSize,
                        stockQty,
                        orderProduct.orderMainStoneNote,
                        orderProduct.orderAssistanceStoneNote,
                        orders.orderNote,
                        orders.factoryName,
                        priority.priorityName,
                        orders.productStatus,
                        orders.orderStatus
                ))
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .join(orders.priority, priority)
                .where(
                        statusBuilder,
                        conditionBuilder,
                        orders.orderDeleted.eq(orderDeleted)
                )
                .orderBy(orders.orderDate.desc(), orders.flowCode.desc())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(orders.count())
                .from(orders)
                .where(
                        statusBuilder,
                        conditionBuilder,
                        orders.orderDeleted.eq(orderDeleted)
                );

        // querydsl에 list 직접 주입은 불가능 별도 쿼리 통해 (TUPLE) 반환해야됨
        Set<StockCondition> stockConditions = content.stream()
                .map(r -> new StockCondition(r.getProductName(), r.getMaterialName(), r.getColorName()))
                .collect(toSet());

        if (!stockConditions.isEmpty()) {
            // 키 OR 조건 구성
            BooleanBuilder keyOr = new BooleanBuilder();
            for (StockCondition k : stockConditions) {
                keyOr.or(stock.product.name.eq(k.pn())
                        .and(stock.product.materialName.eq(k.mn()))
                        .and(stock.product.colorName.eq(k.cn()))
                );
            }

            // 해당 키들의 stock.flowCode 리스트를 한 번에 조회
            List<Tuple> rows = query
                    .select(
                            stock.product.name,
                            stock.product.materialName,
                            stock.product.colorName,
                            stock.flowCode.stringValue()
                    )
                    .from(stock)
                    .where(
                            stock.stockDeleted.isFalse(),
                            stock.storeId.eq(1L),
                            stock.orderStatus.eq(OrderStatus.NORMAL),
                            keyOr
                    )
                    .fetch();

            // (p,m,c) → List<String> 맵으로 그룹핑
            Map<StockCondition, List<String>> map = rows.stream().collect(
                    groupingBy(
                            t -> new StockCondition(
                                    t.get(stock.product.name),
                                    t.get(stock.product.materialName),
                                    t.get(stock.product.colorName)
                            ),
                            mapping(
                                    t -> t.get(stock.flowCode.stringValue()),
                                    toList()
                            )
                    )
            );

            // DTO에 주입
            content.forEach(r -> {
                StockCondition k = new StockCondition(r.getProductName(), r.getMaterialName(), r.getColorName());
                r.setStockFlowCodes(map.getOrDefault(k, java.util.Collections.emptyList()));
            });
        }

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @NotNull
    private static BooleanBuilder getSearchBuilder(OrderDto.InputCondition orderCondition) {
        BooleanBuilder booleanInput = new BooleanBuilder();

        String searchInput = orderCondition.getSearchInput();
        if (StringUtils.hasText(searchInput)) {
            booleanInput.and(orderProduct.productName.containsIgnoreCase(searchInput));
            booleanInput.or(orders.storeName.containsIgnoreCase(searchInput));
            booleanInput.or(orders.factoryName.containsIgnoreCase(searchInput));
        }

        return booleanInput;
    }

    private static BooleanExpression getOrdersStatusBuilder(OrderDto.OrderCondition orderCondition) {
        String startAt = orderCondition.getStartAt();
        String endAt = orderCondition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneOffset.of("+09:00"));
        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        BooleanExpression createdBetween =
                orders.orderDate.between(startDateTime, endDateTime);

        BooleanExpression statusIsReceiptOrWaiting =
                orders.productStatus.in(ProductStatus.RECEIPT, ProductStatus.WAITING);

        BooleanExpression statusIsOrder =
                orders.orderStatus.in(OrderStatus.ORDER);

        if (StringUtils.hasText(orderCondition.getFactoryName())) {
            BooleanExpression hasFactory = orders.factoryName.eq(orderCondition.getFactoryName());
            statusIsReceiptOrWaiting.and(hasFactory);
        }
        if (StringUtils.hasText(orderCondition.getStoreName())) {
            BooleanExpression hasStore = orders.storeName.eq(orderCondition.getStoreName());
            statusIsReceiptOrWaiting.and(hasStore);
        }
        if (StringUtils.hasText(orderCondition.getSetTypeName())) {
            BooleanExpression hasSetType = orders.storeName.eq(orderCondition.getSetTypeName());
            statusIsReceiptOrWaiting.and(hasSetType);
        }

        return statusIsReceiptOrWaiting.and(statusIsOrder).and(createdBetween);
    }

    private static BooleanExpression getExpectStatusBuilder(OrderDto.ExpectCondition orderCondition) {
        String endAt = orderCondition.getEndAt();

        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        BooleanExpression createdBetween =
                orders.orderExpectDate.loe(endDateTime);

        BooleanExpression statusIsReceiptOrWaiting =
                orders.productStatus.in(ProductStatus.RECEIPT, ProductStatus.WAITING);

        BooleanExpression statusIsOrder =
                orders.orderStatus.in(OrderStatus.ORDER);

        return statusIsReceiptOrWaiting.and(statusIsOrder).and(createdBetween);
    }

    private static BooleanExpression getOrdersDeletedStatusBuilder(OrderDto.OrderCondition orderCondition) {
        String startAt = orderCondition.getStartAt();
        String endAt = orderCondition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneOffset.of("+09:00"));
        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        BooleanExpression deletedDate =
                orders.orderExpectDate.between(startDateTime, endDateTime);

        BooleanExpression statusIsReceiptOrWaiting =
                orders.productStatus.in(ProductStatus.RECEIPT, ProductStatus.WAITING);

        BooleanExpression statusIsOrder =
                orders.orderStatus.in(OrderStatus.ORDER);

        if (StringUtils.hasText(orderCondition.getFactoryName())) {
            BooleanExpression hasFactory = orders.factoryName.eq(orderCondition.getFactoryName());
            statusIsReceiptOrWaiting.and(hasFactory);
        }
        if (StringUtils.hasText(orderCondition.getStoreName())) {
            BooleanExpression hasStore = orders.storeName.eq(orderCondition.getStoreName());
            statusIsReceiptOrWaiting.and(hasStore);
        }
        if (StringUtils.hasText(orderCondition.getSetTypeName())) {
            BooleanExpression hasSetType = orders.storeName.eq(orderCondition.getSetTypeName());
            statusIsReceiptOrWaiting.and(hasSetType);
        }

        return statusIsReceiptOrWaiting.and(statusIsOrder).and(deletedDate);
    }

}
