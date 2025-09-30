package com.msa.order.local.sale.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.sale.entity.dto.SaleDto;
import com.msa.order.local.sale.entity.dto.SaleRow;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    public CustomPage<SaleDto.Response> findSales(SaleDto.Condition condition, Pageable pageable) {

        List<SaleRow> items = fetchItems(condition.getDate());
        List<SaleRow> payments = fetchPayment(condition.getDate());

        List<SaleRow> mergedSales = Stream.concat(items.stream(), payments.stream())
                .sorted(Comparator.comparing(SaleRow::createAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        long total = mergedSales.size();
        int from = (int) pageable.getOffset();
        int to = (int) Math.min(from + pageable.getPageSize(), total);
        List<SaleRow> slice = from >= to ? Collections.emptyList() : mergedSales.subList(from, to);

        List<SaleDto.Response> content = slice.stream().map(SaleDto.Response::from).toList();

        return new CustomPage<>(content, pageable, total);
    }

    public List<SaleDto.SaleDetailDto> findSalePast(Long storeId, Long productId, String materialName) {
        return query.select(Projections.constructor(SaleDto.SaleDetailDto.class,
                        stock.flowCode,
                        sale.createAt,
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
                        stock.product.laborCost,
                        stock.product.addLaborCost,
                        stock.mainStoneLaborCost,
                        stock.assistanceStoneLaborCost,
                        stock.addStoneLaborCost,
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
                .orderBy(sale.createAt.desc())
                .limit(4)
                .fetch();
    }

    private List<SaleRow> fetchItems(String date) {

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
        NumberExpression<Integer> stonePurchase = new CaseBuilder()
                .when(orderStone.includeStone.isTrue())
                .then(orderStone.stonePurchaseCost.multiply(orderStone.stoneQuantity)).otherwise(0);

        NumberExpression<Integer> sumMainQty   = mainQty.sum().coalesce(0);
        NumberExpression<Integer> sumAsstQty   = asstQty.sum().coalesce(0);
        NumberExpression<Integer> sumMainLabor = mainLabor.sum().coalesce(0);
        NumberExpression<Integer> sumAsstLabor = asstLabor.sum().coalesce(0);
        NumberExpression<Integer> sumPurchase  = stonePurchase.sum().coalesce(0);

        NumberExpression<Integer> totalPurchase =
                stock.product.productPurchaseCost.add(sumPurchase);

        BooleanExpression base =
                sale.saleDate.eq(LocalDate.parse(date));

        return query.select(Projections.constructor(
                    SaleRow.class,
                    saleItem.createAt,                          // createAt
                    Expressions.constant("SALE"),         // saleType
                    saleItem.createdBy, // name
                    sale.saleCode,                        // saleCode
                    saleItem.flowCode,                          // flowCode
                    stock.product.productName,                      // productName
                    stock.product.materialName,              // materialName
                    stock.product.colorName,                 // colorName
                    stock.stockNote,                         // note
                    sumMainQty,                           // mainStoneQuantity
                    sumAsstQty,                           // assistanceQuantity
                    stock.product.goldWeight, // totalGoldWeight
                    stock.product.stoneWeight,
                    stock.product.laborCost,                 // mainProductCost
                    stock.product.addLaborCost,              // addProductCost
                    sumMainLabor,                         // mainStoneCost
                    sumAsstLabor,                         // assistanceStoneCost
                    totalPurchase                          // totalPurchaseCost
                ))
                .from(saleItem)
                .join(saleItem.sale, sale)
                .join(saleItem.stock, stock)
                .leftJoin(orderStone).on(orderStone.stock.eq(stock))
                .where(base)
                .groupBy(
                        saleItem.createAt, sale.saleCode, saleItem.flowCode,
                        stock.product.productName, stock.product.materialName, stock.product.colorName,
                        stock.stockNote, stock.product.laborCost, stock.product.addLaborCost, stock.product.productPurchaseCost
                )
                .fetch();
    }

    private List<SaleRow> fetchPayment(String date) {
        BooleanExpression base = sale.saleDate.eq(LocalDate.parse(date));
        return query
                .select(Projections.constructor(
                        SaleRow.class,
                        salePayment.createdAt,
                        salePayment.saleStatus.stringValue(),
                        salePayment.createdBy,
                        salePayment.saleCode,
                        salePayment.flowCode,
                        salePayment.saleStatus.stringValue(),
                        salePayment.material,
                        Expressions.nullExpression(String.class), // colorName
                        salePayment.paymentNote, // note
                        Expressions.nullExpression(Integer.class),
                        Expressions.nullExpression(Integer.class),
                        salePayment.goldWeight,
                        Expressions.nullExpression(BigDecimal.class),
                        salePayment.cashAmount.intValue(),
                        Expressions.nullExpression(Integer.class),
                        Expressions.nullExpression(Integer.class),
                        Expressions.nullExpression(Integer.class),
                        Expressions.nullExpression(Integer.class)
                ))
                .from(salePayment)
                .join(salePayment.sale, sale)
                .leftJoin(saleItem).on(saleItem.sale.eq(sale))
                .leftJoin(saleItem.stock, stock)
                .where(base)
                .groupBy(salePayment.salePaymentId)
                .fetch();
    }
}
