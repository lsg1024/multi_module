package com.msa.order.local.sale.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.global.util.GoldUtils;
import com.msa.order.local.sale.entity.dto.QSaleDto_SaleDetailDto;
import com.msa.order.local.sale.entity.dto.QSaleItemResponse;
import com.msa.order.local.sale.entity.dto.SaleDto;
import com.msa.order.local.sale.entity.dto.SaleItemResponse;
import com.msa.order.local.sale.sale_enum.SaleStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.msa.order.local.order.entity.QOrderStone.orderStone;
import static com.msa.order.local.sale.entity.QSale.sale;
import static com.msa.order.local.sale.entity.QSaleItem.saleItem;
import static com.msa.order.local.sale.entity.QSalePayment.salePayment;
import static com.msa.order.local.stock.entity.QStock.stock;

@Slf4j
public class SaleRepositoryImpl implements CustomSaleRepository {

    private final JPAQueryFactory query;

    public SaleRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public CustomPage<SaleItemResponse> findSales(SaleDto.Condition condition, Pageable pageable) {

        List<SaleItemResponse> items = fetchItems(condition);
        List<SaleItemResponse> payments = fetchPayment(condition);

        List<SaleItemResponse> mergedSales = Stream.concat(items.stream(), payments.stream())
                .sorted(Comparator.comparing(SaleItemResponse::getCreateAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        long total = mergedSales.size();
        int from = (int) pageable.getOffset();
        int to = (int) Math.min(from + pageable.getPageSize(), total);
        List<SaleItemResponse> content = from >= to ? Collections.emptyList() : mergedSales.subList(from, to);

        return new CustomPage<>(content, pageable, total);
    }

    public List<SaleDto.SaleDetailDto> findSalePast(Long storeId, Long productId, String materialName) {
        return query.select(new QSaleDto_SaleDetailDto(
                    stock.flowCode,
                    sale.createDate.stringValue(),
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
                    stock.product.assistantStoneCreateAt.stringValue(),
                    sale.accountName
                ))
                .from(sale)
                .join(sale.items, saleItem)
                .join(saleItem.stock, stock)
                .where(
                        sale.accountId.eq(storeId),
                        stock.product.id.eq(productId),
                        stock.product.materialName.eq(materialName)
                )
                .orderBy(sale.createDate.desc())
                .limit(4)
                .fetch();
    }

    private List<SaleItemResponse> fetchItems(SaleDto.Condition condition) {

        BooleanBuilder searchBuilder = getSearchBuilder(condition.getInput());
        BooleanExpression createAtAndEndAt = getCreateAtAndEndAt(condition.getStartAt(), condition.getEndAt());

        BooleanBuilder materialBuilder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getMaterial())) {
            materialBuilder.and(stock.product.materialName.containsIgnoreCase(condition.getMaterial()));
        }

        Expression<Long> subMainQty = JPAExpressions
                .select(orderStone.stoneQuantity.sum().coalesce(0).castToNum(Long.class))
                .from(orderStone)
                .where(orderStone.stock.eq(stock)
                        .and(orderStone.includeStone.isTrue())
                        .and(orderStone.mainStone.isTrue()));

        Expression<Long> subAsstQty = JPAExpressions
                .select(orderStone.stoneQuantity.sum().coalesce(0).castToNum(Long.class))
                .from(orderStone)
                .where(orderStone.stock.eq(stock)
                        .and(orderStone.includeStone.isTrue())
                        .and(orderStone.mainStone.isFalse()));

        Expression<Long> subMainLabor = JPAExpressions
                .select(orderStone.stoneLaborCost.multiply(orderStone.stoneQuantity).sum().coalesce(0).castToNum(Long.class))
                .from(orderStone)
                .where(orderStone.stock.eq(stock)
                        .and(orderStone.includeStone.isTrue())
                        .and(orderStone.mainStone.isTrue()));

        Expression<Long> subAsstLabor = JPAExpressions
                .select(orderStone.stoneLaborCost.multiply(orderStone.stoneQuantity).sum().coalesce(0).castToNum(Long.class))
                .from(orderStone)
                .where(orderStone.stock.eq(stock)
                        .and(orderStone.includeStone.isTrue())
                        .and(orderStone.mainStone.isFalse()));

        StringExpression saleTypeTitle = new CaseBuilder()
                .when(saleItem.itemStatus.eq(SaleStatus.SALE)).then("판매")
                .when(saleItem.itemStatus.eq(SaleStatus.RETURN)).then("반품")
                .when(saleItem.itemStatus.eq(SaleStatus.PAYMENT)).then("결제")
                .when(saleItem.itemStatus.eq(SaleStatus.DISCOUNT)).then("DC")
                .when(saleItem.itemStatus.eq(SaleStatus.PAYMENT_TO_BANK)).then("통장")
                .when(saleItem.itemStatus.eq(SaleStatus.WG)).then("WG")
                .otherwise("기타");

        return query.select(new QSaleItemResponse(
                    sale.createDate.stringValue(),
                    saleItem.createdBy,
                    saleTypeTitle,
                    sale.accountId.stringValue(),
                    sale.accountName,
                    sale.saleCode.stringValue(),
                    saleItem.flowCode.stringValue(),
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
                    sale.accountHarry,
                    stock.product.productLaborCost,
                    stock.product.productAddLaborCost,
                    stock.product.assistantStoneCreateAt.stringValue(),
                    subMainLabor,
                    subAsstLabor,
                    stock.stoneAddLaborCost,
                    subMainQty,
                    subAsstQty
                ))
                .from(saleItem)
                .join(saleItem.sale, sale)
                .join(saleItem.stock, stock)
                .where(
                        searchBuilder,
                        createAtAndEndAt,
                        materialBuilder
                )
                .orderBy(sale.createDate.desc())
                .fetch();
    }

    private List<SaleItemResponse> fetchPayment(SaleDto.Condition condition) {
        BooleanBuilder searchBuilder = getSearchBuilder(condition.getInput());
        BooleanExpression createAtAndEndAt = getCreateAtAndEndAt(condition.getStartAt(), condition.getEndAt());
        BooleanBuilder materialBuilder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getMaterial())) {
            materialBuilder.and(salePayment.material.containsIgnoreCase(condition.getMaterial()));
        }

        NumberExpression<BigDecimal> baseGoldWeight = salePayment.goldWeight.coalesce(BigDecimal.ZERO);
        NumberExpression<BigDecimal> harryFactor = sale.accountHarry.coalesce(BigDecimal.ONE); // 해리가 없으면 1

        CaseBuilder.Cases<BigDecimal, NumberExpression<BigDecimal>> caseBuilder = new CaseBuilder()
                .when(salePayment.material.isNull())
                .then(BigDecimal.ZERO);

        for (Map.Entry<String, BigDecimal> entry : GoldUtils.getPurityMap().entrySet()) {
            String material = entry.getKey();
            BigDecimal purity = entry.getValue();

            caseBuilder = caseBuilder
                    .when(salePayment.material.equalsIgnoreCase(material))
                    .then(baseGoldWeight.multiply(purity));
        }

        NumberExpression<BigDecimal> pureGoldWeight = caseBuilder.otherwise(BigDecimal.ZERO)
                .multiply(harryFactor);

        NumberExpression<BigDecimal> finalPureGoldWeight = Expressions.numberTemplate(
                BigDecimal.class,
                "ROUND({0}, {1})",
                pureGoldWeight,
                3
        );

        StringExpression saleTypeTitle = new CaseBuilder()
                .when(salePayment.saleStatus.eq(SaleStatus.RETURN)).then("반품")
                .when(salePayment.saleStatus.eq(SaleStatus.PAYMENT)).then("결제")
                .when(salePayment.saleStatus.eq(SaleStatus.DISCOUNT)).then("DC")
                .when(salePayment.saleStatus.eq(SaleStatus.PAYMENT_TO_BANK)).then("통장")
                .when(salePayment.saleStatus.eq(SaleStatus.WG)).then("WG")
                .otherwise("기타");

        return query
                .select(new QSaleItemResponse(
                        sale.createDate.stringValue(),
                        salePayment.createdBy,
                        saleTypeTitle,
                        sale.accountId.stringValue(),
                        sale.accountName,
                        sale.saleCode.stringValue(),
                        salePayment.flowCode.stringValue(),
                        saleTypeTitle,
                        salePayment.material,
                        Expressions.nullExpression(String.class), // colorName
                        salePayment.paymentNote, // note
                        Expressions.nullExpression(String.class),
                        Expressions.nullExpression(String.class),
                        Expressions.nullExpression(Boolean.class),
                        Expressions.nullExpression(String.class),
                        finalPureGoldWeight,
                        Expressions.nullExpression(BigDecimal.class),
                        Expressions.nullExpression(BigDecimal.class),
                        salePayment.cashAmount.intValue(),
                        Expressions.nullExpression(Integer.class),
                        Expressions.nullExpression(String.class),
                        Expressions.nullExpression(Long.class),
                        Expressions.nullExpression(Long.class),
                        Expressions.nullExpression(Integer.class),
                        Expressions.nullExpression(Long.class),
                        Expressions.nullExpression(Long.class)
                ))
                .from(salePayment)
                .join(salePayment.sale, sale)
                .where(
                        searchBuilder,
                        createAtAndEndAt,
                        materialBuilder
                )
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
