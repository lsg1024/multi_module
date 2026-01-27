package com.msa.order.local.dashboard.repository;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.dashboard.dto.DashboardDto;
import com.msa.order.local.dashboard.dto.QDashboardDto_MaterialStockSummary;
import com.msa.order.local.dashboard.dto.QDashboardDto_SaleModelTop;
import com.msa.order.local.dashboard.dto.QDashboardDto_StockDetail;
import com.msa.order.local.dashboard.dto.QDashboardDto_StockModelTop;
import com.msa.order.local.dashboard.dto.QDashboardDto_StoreLaborCostTop;
import com.msa.order.local.dashboard.dto.QDashboardDto_MonthlySalesSummary;
import com.msa.order.local.dashboard.dto.QDashboardDto_ReceivableSummary;
import com.msa.order.local.dashboard.dto.QDashboardDto_RentalSummary;
import com.msa.order.local.dashboard.dto.QDashboardDto_RentalDetail;
import com.msa.order.local.dashboard.dto.QDashboardDto_FactoryUnpaidSummary;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.msa.order.local.order.entity.QOrderStone.orderStone;
import static com.msa.order.local.sale.entity.QSale.sale;
import static com.msa.order.local.sale.entity.QSaleItem.saleItem;
import static com.msa.order.local.sale.entity.QSalePayment.salePayment;
import static com.msa.order.local.stock.entity.QStock.stock;

@Repository
public class DashboardRepositoryImpl implements DashboardRepository {

    private final JPAQueryFactory query;

    public DashboardRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<DashboardDto.MaterialStockSummary> findMaterialStockSummary(int limit) {
        return query
                .select(new QDashboardDto_MaterialStockSummary(
                        stock.product.materialName,
                        stock.product.goldWeight.sum(),
                        stock.count()
                ))
                .from(stock)
                .where(
                        stock.stockDeleted.isFalse(),
                        stock.orderStatus.in(OrderStatus.STOCK, OrderStatus.NORMAL, OrderStatus.WAIT)
                )
                .groupBy(stock.product.materialName)
                .orderBy(stock.count().desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<DashboardDto.StockModelTop> findStockModelTop(int limit) {
        return query
                .select(new QDashboardDto_StockModelTop(
                        stock.product.productName,
                        stock.count()
                ))
                .from(stock)
                .where(
                        stock.stockDeleted.isFalse(),
                        stock.orderStatus.in(OrderStatus.STOCK, OrderStatus.NORMAL, OrderStatus.WAIT)
                )
                .groupBy(stock.product.productName)
                .orderBy(stock.count().desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public CustomPage<DashboardDto.StockDetail> findAllStockDetails(Pageable pageable) {
        List<DashboardDto.StockDetail> content = query
                .select(new QDashboardDto_StockDetail(
                        stock.flowCode.stringValue(),
                        stock.createDate.stringValue(),
                        stock.product.productName,
                        stock.product.materialName,
                        stock.product.colorName,
                        stock.product.size,
                        stock.product.goldWeight,
                        stock.product.stoneWeight,
                        stock.factoryName,
                        stock.storeName,
                        stock.orderStatus.stringValue()
                ))
                .from(stock)
                .where(
                        stock.stockDeleted.isFalse(),
                        stock.orderStatus.in(OrderStatus.STOCK, OrderStatus.NORMAL, OrderStatus.WAIT)
                )
                .orderBy(stock.createDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = query
                .select(stock.count())
                .from(stock)
                .where(
                        stock.stockDeleted.isFalse(),
                        stock.orderStatus.in(OrderStatus.STOCK, OrderStatus.NORMAL, OrderStatus.WAIT)
                )
                .fetchOne();

        return new CustomPage<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public CustomPage<DashboardDto.StockDetail> findStockDetailsWithSearch(DashboardDto.StockSearchCondition condition, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(stock.stockDeleted.isFalse());
        builder.and(stock.orderStatus.in(OrderStatus.STOCK, OrderStatus.NORMAL, OrderStatus.WAIT));

        if (StringUtils.isNotBlank(condition.getProductName())) {
            builder.and(stock.product.productName.containsIgnoreCase(condition.getProductName()));
        }
        if (StringUtils.isNotBlank(condition.getMaterialName())) {
            builder.and(stock.product.materialName.eq(condition.getMaterialName()));
        }
        if (StringUtils.isNotBlank(condition.getColorName())) {
            builder.and(stock.product.colorName.eq(condition.getColorName()));
        }
        if (StringUtils.isNotBlank(condition.getStoreName())) {
            builder.and(stock.storeName.eq(condition.getStoreName()));
        }

        List<DashboardDto.StockDetail> content = query
                .select(new QDashboardDto_StockDetail(
                        stock.flowCode.stringValue(),
                        stock.createDate.stringValue(),
                        stock.product.productName,
                        stock.product.materialName,
                        stock.product.colorName,
                        stock.product.size,
                        stock.product.goldWeight,
                        stock.product.stoneWeight,
                        stock.factoryName,
                        stock.storeName,
                        stock.orderStatus.stringValue()
                ))
                .from(stock)
                .where(builder)
                .orderBy(stock.createDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = query
                .select(stock.count())
                .from(stock)
                .where(builder)
                .fetchOne();

        return new CustomPage<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public DashboardDto.StockFilterOption findStockFilterOptions() {
        BooleanExpression baseCondition = stock.stockDeleted.isFalse()
                .and(stock.orderStatus.in(OrderStatus.STOCK, OrderStatus.NORMAL, OrderStatus.WAIT));

        List<String> materials = query
                .select(stock.product.materialName)
                .distinct()
                .from(stock)
                .where(baseCondition, stock.product.materialName.isNotNull())
                .orderBy(stock.product.materialName.asc())
                .fetch();

        List<String> colors = query
                .select(stock.product.colorName)
                .distinct()
                .from(stock)
                .where(baseCondition, stock.product.colorName.isNotNull())
                .orderBy(stock.product.colorName.asc())
                .fetch();

        List<String> stores = query
                .select(stock.storeName)
                .distinct()
                .from(stock)
                .where(baseCondition, stock.storeName.isNotNull())
                .orderBy(stock.storeName.asc())
                .fetch();

        return DashboardDto.StockFilterOption.builder()
                .materials(materials)
                .colors(colors)
                .stores(stores)
                .build();
    }

    @Override
    public List<DashboardDto.SaleModelTop> findMonthlySaleModelTop(int limit) {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);

        return query
                .select(new QDashboardDto_SaleModelTop(
                        stock.product.productName,
                        saleItem.count()
                ))
                .from(saleItem)
                .join(saleItem.stock, stock)
                .join(saleItem.sale, sale)
                .where(
                        saleItem.itemStatus.eq(SaleStatus.SALE),
                        sale.createDate.between(startOfMonth, endOfMonth)
                )
                .groupBy(stock.product.productName)
                .orderBy(saleItem.count().desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<DashboardDto.StoreLaborCostTop> findMonthlyStoreLaborCostTop(int limit) {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);

        NumberExpression<Long> totalLaborCost = Expressions.asNumber(0L)
                .add(stock.product.productLaborCost.coalesce(0))
                .add(stock.product.productAddLaborCost.coalesce(0))
                .add(stock.stoneMainLaborCost.coalesce(0))
                .add(stock.stoneAssistanceLaborCost.coalesce(0))
                .add(stock.stoneAddLaborCost.coalesce(0));

        return query
                .select(new QDashboardDto_StoreLaborCostTop(
                        sale.accountName,
                        totalLaborCost.sum()
                ))
                .from(saleItem)
                .join(saleItem.stock, stock)
                .join(saleItem.sale, sale)
                .where(
                        saleItem.itemStatus.eq(SaleStatus.SALE),
                        sale.createDate.between(startOfMonth, endOfMonth)
                )
                .groupBy(sale.accountId, sale.accountName)
                .orderBy(totalLaborCost.sum().desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public DashboardDto.MonthlySalesSummary findMonthlySalesSummary() {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);

        NumberExpression<Long> laborCost = Expressions.asNumber(0L)
                .add(stock.product.productLaborCost.coalesce(0))
                .add(stock.product.productAddLaborCost.coalesce(0))
                .add(stock.stoneMainLaborCost.coalesce(0))
                .add(stock.stoneAssistanceLaborCost.coalesce(0))
                .add(stock.stoneAddLaborCost.coalesce(0));

        NumberExpression<Long> purchaseCost = Expressions.asNumber(0L)
                .add(stock.product.productPurchaseCost.coalesce(0))
                .add(stock.totalStonePurchaseCost.coalesce(0));

        DashboardDto.MonthlySalesSummary result = query
                .select(new QDashboardDto_MonthlySalesSummary(
                        stock.product.goldWeight.sum(),
                        laborCost.sum(),
                        laborCost.sum().subtract(purchaseCost.sum())
                ))
                .from(saleItem)
                .join(saleItem.stock, stock)
                .join(saleItem.sale, sale)
                .where(
                        saleItem.itemStatus.eq(SaleStatus.SALE),
                        sale.createDate.between(startOfMonth, endOfMonth)
                )
                .fetchOne();

        return result != null ? result : DashboardDto.MonthlySalesSummary.builder().build();
    }

    @Override
    public List<DashboardDto.StoreTradeStatistics> findStoreTradeStatistics(DashboardDto.StoreStatisticsSearchCondition condition) {
        // 날짜 범위 설정 (조건이 없으면 당월)
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if (StringUtils.isNotBlank(condition.getStart()) && StringUtils.isNotBlank(condition.getEnd())) {
            startDateTime = LocalDate.parse(condition.getStart()).atStartOfDay();
            endDateTime = LocalDate.parse(condition.getEnd()).atTime(23, 59, 59);
        } else {
            LocalDate now = LocalDate.now();
            startDateTime = now.withDayOfMonth(1).atStartOfDay();
            endDateTime = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);
        }

        NumberExpression<Long> laborCost = Expressions.asNumber(0L)
                .add(stock.product.productLaborCost.coalesce(0))
                .add(stock.product.productAddLaborCost.coalesce(0))
                .add(stock.stoneMainLaborCost.coalesce(0))
                .add(stock.stoneAssistanceLaborCost.coalesce(0))
                .add(stock.stoneAddLaborCost.coalesce(0));

        NumberExpression<Long> purchaseCost = Expressions.asNumber(0L)
                .add(stock.product.productPurchaseCost.coalesce(0))
                .add(stock.totalStonePurchaseCost.coalesce(0));

        // 공통 조건 빌더
        BooleanBuilder saleCondition = new BooleanBuilder();
        saleCondition.and(sale.createDate.between(startDateTime, endDateTime));

        // 거래처명 검색
        if (StringUtils.isNotBlank(condition.getStoreName())) {
            saleCondition.and(sale.accountName.containsIgnoreCase(condition.getStoreName()));
        }
        // 매장구분 (등급)
        if (StringUtils.isNotBlank(condition.getStoreGrade())) {
            saleCondition.and(sale.accountGrade.eq(condition.getStoreGrade()));
        }
        // 재질구분
        if (StringUtils.isNotBlank(condition.getMaterialName())) {
            saleCondition.and(stock.product.materialName.eq(condition.getMaterialName()));
        }
        // 분류구분
        if (StringUtils.isNotBlank(condition.getClassificationName())) {
            saleCondition.and(stock.product.classificationName.eq(condition.getClassificationName()));
        }
        // 매입처구분
        if (StringUtils.isNotBlank(condition.getFactoryName())) {
            saleCondition.and(stock.factoryName.eq(condition.getFactoryName()));
        }
        // 관리자구분
        if (StringUtils.isNotBlank(condition.getCreatedBy())) {
            saleCondition.and(sale.createdBy.eq(condition.getCreatedBy()));
        }

        // 거래처별로 판매/반품/DC 등을 집계하는 복잡한 쿼리
        // 각 상태별로 별도 쿼리 후 Java에서 조합
        List<Tuple> saleResults = query
                .select(
                        sale.accountId,
                        sale.accountName,
                        saleItem.itemStatus,
                        laborCost.sum(),
                        stock.product.goldWeight.sum(),
                        saleItem.count(),
                        purchaseCost.sum()
                )
                .from(saleItem)
                .join(saleItem.stock, stock)
                .join(saleItem.sale, sale)
                .where(saleCondition)
                .groupBy(sale.accountId, sale.accountName, saleItem.itemStatus)
                .fetch();

        // 결제 정보 조회 조건 빌더
        BooleanBuilder paymentCondition = new BooleanBuilder();
        paymentCondition.and(sale.createDate.between(startDateTime, endDateTime));
        paymentCondition.and(salePayment.saleStatus.eq(SaleStatus.PAYMENT));
        if (StringUtils.isNotBlank(condition.getStoreName())) {
            paymentCondition.and(sale.accountName.containsIgnoreCase(condition.getStoreName()));
        }
        if (StringUtils.isNotBlank(condition.getStoreGrade())) {
            paymentCondition.and(sale.accountGrade.eq(condition.getStoreGrade()));
        }

        List<Tuple> paymentResults = query
                .select(
                        sale.accountId,
                        salePayment.cashAmount.sum(),
                        salePayment.pureGoldWeight.sum()
                )
                .from(salePayment)
                .join(salePayment.sale, sale)
                .where(paymentCondition)
                .groupBy(sale.accountId)
                .fetch();

        // 메인스톤/보조스톤 개수 조회 조건 빌더
        BooleanBuilder stoneCondition = new BooleanBuilder();
        stoneCondition.and(sale.createDate.between(startDateTime, endDateTime));
        stoneCondition.and(saleItem.itemStatus.eq(SaleStatus.SALE));
        if (StringUtils.isNotBlank(condition.getStoreName())) {
            stoneCondition.and(sale.accountName.containsIgnoreCase(condition.getStoreName()));
        }
        if (StringUtils.isNotBlank(condition.getStoreGrade())) {
            stoneCondition.and(sale.accountGrade.eq(condition.getStoreGrade()));
        }
        if (StringUtils.isNotBlank(condition.getMaterialName())) {
            stoneCondition.and(stock.product.materialName.eq(condition.getMaterialName()));
        }
        if (StringUtils.isNotBlank(condition.getClassificationName())) {
            stoneCondition.and(stock.product.classificationName.eq(condition.getClassificationName()));
        }
        if (StringUtils.isNotBlank(condition.getFactoryName())) {
            stoneCondition.and(stock.factoryName.eq(condition.getFactoryName()));
        }

        List<Tuple> stoneResults = query
                .select(
                        sale.accountId,
                        orderStone.mainStone.when(true).then(orderStone.stoneQuantity.coalesce(0)).otherwise(0).sum(),
                        orderStone.mainStone.when(false).then(orderStone.stoneQuantity.coalesce(0)).otherwise(0).sum()
                )
                .from(saleItem)
                .join(saleItem.stock, stock)
                .join(stock.orderStones, orderStone)
                .join(saleItem.sale, sale)
                .where(stoneCondition)
                .groupBy(sale.accountId)
                .fetch();

        // 결과 조합
        java.util.Map<Long, DashboardDto.StoreTradeStatistics.StoreTradeStatisticsBuilder> builderMap = new java.util.HashMap<>();

        for (Tuple row : saleResults) {
            Long accountId = row.get(sale.accountId);
            String accountName = row.get(sale.accountName);
            SaleStatus status = row.get(saleItem.itemStatus);
            Long labor = row.get(3, Long.class);
            java.math.BigDecimal gold = row.get(4, java.math.BigDecimal.class);
            Long count = row.get(5, Long.class);
            Long purchase = row.get(6, Long.class);

            labor = labor != null ? labor : 0L;
            gold = gold != null ? gold : java.math.BigDecimal.ZERO;
            count = count != null ? count : 0L;
            purchase = purchase != null ? purchase : 0L;

            DashboardDto.StoreTradeStatistics.StoreTradeStatisticsBuilder builder = builderMap.computeIfAbsent(accountId,
                    k -> DashboardDto.StoreTradeStatistics.builder().storeId(accountId).storeName(accountName));

            if (status == SaleStatus.SALE) {
                builder.saleLaborCost(labor).salePureGold(gold).saleCount(count).purchaseCost(purchase);
            } else if (status == SaleStatus.RETURN) {
                builder.returnLaborCost(labor).returnPureGold(gold).returnCount(count);
            } else if (status == SaleStatus.DISCOUNT) {
                builder.dcLaborCost(labor).dcPureGold(gold).dcCount(count);
            }
        }

        for (Tuple row : paymentResults) {
            Long accountId = row.get(sale.accountId);
            Integer cash = row.get(1, Integer.class);
            java.math.BigDecimal gold = row.get(2, java.math.BigDecimal.class);

            cash = cash != null ? cash : 0;
            gold = gold != null ? gold : java.math.BigDecimal.ZERO;

            DashboardDto.StoreTradeStatistics.StoreTradeStatisticsBuilder builder = builderMap.get(accountId);
            if (builder != null) {
                builder.paymentAmount(cash.longValue()).paymentPureGold(gold);
            }
        }

        for (Tuple row : stoneResults) {
            Long accountId = row.get(sale.accountId);
            Integer mainCount = row.get(1, Integer.class);
            Integer assistCount = row.get(2, Integer.class);

            mainCount = mainCount != null ? mainCount : 0;
            assistCount = assistCount != null ? assistCount : 0;

            DashboardDto.StoreTradeStatistics.StoreTradeStatisticsBuilder builder = builderMap.get(accountId);
            if (builder != null) {
                builder.saleMainStoneCount(mainCount.longValue()).saleAssistStoneCount(assistCount.longValue());
            }
        }

        return builderMap.values().stream()
                .map(DashboardDto.StoreTradeStatistics.StoreTradeStatisticsBuilder::build)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public DashboardDto.StoreStatisticsFilterOption findStoreStatisticsFilterOptions() {
        // 매장구분 (등급) 목록
        List<String> storeGrades = query
                .select(sale.accountGrade)
                .distinct()
                .from(sale)
                .where(sale.accountGrade.isNotNull())
                .orderBy(sale.accountGrade.asc())
                .fetch();

        // 재질구분 목록
        List<String> materials = query
                .select(stock.product.materialName)
                .distinct()
                .from(saleItem)
                .join(saleItem.stock, stock)
                .where(stock.product.materialName.isNotNull())
                .orderBy(stock.product.materialName.asc())
                .fetch();

        // 분류구분 목록
        List<String> classifications = query
                .select(stock.product.classificationName)
                .distinct()
                .from(saleItem)
                .join(saleItem.stock, stock)
                .where(stock.product.classificationName.isNotNull())
                .orderBy(stock.product.classificationName.asc())
                .fetch();

        // 매입처구분 목록
        List<String> factories = query
                .select(stock.factoryName)
                .distinct()
                .from(saleItem)
                .join(saleItem.stock, stock)
                .where(stock.factoryName.isNotNull())
                .orderBy(stock.factoryName.asc())
                .fetch();

        // 관리자구분 목록
        List<String> managers = query
                .select(sale.createdBy)
                .distinct()
                .from(sale)
                .where(sale.createdBy.isNotNull())
                .orderBy(sale.createdBy.asc())
                .fetch();

        // 통계선택 옵션 (고정값)
        List<String> statisticsTypes = java.util.Arrays.asList("STORE", "FACTORY");

        // 거래형태 (tradeTypes)는 account-service의 Store 엔티티에서 관리되므로 고정값 사용
        List<String> tradeTypes = java.util.Arrays.asList("도매", "소매", "온라인");

        return DashboardDto.StoreStatisticsFilterOption.builder()
                .storeGrades(storeGrades)
                .tradeTypes(tradeTypes)
                .materials(materials)
                .classifications(classifications)
                .factories(factories)
                .managers(managers)
                .statisticsTypes(statisticsTypes)
                .build();
    }

    @Override
    public DashboardDto.ReceivableSummary findReceivableSummary() {
        // 전체 판매 금액
        NumberExpression<Long> laborCost = Expressions.asNumber(0L)
                .add(stock.product.productLaborCost.coalesce(0))
                .add(stock.product.productAddLaborCost.coalesce(0))
                .add(stock.stoneMainLaborCost.coalesce(0))
                .add(stock.stoneAssistanceLaborCost.coalesce(0))
                .add(stock.stoneAddLaborCost.coalesce(0));

        Tuple saleTotal = query
                .select(
                        stock.product.goldWeight.sum(),
                        laborCost.sum()
                )
                .from(saleItem)
                .join(saleItem.stock, stock)
                .where(saleItem.itemStatus.eq(SaleStatus.SALE))
                .fetchOne();

        // 전체 결제 금액
        Tuple paymentTotal = query
                .select(
                        salePayment.pureGoldWeight.sum(),
                        salePayment.cashAmount.sum()
                )
                .from(salePayment)
                .where(salePayment.saleStatus.eq(SaleStatus.PAYMENT))
                .fetchOne();

        java.math.BigDecimal saleGold = saleTotal != null ? saleTotal.get(0, java.math.BigDecimal.class) : null;
        Long saleCash = saleTotal != null ? saleTotal.get(1, Long.class) : null;
        saleGold = saleGold != null ? saleGold : java.math.BigDecimal.ZERO;
        saleCash = saleCash != null ? saleCash : 0L;

        java.math.BigDecimal paymentGold = paymentTotal != null ? paymentTotal.get(0, java.math.BigDecimal.class) : null;
        Integer paymentCash = paymentTotal != null ? paymentTotal.get(1, Integer.class) : null;
        paymentGold = paymentGold != null ? paymentGold : java.math.BigDecimal.ZERO;
        paymentCash = paymentCash != null ? paymentCash : 0;

        return DashboardDto.ReceivableSummary.builder()
                .totalPureGold(saleGold.subtract(paymentGold))
                .totalAmount(saleCash - paymentCash.longValue())
                .build();
    }

    @Override
    public DashboardDto.RentalSummary findRentalSummary() {
        NumberExpression<Long> laborCost = Expressions.asNumber(0L)
                .add(stock.product.productLaborCost.coalesce(0))
                .add(stock.product.productAddLaborCost.coalesce(0))
                .add(stock.stoneMainLaborCost.coalesce(0))
                .add(stock.stoneAssistanceLaborCost.coalesce(0))
                .add(stock.stoneAddLaborCost.coalesce(0));

        DashboardDto.RentalSummary result = query
                .select(new QDashboardDto_RentalSummary(
                        stock.product.goldWeight.sum(),
                        laborCost.sum(),
                        stock.count()
                ))
                .from(stock)
                .where(
                        stock.stockDeleted.isFalse(),
                        stock.orderStatus.eq(OrderStatus.RENTAL)
                )
                .fetchOne();

        return result != null ? result : DashboardDto.RentalSummary.builder().build();
    }

    @Override
    public List<DashboardDto.RentalDetail> findRentalDetails() {
        NumberExpression<Long> laborCost = Expressions.asNumber(0L)
                .add(stock.product.productLaborCost.coalesce(0))
                .add(stock.product.productAddLaborCost.coalesce(0))
                .add(stock.stoneMainLaborCost.coalesce(0))
                .add(stock.stoneAssistanceLaborCost.coalesce(0))
                .add(stock.stoneAddLaborCost.coalesce(0));

        return query
                .select(new QDashboardDto_RentalDetail(
                        stock.storeId,
                        stock.storeName,
                        stock.product.goldWeight.sum(),
                        laborCost.sum(),
                        stock.count(),
                        stock.createDate.min().stringValue(),
                        stock.createDate.max().stringValue()
                ))
                .from(stock)
                .where(
                        stock.stockDeleted.isFalse(),
                        stock.orderStatus.eq(OrderStatus.RENTAL)
                )
                .groupBy(stock.storeId, stock.storeName)
                .orderBy(stock.storeName.asc())
                .fetch();
    }

    @Override
    public DashboardDto.FactoryUnpaidSummary findFactoryUnpaidSummary() {
        // 매입 총액 (공장으로부터 구매한 상품의 매입 비용)
        NumberExpression<Long> purchaseCost = Expressions.asNumber(0L)
                .add(stock.product.productPurchaseCost.coalesce(0))
                .add(stock.totalStonePurchaseCost.coalesce(0));

        Tuple purchaseTotal = query
                .select(
                        stock.product.goldWeight.sum(),
                        purchaseCost.sum()
                )
                .from(stock)
                .where(stock.stockDeleted.isFalse())
                .fetchOne();

        // 매입처에 대한 결제 (PURCHASE 상태)
        Tuple paymentTotal = query
                .select(
                        salePayment.pureGoldWeight.sum(),
                        salePayment.cashAmount.sum()
                )
                .from(salePayment)
                .where(salePayment.saleStatus.eq(SaleStatus.PURCHASE))
                .fetchOne();

        java.math.BigDecimal purchaseGold = purchaseTotal != null ? purchaseTotal.get(0, java.math.BigDecimal.class) : null;
        Long purchaseCash = purchaseTotal != null ? purchaseTotal.get(1, Long.class) : null;
        purchaseGold = purchaseGold != null ? purchaseGold : java.math.BigDecimal.ZERO;
        purchaseCash = purchaseCash != null ? purchaseCash : 0L;

        java.math.BigDecimal paymentGold = paymentTotal != null ? paymentTotal.get(0, java.math.BigDecimal.class) : null;
        Integer paymentCash = paymentTotal != null ? paymentTotal.get(1, Integer.class) : null;
        paymentGold = paymentGold != null ? paymentGold : java.math.BigDecimal.ZERO;
        paymentCash = paymentCash != null ? paymentCash : 0;

        return DashboardDto.FactoryUnpaidSummary.builder()
                .totalPureGold(purchaseGold.subtract(paymentGold))
                .totalAmount(purchaseCash - paymentCash.longValue())
                .build();
    }
}
