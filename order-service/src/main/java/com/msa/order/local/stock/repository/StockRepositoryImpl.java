package com.msa.order.local.stock.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.entity.QStatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.SourceType;
import com.msa.order.local.stock.dto.QStockDto_Response;
import com.msa.order.local.stock.dto.StockDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.msa.order.global.util.DateConversionUtil.LocalDateToOffsetDateTime;
import static com.msa.order.local.order.entity.QOrderStone.orderStone;
import static com.msa.order.local.order.entity.QStatusHistory.statusHistory;
import static com.msa.order.local.stock.entity.QStock.stock;

@Repository
public class StockRepositoryImpl implements CustomStockRepository {

    private final JPAQueryFactory query;

    public StockRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public CustomPage<StockDto.Response> findByStockProducts(OrderDto.InputCondition inputCondition, StockDto.StockCondition condition, Pageable pageable) {

        BooleanBuilder searchBuilder = getSearchBuilder(inputCondition.getSearchInput());
        BooleanExpression stockStatusBuilder = getStockCreateAtAndEndAt(condition);
        BooleanExpression orderTypeCondition = getOrderTypeBuilder(condition);

        BooleanBuilder optionBuilder = getOptionBuilder(condition.getOptionCondition());
        OrderSpecifier<?>[] stockSpecifiers = createStockSpecifiers(condition.getSortCondition());
        QStatusHistory subHistory = new QStatusHistory("subHistory");

        List<StockDto.Response> content = query
                .select(new QStockDto_Response(
                        stock.flowCode.stringValue(),
                        stock.createDate.stringValue(),
                        stock.lastModifiedDate.stringValue(),
                        statusHistory.sourceType.stringValue(),
                        stock.orderStatus.stringValue(),
                        stock.storeName,
                        stock.product.id.stringValue(),
                        stock.product.size,
                        stock.stockNote,
                        stock.product.materialName,
                        stock.product.classificationName,
                        stock.product.colorName,
                        stock.product.productLaborCost,
                        stock.product.productAddLaborCost,
                        stock.product.assistantStoneName,
                        stock.product.assistantStone,
                        stock.stoneMainLaborCost,
                        stock.stoneAssistanceLaborCost,
                        stock.stoneAddLaborCost,
                        stock.stockMainStoneNote,
                        stock.stockAssistanceStoneNote,
                        Expressions.cases()
                                .when(orderStone.includeStone.isTrue().and(orderStone.mainStone.isTrue()))
                                .then(orderStone.stoneQuantity)
                                .otherwise(0)
                                .sum()
                                .coalesce(0)
                                .intValue(),
                        Expressions.cases()
                                .when(orderStone.includeStone.isTrue().and(orderStone.mainStone.isFalse()))
                                .then(orderStone.stoneQuantity)
                                .otherwise(0)
                                .sum()
                                .coalesce(0)
                                .intValue(),
                        stock.product.stoneWeight.stringValue(),
                        stock.product.goldWeight.stringValue(),
                        stock.product.productPurchaseCost,
                        stock.totalStonePurchaseCost
                ))
                .from(stock)
                .leftJoin(orderStone)
                    .on(orderStone.stock.eq(stock))
                .leftJoin(statusHistory)
                    .on(statusHistory.flowCode.eq(stock.flowCode),
                            statusHistory.createAt.eq(
                                    JPAExpressions
                                            .select(subHistory.createAt.max())
                                            .from(subHistory)
                                            .where(subHistory.flowCode.eq(stock.flowCode))
                            ))
                .where(searchBuilder, stockStatusBuilder, orderTypeCondition, optionBuilder)
                .orderBy(stockSpecifiers)
                .groupBy(stock.stockId, statusHistory.id)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        for (StockDto.Response response : content) {
            String originStatus = SourceType.valueOf(response.getOriginStatus()).getDisplayName();
            String currentStatus = BusinessPhase.valueOf(response.getCurrentStatus()).getDisplayName();
            response.updateStatus(originStatus, currentStatus);
        }

        JPAQuery<Long> countQuery = query
                .select(stock.stockId.count())
                .from(stock)
                .where(
                        searchBuilder,
                        stockStatusBuilder,
                        orderTypeCondition,
                        optionBuilder
                );

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @Override
    public CustomPage<StockDto.Response> findStocksByHistoricalPhase(OrderDto.InputCondition inputCondition, StockDto.HistoryCondition condition, Pageable pageable) {
        QStatusHistory history = QStatusHistory.statusHistory;

        JPQLQuery<Long> subQuery = JPAExpressions
                .select(history.flowCode)
                .from(history)
                .where(
                        history.phase.eq(condition.getPhase()),
                        history.createAt.between(
                                LocalDateToOffsetDateTime(condition.getStartAt() + " 00:00:00"),
                                LocalDateToOffsetDateTime(condition.getEndAt() + " 23:59:59")
                        )
                );

        BooleanBuilder searchBuilder = getSearchBuilder(inputCondition.getSearchInput());
        BooleanBuilder optionBuilder = getOptionBuilder(condition.getOptionCondition());
        OrderSpecifier<?>[] stockSpecifiers = createStockSpecifiers(condition.getSortCondition());
        QStatusHistory subHistory = new QStatusHistory("subHistory");

        List<StockDto.Response> content = query
                .select(new QStockDto_Response(
                        stock.flowCode.stringValue(),
                        stock.createDate.stringValue(),
                        stock.lastModifiedDate.stringValue(),
                        statusHistory.sourceType.stringValue(),
                        stock.orderStatus.stringValue(),
                        stock.storeName,
                        stock.product.id.stringValue(),
                        stock.product.size,
                        stock.stockNote,
                        stock.product.materialName,
                        stock.product.classificationName,
                        stock.product.colorName,
                        stock.product.productLaborCost,
                        stock.product.productAddLaborCost,
                        stock.product.assistantStoneName,
                        stock.product.assistantStone,
                        stock.stoneMainLaborCost,
                        stock.stoneAssistanceLaborCost,
                        stock.stoneAddLaborCost,
                        stock.stockMainStoneNote,
                        stock.stockAssistanceStoneNote,
                        Expressions.cases()
                                .when(orderStone.includeStone.isTrue().and(orderStone.mainStone.isTrue()))
                                .then(orderStone.stoneQuantity)
                                .otherwise(0)
                                .sum()
                                .coalesce(0)
                                .intValue(),
                        Expressions.cases()
                                .when(orderStone.includeStone.isTrue().and(orderStone.mainStone.isFalse()))
                                .then(orderStone.stoneQuantity)
                                .otherwise(0)
                                .sum()
                                .coalesce(0)
                                .intValue(),
                        stock.product.stoneWeight.stringValue(),
                        stock.product.goldWeight.stringValue(),
                        stock.product.productPurchaseCost,
                        stock.totalStonePurchaseCost
                ))
                .from(stock)
                .leftJoin(orderStone).on(orderStone.stock.eq(stock))
                .leftJoin(statusHistory)
                .on(statusHistory.flowCode.eq(stock.flowCode),
                        statusHistory.createAt.eq(
                                JPAExpressions
                                        .select(subHistory.createAt.max())
                                        .from(subHistory)
                                        .where(subHistory.flowCode.eq(stock.flowCode))
                        ))
                .where(
                        stock.flowCode.in(subQuery),
                        searchBuilder,
                        optionBuilder,
                        stock.orderStatus.in(OrderStatus.RENTAL, OrderStatus.RETURN)
                )
                .orderBy(stockSpecifiers)
                .groupBy(stock.stockId, statusHistory.id)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(stock.stockId.count())
                .from(stock)
                .where(
                        stock.flowCode.in(subQuery),
                        searchBuilder,
                        optionBuilder,
                        stock.orderStatus.in(OrderStatus.RENTAL, OrderStatus.RETURN)
                );

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @Override
    public List<String> findByFilterFactories(StockDto.StockCondition condition) {

        BooleanExpression stockCreateAtAndEndAt = getStockCreateAtAndEndAt(condition);
        BooleanExpression orderTypeBuilder = getOrderTypeBuilder(condition);

        return query
                .selectDistinct(stock.factoryName)
                .from(stock)
                .where(
                        stockCreateAtAndEndAt,
                        orderTypeBuilder)
                .fetch();
    }

    @Override
    public List<String> findByFilterStores(StockDto.StockCondition condition) {
        BooleanExpression stockCreateAtAndEndAt = getStockCreateAtAndEndAt(condition);
        BooleanExpression orderTypeBuilder = getOrderTypeBuilder(condition);

        return query
                .selectDistinct(stock.storeName)
                .from(stock)
                .where(
                        stockCreateAtAndEndAt,
                        orderTypeBuilder)
                .fetch();
    }

    @Override
    public List<String> findByFilterSetType(StockDto.StockCondition condition) {
        BooleanExpression stockCreateAtAndEndAt = getStockCreateAtAndEndAt(condition);
        BooleanExpression orderTypeBuilder = getOrderTypeBuilder(condition);

        return query
                .selectDistinct(stock.product.setTypeName)
                .from(stock)
                .where(
                        stockCreateAtAndEndAt,
                        orderTypeBuilder)
                .fetch();
    }

    @Override
    public List<String> findByFilterColor(StockDto.StockCondition condition) {
        BooleanExpression stockCreateAtAndEndAt = getStockCreateAtAndEndAt(condition);
        BooleanExpression orderTypeBuilder = getOrderTypeBuilder(condition);
        return query
                .selectDistinct(stock.product.colorName)
                .from(stock)
                .where(
                        stockCreateAtAndEndAt,
                        orderTypeBuilder)
                .fetch();
    }

    private static BooleanExpression getStockCreateAtAndEndAt(StockDto.StockCondition condition) {
        String startAt = condition.getStartAt();
        String endAt = condition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        return stock.createDate.between(start, end);
    }

    @Nullable
    private BooleanExpression getOrderTypeBuilder(StockDto.StockCondition condition) {
        String orderStatus = condition.getOrderStatus();

        if ("ALL".equalsIgnoreCase(orderStatus)) {
            return stock.orderStatus.in(
                    OrderStatus.STOCK,
                    OrderStatus.NORMAL,
                    OrderStatus.RENTAL,
                    OrderStatus.FIX,
                    OrderStatus.RETURN,
                    OrderStatus.SALE
                    );
        }

        if (StringUtils.hasText(orderStatus)) {
            return stock.orderStatus.eq(OrderStatus.valueOf(orderStatus));
        }

        return stock.orderStatus.in(
                OrderStatus.STOCK,
                OrderStatus.NORMAL,
                OrderStatus.RENTAL,
                OrderStatus.FIX
        );
    }

    @NotNull
    private static BooleanBuilder getSearchBuilder(String searchInput) {
        BooleanBuilder searchBuilder = new BooleanBuilder();

        if (StringUtils.hasText(searchInput)) {
            searchBuilder.and(
                    stock.product.productName.containsIgnoreCase(searchInput)
                            .or(stock.storeName.containsIgnoreCase(searchInput))
                            .or(stock.factoryName.containsIgnoreCase(searchInput))
            );
        }
        return searchBuilder;
    }

    private OrderSpecifier<?>[] createStockSpecifiers(OrderDto.SortCondition sortCondition) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (sortCondition != null && StringUtils.hasText(sortCondition.getSortField())) {
            Order direction = "ASC".equalsIgnoreCase(sortCondition.getSort()) ? Order.ASC : Order.DESC;
            String field = sortCondition.getSortField();

            switch (field) {
                case "factory" -> orderSpecifiers.add(new OrderSpecifier<>(direction, stock.factoryName));
                case "store" -> orderSpecifiers.add(new OrderSpecifier<>(direction, stock.storeName));
                case "setType" -> orderSpecifiers.add(new OrderSpecifier<>(direction, stock.product.setTypeName));
                case "color" -> orderSpecifiers.add(new OrderSpecifier<>(direction, stock.product.colorName));

                default -> {
                    orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, stock.flowCode));
                }
            }
        } else {
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, stock.flowCode));
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

    private BooleanBuilder getOptionBuilder(OrderDto.OptionCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition != null) {
            if (StringUtils.hasText(condition.getFactoryName())) {
                builder.and(stock.factoryName.eq(condition.getFactoryName()));
            }
            if (StringUtils.hasText(condition.getStoreName())) {
                builder.and(stock.storeName.eq(condition.getStoreName()));
            }
            if (StringUtils.hasText(condition.getSetTypeName())) {
                builder.and(stock.product.setTypeName.eq(condition.getSetTypeName()));
            }
            if (StringUtils.hasText(condition.getColorName())) {
                builder.and(stock.product.colorName.eq(condition.getColorName()));
            }
        }
        return builder;
    }
}
