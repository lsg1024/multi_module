package com.msa.order.local.domain.stock.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.order.entity.OrderStone;
import com.msa.order.local.domain.order.entity.order_enum.OrderStatus;
import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import com.msa.order.local.domain.stock.dto.StockDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.msa.order.local.domain.order.entity.QOrderProduct.orderProduct;
import static com.msa.order.local.domain.order.entity.QOrderStone.orderStone;
import static com.msa.order.local.domain.order.entity.QOrders.orders;

@Repository
public class StockRepositoryImpl implements CustomStockRepository {

    private final JPAQueryFactory query;

    public StockRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public CustomPage<StockDto.Response> findByStockProducts(String input, StockDto.StockCondition condition, Pageable pageable) {

        BooleanBuilder searchBuilder = getSearchBuilder(input);
        BooleanExpression stockStatusBuilder = getStockStatusBuilder(condition);

        OffsetDateTime start = null;
        OffsetDateTime end = null;
        if (condition != null && condition.getStartAt() != null && condition.getEndAt() != null) {
            LocalDate s = LocalDate.parse(condition.getStartAt());
            LocalDate e = LocalDate.parse(condition.getEndAt());
            start = s.atStartOfDay().atOffset(ZoneOffset.of("+09:00"));
            end = e.atTime(23, 59, 59).atOffset(ZoneOffset.of("+09:00"));
        }


        // stock 엔티티로 변경
        List<Long> pageOrderIds = query
                .select(orders.orderId)
                .from(orders)
                .where(
                        orders.orderStatus.eq(OrderStatus.STOCK), // 재고만?
                        start != null ? orders.orderDate.goe(start) : null,
                        end != null ? orders.orderDate.loe(end) : null
                )
                .orderBy(orders.orderDate.desc(), orders.orderId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<Tuple> basics = query
                .select(
                        orders.orderId,                   // 0
                        orders.flowCode,                 // 1
                        orders.orderDate,                 // 2 (createAt)
                        orders.productStatus,             // 3
                        orders.storeName,                 // 4
                        orderProduct.productSize,         // 5
                        orders.orderNote,                 // 6
                        orderProduct.materialName,        // 7
                        orderProduct.classificationName,  // 8
                        orderProduct.colorName,           // 9
                        orderProduct.productLaborCost,    // 10
                        orderProduct.productAddLaborCost  // 11
                )
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .where(orders.orderId.in(pageOrderIds))
                .fetch();

        // 4) 스톤 일괄 조회 후 orderId 기준 그룹핑 (페이지 범위 내)
        List<OrderStone> stones = query
                .selectFrom(orderStone)
                .where(orderStone.order.orderId.in(pageOrderIds))
                .fetch();

        Map<Long, List<OrderStone>> stonesByOrderId = stones.stream()
                .collect(Collectors.groupingBy(os -> os.getOrder().getOrderId()));

        // 5) 응답 리스트 생성 (페이지 id 순서 보장)
        //    LinkedHashMap/리스트로 원래 정렬 유지
        Map<Long, Tuple> basicByOrderId = new LinkedHashMap<>();
        for (Tuple t : basics) {
            basicByOrderId.put(t.get(0, Long.class), t);
        }

        List<StockDto.Response> content = new ArrayList<>(pageOrderIds.size());
        for (Long oid : pageOrderIds) {
            Tuple t = basicByOrderId.get(oid);
            if (t == null) continue;

            List<OrderStone> list = stonesByOrderId.getOrDefault(oid, Collections.emptyList());

            int mainCostSum = list.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getProductStoneMain()))
                    .mapToInt(s -> safeInt(s.getStoneLaborCost()) * safeInt(s.getStoneQuantity()))
                    .sum();

            int subCostSum = list.stream()
                    .filter(s -> !Boolean.TRUE.equals(s.getProductStoneMain()))
                    .mapToInt(s -> safeInt(s.getStoneLaborCost()) * safeInt(s.getStoneQuantity()))
                    .sum();

            int mainQtySum = list.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getProductStoneMain()))
                    .mapToInt(s -> safeInt(s.getStoneQuantity()))
                    .sum();

            int subQtySum = list.stream()
                    .filter(s -> !Boolean.TRUE.equals(s.getProductStoneMain()))
                    .mapToInt(s -> safeInt(s.getStoneQuantity()))
                    .sum();

            String orderCode = t.get(1, String.class);
            OffsetDateTime createAt = t.get(2, OffsetDateTime.class);
            ProductStatus status = t.get(3, ProductStatus.class);
            String storeName = t.get(4, String.class);
            String productSize = t.get(5, String.class);
            String orderNote = t.get(6, String.class);
            String materialName = t.get(7, String.class);
            String classificationName = t.get(8, String.class);
            String colorName = t.get(9, String.class);
            Integer productLaborCost = t.get(10, Integer.class);
            Integer productAddLaborCost = t.get(11, Integer.class);

            StockDto.Response dto = new StockDto.Response(
                    orderCode,
                    createAt != null ? createAt.toString() : null,
                    status,
                    storeName,
                    productSize,
                    orderNote,
                    materialName,
                    classificationName,
                    colorName,
                    toStr(productLaborCost),
                    toStr(productAddLaborCost),
                    String.valueOf(mainCostSum),
                    String.valueOf(subCostSum),
                    null, // mainStoneNote
                    null, // assistanceStoneNote
                    String.valueOf(mainQtySum),
                    String.valueOf(subQtySum),
                    null, // stoneWeight (원하면 계산 추가)
                    null, // totalWeight
                    null, // goldHarry
                    null, // productPurchaseCost
                    null  // stonePurchaseCost
            );

            content.add(dto);
        }

        JPAQuery<Long> countQuery = query
                .select(orders.orderId.count())
                .from(orders)
                .where(
                        searchBuilder,
                        stockStatusBuilder,
                        orders.orderStatus.eq(OrderStatus.STOCK),
                        start != null ? orders.orderDate.goe(start) : null,
                        end != null ? orders.orderDate.loe(end) : null
                );

        // 6) CustomPage 로 포장
        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    private static int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private static String toStr(Integer v) {
        return v == null ? null : v.toString();
    }

    @NotNull
    private static BooleanBuilder getSearchBuilder(String searchInput) {
        BooleanBuilder booleanInput = new BooleanBuilder();

        if (StringUtils.hasText(searchInput)) {
            booleanInput.and(orderProduct.productName.containsIgnoreCase(searchInput));
            booleanInput.or(orders.storeName.containsIgnoreCase(searchInput));
            booleanInput.or(orders.factoryName.containsIgnoreCase(searchInput));
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

        BooleanExpression createdBetween =
                orders.orderDate.between(startDateTime, endDateTime);

        BooleanExpression statusIsReceiptOrWaiting =
                orders.orderStatus.in(OrderStatus.NONE);

        return statusIsReceiptOrWaiting.and(createdBetween);
    }
}
