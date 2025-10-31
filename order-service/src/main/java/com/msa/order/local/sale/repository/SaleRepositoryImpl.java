package com.msa.order.local.sale.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.sale.entity.dto.SaleDto;
import com.msa.order.local.sale.entity.dto.SaleRow;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static com.msa.order.local.order.entity.QOrderStone.orderStone;
import static com.msa.order.local.sale.entity.QSale.sale;
import static com.msa.order.local.sale.entity.QSaleItem.saleItem;
import static com.msa.order.local.sale.entity.QSalePayment.salePayment;
import static com.msa.order.local.stock.entity.QStock.stock;

public class SaleRepositoryImpl implements CustomSaleRepository {

    private final JPAQueryFactory query;

    public SaleRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public CustomPage<SaleRow> findSales(SaleDto.Condition condition, Pageable pageable) {

        List<SaleRow> items = fetchItems(condition);
        List<SaleRow> payments = fetchPayment(condition);

        List<SaleRow> mergedSales = Stream.concat(items.stream(), payments.stream())
                .sorted(Comparator.comparing(SaleRow::createAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        long total = mergedSales.size();
        int from = (int) pageable.getOffset();
        int to = (int) Math.min(from + pageable.getPageSize(), total);
        List<SaleRow> content = from >= to ? Collections.emptyList() : mergedSales.subList(from, to);

        return new CustomPage<>(content, pageable, total);
    }

    public List<SaleDto.SaleDetailDto> findSalePast(Long storeId, Long productId, String materialName) {
        return query.select(Projections.constructor(SaleDto.SaleDetailDto.class,
                        stock.flowCode,
                        sale.createDate,
                        stock.product.productName,
                        stock.product.materialName,
                        stock.product.colorName,
                        stock.stockMainStoneNote,
                        stock.stockAssistanceStoneNote,
                        stock.product.size,
                        stock.stockNote,
                        stock.product.goldWeight,
                        stock.product.stoneWeight,
                        Expressions.nullExpression(Integer.class), // mainStoneQuantity
                        Expressions.nullExpression(Integer.class), // assistanceStoneQuantity
                        stock.product.productLaborCost,
                        stock.product.productAddLaborCost,
                        stock.stoneMainLaborCost,
                        stock.stoneAssistanceLaborCost,
                        stock.stoneAddLaborCost,
                        stock.product.assistantStone,
                        stock.product.assistantStoneName,
                        stock.product.assistantStoneCreateAt,
                        sale.storeName
                ))
                .from(sale)
                .join(sale.items, saleItem)
                .join(saleItem.stock, stock)
                .where(
                        sale.storeId.eq(storeId),
                        stock.product.id.eq(productId),
                        stock.product.materialName.eq(materialName)
                )
                .orderBy(sale.createDate.desc())
                .limit(4)
                .fetch();
    }

    private List<SaleRow> fetchItems(SaleDto.Condition condition) {

        BooleanBuilder searchBuilder = getSearchBuilder(condition.getInput());
        BooleanExpression createAtAndEndAt = getCreateAtAndEndAt(condition.getStartAt(), condition.getEndAt());
        BooleanBuilder materialBuilder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getMaterial())) {
            materialBuilder.and(stock.product.materialName.containsIgnoreCase(condition.getMaterial()));
        }

        NumberExpression<Integer> mainQty = new CaseBuilder()
                .when(orderStone.includeStone.isTrue().and(orderStone.mainStone.isTrue()))
                .then(orderStone.stoneQuantity).otherwise(0);
        NumberExpression<Integer> asstQty = new CaseBuilder()
                .when(orderStone.includeStone.isTrue().and(orderStone.mainStone.isFalse()))
                .then(orderStone.stoneQuantity).otherwise(0);
        NumberExpression<Integer> mainLabor = new CaseBuilder()
                .when(orderStone.includeStone.isTrue().and(orderStone.mainStone.isTrue()))
                .then(orderStone.stoneLaborCost.multiply(orderStone.stoneQuantity)).otherwise(0);
        NumberExpression<Integer> asstLabor = new CaseBuilder()
                .when(orderStone.includeStone.isTrue().and(orderStone.mainStone.isFalse()))
                .then(orderStone.stoneLaborCost.multiply(orderStone.stoneQuantity)).otherwise(0);

        NumberExpression<Integer> sumMainQty   = mainQty.sum().coalesce(0);
        NumberExpression<Integer> sumAsstQty   = asstQty.sum().coalesce(0);
        NumberExpression<Integer> sumMainLabor = mainLabor.sum().coalesce(0);
        NumberExpression<Integer> sumAsstLabor = asstLabor.sum().coalesce(0);

        return query.select(Projections.constructor(
                    SaleRow.class,
                    sale.createDate,                          // createAt
                    saleItem.createdBy,
                    stock.orderStatus.stringValue(),         // saleType
                    stock.storeName, // storeName
                    sale.saleCode,                        // saleCode
                    saleItem.flowCode,                          // flowCode
                    stock.product.productName,                      // productName
                    stock.product.materialName,              // materialName
                    stock.product.colorName,                 // colorName
                    stock.stockNote.concat("\n").concat(stock.stockMainStoneNote).concat(" + ").concat(stock.stockAssistanceStoneNote), // note
                    stock.product.assistantStone, // assistantStone
                    stock.product.assistantStoneName, // assistantStoneName
                    stock.product.goldWeight.add(stock.product.stoneWeight).coalesce(BigDecimal.ZERO), // totalWeight
                    stock.product.goldWeight.coalesce(BigDecimal.ZERO), //goldWeight
                    stock.product.productLaborCost.add(stock.product.productAddLaborCost), // totalProductLaborCost
                    sumMainLabor,                         // mainStoneCost
                    sumAsstLabor,                         // assistanceStoneCost
                    stock.stoneAddLaborCost,              // stoneAddLaborCost
                    sumMainQty,                           // mainStoneQuantity
                    sumAsstQty                            // assistanceQuantity
                ))
                .from(saleItem)
                .join(saleItem.sale, sale)
                .join(saleItem.stock, stock)
                .leftJoin(orderStone).on(orderStone.stock.eq(stock))
                .where(
                        searchBuilder,
                        createAtAndEndAt,
                        materialBuilder
                )
                .groupBy(
                        sale.createDate,
                        saleItem.createdBy,
                        stock.orderStatus,
                        stock.storeName,
                        sale.saleCode,
                        saleItem.flowCode,
                        stock.product.productName,
                        stock.product.materialName,
                        stock.product.colorName,
                        stock.stockNote,
                        stock.stockMainStoneNote,
                        stock.stockAssistanceStoneNote,
                        stock.product.assistantStone,
                        stock.product.assistantStoneName,
                        stock.product.goldWeight,
                        stock.product.stoneWeight,
                        stock.product.productLaborCost,
                        stock.product.productAddLaborCost,
                        stock.stoneAddLaborCost
                )
                .orderBy(sale.createDate.desc())
                .fetch();
    }

    private List<SaleRow> fetchPayment(SaleDto.Condition condition) {
        BooleanBuilder searchBuilder = getSearchBuilder(condition.getInput());
        BooleanExpression createAtAndEndAt = getCreateAtAndEndAt(condition.getStartAt(), condition.getEndAt());
        BooleanBuilder materialBuilder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getMaterial())) {
            materialBuilder.and(salePayment.material.containsIgnoreCase(condition.getMaterial()));
        }

        return query
                .select(Projections.constructor(
                        SaleRow.class,
                        sale.createDate,
                        salePayment.createdBy,
                        salePayment.saleStatus.stringValue(),
                        salePayment.storeName,
                        salePayment.saleCode,
                        salePayment.flowCode,
                        salePayment.saleStatus.stringValue(),
                        salePayment.material,
                        Expressions.nullExpression(String.class), // colorName
                        salePayment.paymentNote, // note
                        Expressions.nullExpression(Boolean.class),
                        Expressions.nullExpression(String.class),
                        salePayment.goldWeight,
                        Expressions.nullExpression(BigDecimal.class),
                        salePayment.cashAmount.intValue(),
                        Expressions.nullExpression(Integer.class), // mainStoneLaborCost
                        Expressions.nullExpression(Integer.class), // assistanceStoneLaborCost
                        Expressions.nullExpression(Integer.class), // stoneAddLaborCost
                        Expressions.nullExpression(Integer.class), // mainStoneQuantity
                        Expressions.nullExpression(Integer.class)  // assistanceStoneQuantity
                ))
                .from(salePayment)
                .join(salePayment.sale, sale)
                .leftJoin(saleItem).on(saleItem.sale.eq(sale))
                .where(
                        searchBuilder,
                        createAtAndEndAt,
                        materialBuilder
                )
                .groupBy(salePayment.salePaymentId, sale.createDate)
                .orderBy(sale.createDate.desc())
                .fetch();
    }

    @NotNull
    private static BooleanBuilder getSearchBuilder(String searchInput) {
        BooleanBuilder searchBuilder = new BooleanBuilder();

        if (StringUtils.hasText(searchInput)) {
            searchBuilder.and(stock.storeName.containsIgnoreCase(searchInput));
        }

        return searchBuilder;
    }

    private static BooleanExpression getCreateAtAndEndAt(String startAt, String endAt) {
        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        return sale.createDate.between(start, end);
    }
}
