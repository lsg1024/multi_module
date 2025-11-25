package com.msa.order.local.sale.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.global.util.GoldUtils;
import com.msa.order.local.sale.entity.dto.QSaleDto_SaleDetailDto;
import com.msa.order.local.sale.entity.dto.SaleDto;
import com.msa.order.local.sale.entity.dto.SaleRow;
import com.msa.order.local.sale.sale_enum.SaleStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.*;
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
import java.util.Map;
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

        StringExpression saleTypeTitle = new CaseBuilder()
                .when(saleItem.itemStatus.eq(SaleStatus.SALE)).then("판매")
                .when(saleItem.itemStatus.eq(SaleStatus.RETURN)).then("반품")
                .when(saleItem.itemStatus.eq(SaleStatus.PAYMENT)).then("결제")
                .when(saleItem.itemStatus.eq(SaleStatus.DISCOUNT)).then("DC")
                .when(saleItem.itemStatus.eq(SaleStatus.PAYMENT_TO_BANK)).then("통장")
                .when(saleItem.itemStatus.eq(SaleStatus.WG)).then("WG")
                .otherwise("기타");

        NumberExpression<BigDecimal> baseGoldWeight = stock.product.goldWeight.coalesce(BigDecimal.ZERO);

        CaseBuilder.Cases<BigDecimal, NumberExpression<BigDecimal>> caseBuilder = new CaseBuilder()
                .when(stock.product.materialName.isNull())
                .then(BigDecimal.ZERO);

        for (Map.Entry<String, BigDecimal> entry : GoldUtils.getPurityMap().entrySet()) {
            String material = entry.getKey();
            BigDecimal purity = entry.getValue();

            caseBuilder = caseBuilder
                    .when(stock.product.materialName.equalsIgnoreCase(material))
                    .then(baseGoldWeight.multiply(purity));
        }

        NumberExpression<BigDecimal> pureGoldWeight = caseBuilder.otherwise(BigDecimal.ZERO);

        NumberExpression<BigDecimal> finalPureGoldWeight = Expressions.numberTemplate(
                BigDecimal.class,
                "ROUND({0}, {1})",
                pureGoldWeight,
                3
        );

        return query.select(Projections.constructor(
                    SaleRow.class,
                    sale.createDate,
                    saleItem.createdBy,
                    saleTypeTitle,
                    sale.accountId.stringValue(),
                    sale.accountName,
                    sale.saleCode.stringValue(),
                    saleItem.flowCode.stringValue(),
                    stock.product.productName,
                    stock.product.materialName,
                    stock.product.colorName,
                    stock.stockNote.coalesce("").concat("\n")
                            .concat(stock.stockMainStoneNote.coalesce("")).concat("\n")
                            .concat(stock.stockAssistanceStoneNote.coalesce("")),
                    stock.product.assistantStone,
                    stock.product.assistantStoneName,
                    stock.product.goldWeight.add(stock.product.stoneWeight).coalesce(BigDecimal.ZERO),
                    finalPureGoldWeight,
                    stock.product.productLaborCost.add(stock.product.productAddLaborCost),
                    sumMainLabor,
                    sumAsstLabor,
                    stock.stoneAddLaborCost,
                    sumMainQty,
                    sumAsstQty
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
                        saleItem.itemStatus,
                        sale.accountId,
                        sale.accountName,
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

        StringExpression saleTypeTitle = new CaseBuilder()
                .when(salePayment.saleStatus.eq(SaleStatus.RETURN)).then("반품")
                .when(salePayment.saleStatus.eq(SaleStatus.PAYMENT)).then("결제")
                .when(salePayment.saleStatus.eq(SaleStatus.DISCOUNT)).then("DC")
                .when(salePayment.saleStatus.eq(SaleStatus.PAYMENT_TO_BANK)).then("통장")
                .when(salePayment.saleStatus.eq(SaleStatus.WG)).then("WG")
                .otherwise("기타");

        return query
                .select(Projections.constructor(
                        SaleRow.class,
                        sale.createDate,
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
