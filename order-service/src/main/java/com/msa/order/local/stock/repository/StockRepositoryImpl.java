package com.msa.order.local.stock.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.stock.dto.QStockDto_Response;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.order.entity.QOrderStone;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.msa.order.local.stock.entity.QStock.stock;

@Repository
public class StockRepositoryImpl implements CustomStockRepository {

    private final JPAQueryFactory query;

    public StockRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public CustomPage<StockDto.Response> findByStockProducts(String input, String orderType, StockDto.StockCondition condition, Pageable pageable) {

        BooleanBuilder searchBuilder = getSearchBuilder(input);
        BooleanExpression stockStatusBuilder = getStockStatusBuilder(condition);

        BooleanExpression orderTypeCondition = null;
        if (orderType != null && !orderType.isBlank()) {
            orderTypeCondition = stock.orderStatus.eq(OrderStatus.valueOf(orderType));
        }

        List<StockDto.Response> content = query
                .select(new QStockDto_Response(
                        stock.flowCode.stringValue(),
                        stock.stockCreateAt.stringValue(),
                        stock.storeName,
                        stock.product.size,
                        stock.stockNote,
                        stock.product.materialName,
                        stock.product.classificationName,
                        stock.product.colorName,
                        stock.product.laborCost,    // 11 기본
                        stock.product.addLaborCost,  // 12 추가
                        stock.mainStoneLaborCost,
                        stock.assistanceStoneLaborCost,
                        stock.stockMainStoneNote,
                        stock.stockAssistanceStoneNote,
                        Expressions.cases()
                                .when(QOrderStone.orderStone.includeStone.isTrue().and(QOrderStone.orderStone.mainStone.isTrue()))
                                .then(QOrderStone.orderStone.stoneQuantity)
                                .otherwise(0)
                                .sum()
                                .coalesce(0)
                                .intValue(),
                        Expressions.cases()
                                .when(QOrderStone.orderStone.includeStone.isTrue().and(QOrderStone.orderStone.mainStone.isFalse()))
                                .then(QOrderStone.orderStone.stoneQuantity)
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
                .leftJoin(QOrderStone.orderStone).on(QOrderStone.orderStone.stock.eq(stock))
                .where(searchBuilder, stockStatusBuilder, orderTypeCondition)
                .orderBy(stock.stockCreateAt.asc())
                .groupBy(stock.stockId)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        JPAQuery<Long> countQuery = query
                .select(stock.stockId.count())
                .from(stock)
                .where(
                        searchBuilder,
                        stockStatusBuilder,
                        orderTypeCondition
                );

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    @NotNull
    private static BooleanBuilder getSearchBuilder(String searchInput) {
        BooleanBuilder booleanInput = new BooleanBuilder();

        if (StringUtils.hasText(searchInput)) {
            booleanInput.and(stock.product.productName.containsIgnoreCase(searchInput));
            booleanInput.or(stock.storeName.containsIgnoreCase(searchInput));
            booleanInput.or(stock.factoryName.containsIgnoreCase(searchInput));
        }

        return booleanInput;
    }

    private static BooleanExpression getStockStatusBuilder(StockDto.StockCondition condition) {
        String startAt = condition.getStartAt();
        String endAt = condition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneOffset.of("+09:00"));
        OffsetDateTime endDateTime = end.atOffset(ZoneOffset.of("+09:00"));

        return stock.stockCreateAt.between(startDateTime, endDateTime);
    }
}
