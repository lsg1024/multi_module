package com.msa.order.local.order.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.global.excel.dto.OrderExcelQueryDto;
import com.msa.order.global.excel.dto.QOrderExcelQueryDto;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.dto.OrderQueryDto;
import com.msa.order.local.order.dto.QOrderQueryDto;
import com.msa.order.local.order.dto.StockCondition;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.msa.order.local.order.entity.QOrderProduct.orderProduct;
import static com.msa.order.local.order.entity.QOrders.orders;
import static com.msa.order.local.order.entity.QStatusHistory.statusHistory;
import static com.msa.order.local.priority.entitiy.QPriority.priority;
import static com.msa.order.local.stock.entity.QStock.stock;
import static java.util.stream.Collectors.*;

@Slf4j
@Repository
public class OrderRepositoryImpl implements CustomOrderRepository {

    private final JPAQueryFactory query;
    private final static Long DEFAULT_STORE_STOCK_ID = 1L; // 매장 재고 고정 아이디

    public OrderRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public CustomPage<OrderQueryDto> findByOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable) {

        BooleanBuilder conditionBuilder = getSearchBuilder(inputCondition);
        BooleanExpression statusBuilder = getOrdersStatusBuilder(orderCondition);
        BooleanBuilder optionBuilder = getOptionBuilder(orderCondition.getOptionCondition());

        return getResponse(pageable, orderCondition.getSortCondition(), conditionBuilder, statusBuilder, optionBuilder, false);
    }

    @Override
    public CustomPage<OrderQueryDto> findByFixOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable) {
        BooleanBuilder conditionBuilder = getSearchBuilder(inputCondition);
        BooleanExpression statusBuilder = getOrdersStatusBuilder(orderCondition);
        BooleanBuilder optionBuilder = getOptionBuilder(orderCondition.getOptionCondition());

        return getResponse(pageable, orderCondition.getSortCondition(), conditionBuilder, statusBuilder, optionBuilder, false);
    }

    @Override
    public CustomPage<OrderQueryDto> findByDeliveryOrders(OrderDto.InputCondition inputCondition, OrderDto.ExpectCondition expectCondition, Pageable pageable) {
        BooleanBuilder conditionBuilder = getSearchBuilder(inputCondition);
        BooleanExpression statusBuilder = getExpectStatusBuilder(expectCondition);
        BooleanBuilder optionBuilder = getOptionBuilder(expectCondition.getOptionCondition());

        return getResponse(pageable, expectCondition.getSortCondition(), conditionBuilder, statusBuilder, optionBuilder, false);
    }

    @Override
    public CustomPage<OrderQueryDto> findByDeletedOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable) {
        BooleanBuilder conditionBuilder = getSearchBuilder(inputCondition);
        BooleanExpression statusBuilder = getDeleteBuilder(orderCondition);
        BooleanBuilder optionBuilder = getOptionBuilder(orderCondition.getOptionCondition());

        return getResponse(pageable, orderCondition.getSortCondition(), conditionBuilder, statusBuilder, optionBuilder,  true);
    }

    @Override
    public List<String> findByFilterFactories(OrderDto.OrderCondition condition) {

        BooleanExpression ordersStatusBuilder;
        if (condition.getOrderStatus().equals("EXPECT")) {
            ordersStatusBuilder = getExpectBuilder(condition);
        } else {
            ordersStatusBuilder = getOrdersStatusBuilder(condition);
        }

        return query
                .selectDistinct(orders.factoryName)
                .from(orders)
                .where(ordersStatusBuilder)
                .fetch();
    }

    @Override
    public List<String> findByFilterStores(OrderDto.OrderCondition condition) {

        BooleanExpression ordersStatusBuilder;
        if (condition.getOrderStatus().equals("EXPECT")) {
            ordersStatusBuilder = getExpectBuilder(condition);
        } else {
            ordersStatusBuilder = getOrdersStatusBuilder(condition);
        }

        return query
                .selectDistinct(orders.storeName)
                .from(orders)
                .where(ordersStatusBuilder)
                .fetch();
    }

    @Override
    public List<String> findByFilterSetType(OrderDto.OrderCondition condition) {

        BooleanExpression ordersStatusBuilder;
        if (condition.getOrderStatus().equals("EXPECT")) {
            ordersStatusBuilder = getExpectBuilder(condition);
        } else {
            ordersStatusBuilder = getOrdersStatusBuilder(condition);
        }

        return query
                .selectDistinct(orders.orderProduct.setTypeName)
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .where(ordersStatusBuilder)
                .fetch();
    }

    @Override
    public List<String> findByFilterColor(OrderDto.OrderCondition condition) {

        BooleanExpression ordersStatusBuilder;
        if (condition.getOrderStatus().equals("EXPECT")) {
            ordersStatusBuilder = getExpectBuilder(condition);
        } else {
            ordersStatusBuilder = getOrdersStatusBuilder(condition);
        }
        return query
                .selectDistinct(orders.orderProduct.colorName)
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .where(ordersStatusBuilder)
                .fetch();
    }

    @Override
    public List<OrderExcelQueryDto> findByExcelData(OrderDto.OrderCondition condition) {

        BooleanBuilder optionBuilder = getOptionBuilder(condition.getOptionCondition());
        BooleanExpression ordersReceiptStatusBuilder = getOrdersReceiptStatusBuilder(condition);

        return query
                .select(new QOrderExcelQueryDto(
                        orders.factoryName,
                        orderProduct.productFactoryName, // productFactoryName으로 업데이트 필요
                        orderProduct.materialName,
                        orderProduct.colorName,
                        orderProduct.orderMainStoneNote,
                        orderProduct.orderAssistanceStoneNote,
                        orderProduct.productSize,
                        orders.orderNote
                ))
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .where(
                        optionBuilder,
                        ordersReceiptStatusBuilder
                )
                .orderBy(orders.factoryName.asc())
                .fetch();
    }

    @NotNull
    private CustomPage<OrderQueryDto> getResponse(Pageable pageable, OrderDto.SortCondition sortCondition, BooleanBuilder conditionBuilder, BooleanExpression statusBuilder, BooleanBuilder optionBuilder, Boolean orderDeleted) {

        JPQLQuery<Integer> stockQty = JPAExpressions
                .select(stock.stockCode.count().intValue())
                .from(stock)
                .where(
                        stock.stockDeleted.isFalse(),
                        stock.orderStatus.between(OrderStatus.NORMAL, OrderStatus.STOCK),
                        stock.storeId.eq(DEFAULT_STORE_STOCK_ID),
                        stock.product.productName.eq(orderProduct.productName),
                        stock.product.materialName.eq(orderProduct.materialName),
                        stock.product.colorName.eq(orderProduct.colorName)
                );

        OrderSpecifier<?>[] orderSpecifiers = createOrderSpecifiers(sortCondition);

        List<OrderQueryDto> content = query
                .select(new QOrderQueryDto(
                        orders.orderProduct.productId,
                        orders.createAt.stringValue(),
                        orders.shippingAt.stringValue(),
                        orders.flowCode.stringValue(),
                        orders.storeName,
                        orders.orderProduct.productName,
                        orders.orderProduct.materialName,
                        orderProduct.colorName,
                        orderProduct.setTypeName,
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
                        optionBuilder,
                        orders.orderDeleted.eq(orderDeleted)
                )
                .orderBy(orderSpecifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(orders.count())
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .where(
                        statusBuilder,
                        conditionBuilder,
                        optionBuilder,
                        orders.orderDeleted.eq(orderDeleted)
                );

        // querydsl에 list 직접 주입은 불가능 별도 쿼리 통해 (TUPLE) 반환해야됨
        Set<StockCondition> stockConditions = content.stream()
                .map(r -> new StockCondition(r.getProductName(), r.getMaterialName(), r.getColorName()))
                .collect(toSet());

        if (!stockConditions.isEmpty()) {
            BooleanBuilder keyOr = new BooleanBuilder();
            for (StockCondition k : stockConditions) {

                BooleanExpression productNameMatch = (k.pn() == null)
                        ? stock.product.productName.isNull()
                        : stock.product.productName.eq(k.pn());

                BooleanExpression materialNameMatch = (k.mn() == null)
                        ? stock.product.materialName.isNull()
                        : stock.product.materialName.eq(k.mn());

                BooleanExpression colorNameMatch = (k.cn() == null)
                        ? stock.product.colorName.isNull()
                        : stock.product.colorName.eq(k.cn());

                keyOr.or(productNameMatch.and(materialNameMatch).and(colorNameMatch));
            }

            // 해당 키들의 stock.flowCode 리스트를 한 번에 조회
            List<Tuple> rows = query
                    .select(
                            stock.product.productName,
                            stock.product.materialName,
                            stock.product.colorName,
                            stock.flowCode.stringValue()
                    )
                    .from(stock)
                    .where(
                            stock.stockDeleted.isFalse(),
                            stock.storeId.eq(DEFAULT_STORE_STOCK_ID),
                            stock.orderStatus.eq(OrderStatus.NORMAL),
                            keyOr
                    )
                    .fetch();

            // (p,m,c) → List<String> 맵으로 그룹핑
            Map<StockCondition, List<String>> map = rows.stream().collect(
                    groupingBy(
                            t -> new StockCondition(
                                    t.get(stock.product.productName),
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
                r.setStockFlowCodes(map.getOrDefault(k, List.of()));
            });
        }

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @NotNull
    private static BooleanBuilder getSearchBuilder(OrderDto.InputCondition orderCondition) {
        BooleanBuilder searchBuilder = new BooleanBuilder();

        String searchInput = orderCondition.getSearchInput();
        if (StringUtils.hasText(searchInput)) {
            searchBuilder.and(
                    orderProduct.productName.containsIgnoreCase(searchInput)
                            .or(orders.storeName.containsIgnoreCase(searchInput))
                            .or(orders.factoryName.containsIgnoreCase(searchInput))
            );
        }

        return searchBuilder;
    }

    @NotNull
    private static BooleanBuilder getOptionBuilder(OrderDto.OptionCondition optionCondition) {
        BooleanBuilder booleanOption = new BooleanBuilder();

        if (StringUtils.hasText(optionCondition.getStoreName())) {
            booleanOption.and(orders.storeName.containsIgnoreCase(optionCondition.getStoreName()));
        }

        if (StringUtils.hasText(optionCondition.getFactoryName())) {
            booleanOption.and(orders.factoryName.containsIgnoreCase(optionCondition.getFactoryName()));
        }

        if (StringUtils.hasText(optionCondition.getSetTypeName())) {
            booleanOption.and(orderProduct.setTypeName.containsIgnoreCase(optionCondition.getSetTypeName()));
        }

        if (StringUtils.hasText(optionCondition.getColorName())) {
            booleanOption.and(orderProduct.colorName.containsIgnoreCase(optionCondition.getColorName()));
        }

        return booleanOption;
    }

    private static BooleanExpression getOrdersStatusBuilder(OrderDto.OrderCondition orderCondition) {
        String startAt = orderCondition.getStartAt();
        String endAt = orderCondition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneOffset.of("+09:00"));
        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        BooleanExpression createdBetween =
                orders.createAt.between(startDateTime, endDateTime);

        BooleanExpression status = orders.orderStatus.between(OrderStatus.valueOf(orderCondition.getOrderStatus()), OrderStatus.STOCK);

        return status.and(createdBetween);
    }

    private static BooleanExpression getExpectBuilder(OrderDto.OrderCondition orderCondition) {
        String startAt = orderCondition.getStartAt();
        String endAt = orderCondition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneOffset.of("+09:00"));
        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        BooleanExpression createdBetween =
                orders.createAt.between(startDateTime, endDateTime);

        BooleanExpression status = orders.orderStatus.between(OrderStatus.ORDER, OrderStatus.FIX);

        return status.and(createdBetween);
    }

    private static BooleanExpression getDeleteBuilder(OrderDto.OrderCondition orderCondition) {
        String startAt = orderCondition.getStartAt();
        String endAt = orderCondition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneOffset.of("+09:00"));
        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        BooleanExpression createdBetween =
                orders.createAt.between(startDateTime, endDateTime);

        BooleanExpression status = orders.orderStatus.eq(OrderStatus.valueOf(orderCondition.getOrderStatus()));

        return status.and(createdBetween);
    }

    private static BooleanExpression getOrdersReceiptStatusBuilder(OrderDto.OrderCondition orderCondition) {
        String startAt = orderCondition.getStartAt();
        String endAt = orderCondition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneOffset.of("+09:00"));
        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        BooleanExpression createdBetween =
                orders.createAt.between(startDateTime, endDateTime);

        BooleanExpression statusIsReceiptOrWaiting =
                orders.productStatus.eq(ProductStatus.RECEIPT);

        BooleanExpression status = orders.orderStatus.eq(OrderStatus.valueOf(orderCondition.getOrderStatus()));

        return statusIsReceiptOrWaiting.and(status).and(createdBetween);
    }

    private static BooleanExpression getExpectStatusBuilder(OrderDto.ExpectCondition orderCondition) {
        String endAt = orderCondition.getEndAt();

        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        BooleanExpression shippingAt =
                orders.shippingAt.loe(endDateTime);

        BooleanExpression status = orders.orderStatus.notIn(OrderStatus.STOCK);

        return status.and(shippingAt);
    }

    private OrderSpecifier<?>[] createOrderSpecifiers(OrderDto.SortCondition sortCondition) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (sortCondition != null && StringUtils.hasText(sortCondition.getSortField())) {
            Order direction = "ASC".equalsIgnoreCase(sortCondition.getSort()) ? Order.ASC : Order.DESC;
            String field = sortCondition.getSortField();

            switch (field) {
                case "factory" -> orderSpecifiers.add(new OrderSpecifier<>(direction, orders.factoryName));
                case "store" -> orderSpecifiers.add(new OrderSpecifier<>(direction, orders.storeName));
                case "setType" -> orderSpecifiers.add(new OrderSpecifier<>(direction, orderProduct.setTypeName));
                case "color" -> orderSpecifiers.add(new OrderSpecifier<>(direction, orderProduct.colorName));

                default -> {
                    orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, orders.orderId));
                }
            }
        } else {
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, orders.orderId));
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

    private static BooleanExpression hasStatusHistory(BusinessPhase businessPhase) {
        if (businessPhase == null) {
            return null;
        }

        return orders.flowCode.in(
                JPAExpressions
                        .selectDistinct(statusHistory.flowCode)
                        .from(statusHistory)
                        .where(statusHistory.phase.eq(businessPhase))
        );
    }

}
