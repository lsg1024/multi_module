package com.msa.order.local.order.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.global.excel.dto.OrderExcelQueryDto;
import com.msa.order.global.excel.dto.QOrderExcelQueryDto;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.dto.OrderQueryDto;
import com.msa.order.local.order.dto.QOrderQueryDto;
import com.msa.order.local.order.dto.StockCondition;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.msa.order.local.order.entity.order_enum.SourceType;
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
import java.time.ZoneId;
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

/**
 * 주문 QueryDSL 동적 쿼리 구현체.
 *
 * *주문 목록 조회, 출고 예정 조회, 삭제된 주문 조회, 필터 목록 조회,
 * 엑셀 데이터 조회 등 다양한 동적 쿼리를 제공한다.
 *
 * *주요 특징:
 *
 *   - {@link BooleanBuilder}를 이용한 다중 조건 필터 조합
 *   - {@code StatusHistory} EXISTS 서브쿼리로 주문 상태 이력 검증
 *   - 재고 수량 서브쿼리 및 (상품명·재질·컬러) 키 기반 flowCode 맵 후처리로 2단계 로딩
 *   - 동적 정렬 — 공장·매장·세트유형·컬러 필드별 ASC/DESC 지원
 * 
 *
 * *의존성: {@link JPAQueryFactory}, {@code DEFAULT_STORE_STOCK_ID}(매장 재고 고정 ID = 1)
 */
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
        } else if(condition.getOrderStatus().equals("DELETED")) {
            ordersStatusBuilder = getDeleteBuilder((condition));
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
        } else if(condition.getOrderStatus().equals("DELETED")) {
            ordersStatusBuilder = getDeleteBuilder((condition));
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
        } else if(condition.getOrderStatus().equals("DELETED")) {
            ordersStatusBuilder = getDeleteBuilder((condition));
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
        } else if(condition.getOrderStatus().equals("DELETED")) {
            ordersStatusBuilder = getDeleteBuilder((condition));
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
    public List<String> findByFilterClassification(OrderDto.OrderCondition condition) {

        BooleanExpression ordersStatusBuilder;
        if (condition.getOrderStatus().equals("EXPECT")) {
            ordersStatusBuilder = getExpectBuilder(condition);
        } else if(condition.getOrderStatus().equals("DELETED")) {
            ordersStatusBuilder = getDeleteBuilder((condition));
        } else {
            ordersStatusBuilder = getOrdersStatusBuilder(condition);
        }
        return query
                .selectDistinct(orders.orderProduct.classificationName)
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .where(ordersStatusBuilder)
                .fetch();
    }

    @Override
    public List<String> findByFilterMaterial(OrderDto.OrderCondition condition) {

        BooleanExpression ordersStatusBuilder;
        if (condition.getOrderStatus().equals("EXPECT")) {
            ordersStatusBuilder = getExpectBuilder(condition);
        } else if(condition.getOrderStatus().equals("DELETED")) {
            ordersStatusBuilder = getDeleteBuilder((condition));
        } else {
            ordersStatusBuilder = getOrdersStatusBuilder(condition);
        }
        return query
                .selectDistinct(orders.orderProduct.materialName)
                .from(orders)
                .join(orders.orderProduct, orderProduct)
                .where(ordersStatusBuilder)
                .fetch();
    }

    @Override
    public List<OrderExcelQueryDto> findByExcelData(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition condition) {

        BooleanBuilder searchBuilder = getSearchBuilder(inputCondition);
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
                        searchBuilder,
                        optionBuilder,
                        ordersReceiptStatusBuilder
                )
                .orderBy(orders.factoryName.asc())
                .fetch();
    }

    /**
     * 주문 목록 페이징 쿼리를 실행하고 재고 flowCode 맵을 후처리하여 응답을 구성한다.
     *
     * *처리 흐름:
     *
     *   - 재고 수량 서브쿼리({@code stockQty})를 인라인으로 구성하여 SELECT에 포함
     *   - 동적 정렬 스펙({@link #createOrderSpecifiers})을 적용하여 본 쿼리 실행
     *   - 결과 DTO에서 (productName, materialName, colorName) 키 집합을 추출
     *   - 해당 키들의 {@code stock.flowCode} 목록을 단일 배치 쿼리로 조회
     *   - Tuple을 {@code Map<StockCondition, List<String>>}으로 그룹핑 후 각 DTO에 주입
     *   - 카운트 쿼리를 별도로 실행하여 {@link CustomPage} 반환
     * 
     *
     * @param pageable        페이징 정보
     * @param sortCondition   동적 정렬 조건
     * @param conditionBuilder 검색어 필터
     * @param statusBuilder   상태/날짜 필터
     * @param optionBuilder   드롭다운 옵션 필터
     * @param orderDeleted    삭제 여부 플래그
     * @return 페이징된 주문 쿼리 결과
     */
    @NotNull
    private CustomPage<OrderQueryDto> getResponse(Pageable pageable, OrderDto.SortCondition sortCondition, BooleanBuilder conditionBuilder, BooleanExpression statusBuilder, BooleanBuilder optionBuilder, Boolean orderDeleted) {

        JPQLQuery<Integer> stockQty = JPAExpressions
                .select(stock.stockCode.count().intValue())
                .from(stock)
                .where(
                        stock.stockDeleted.isFalse(),
                        stock.orderStatus.in(OrderStatus.NORMAL, OrderStatus.STOCK),
                        stock.storeId.eq(DEFAULT_STORE_STOCK_ID),
                        stock.product.productName.eq(orderProduct.productName),
                        stock.product.materialName.eq(orderProduct.materialName),
                        stock.product.colorName.eq(orderProduct.colorName)
                );

//        JPQLQuery<List<String>> statusHistories = JPAExpressions
//                .select(statusHistory.)

        OrderSpecifier<?>[] orderSpecifiers = createOrderSpecifiers(sortCondition);

        List<OrderQueryDto> content = query
                .select(new QOrderQueryDto(
                        orders.orderProduct.productId,
                        orders.createAt.stringValue(),
                        orders.shippingAt.stringValue(),
                        orders.flowCode.stringValue(),
                        orders.storeName,
                        orders.orderProduct.productName,
                        orders.orderProduct.productFactoryName,
                        orders.orderProduct.materialName,
                        orderProduct.colorName,
                        orderProduct.setTypeName,
                        orders.orderProduct.productSize,
                        stockQty,
                        orderProduct.orderMainStoneNote,
                        orderProduct.orderAssistanceStoneNote,
                        orderProduct.assistantStone,
                        orderProduct.assistantStoneName,
                        orderProduct.assistantStoneCreateAt.stringValue(),
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

    /**
     * 주문 검색 조건 빌더.
     *
     * 정책:
     *   - 검색 필터 미선택(기본값, {@code searchField} 가 비어있음) → 모든 대상 필드에 대해
     *     부분 일치({@code containsIgnoreCase}) LIKE 검색을 수행한다. (원활한 검색)
     *   - 검색 필터 선택됨 → 해당 필드에 대해 정확히 일치({@code eq}) 검색만 수행한다.
     *     (modelNumber 는 productName / productFactoryName 둘 중 하나가 정확히 일치해야 함)
     */
    @NotNull
    private static BooleanBuilder getSearchBuilder(OrderDto.InputCondition orderCondition) {
        BooleanBuilder searchBuilder = new BooleanBuilder();

        String searchInput = orderCondition.getSearchInput();
        String searchField = orderCondition.getSearchField();

        if (!StringUtils.hasText(searchInput)) {
            return searchBuilder;
        }

        if (!StringUtils.hasText(searchField)) {
            // 기본값(전체): 모든 주요 필드에 대해 LIKE (부분 일치, 대소문자 무시)
            searchBuilder.and(
                    orderProduct.productName.containsIgnoreCase(searchInput)
                            .or(orderProduct.productFactoryName.containsIgnoreCase(searchInput))
                            .or(orders.storeName.containsIgnoreCase(searchInput))
                            .or(orders.factoryName.containsIgnoreCase(searchInput))
            );
            return searchBuilder;
        }

        // 필터 선택 시: 해당 필드에 대해 정확히 일치(eq)
        switch (searchField) {
            case "modelNumber" -> searchBuilder.and(
                    orderProduct.productName.eq(searchInput)
                            .or(orderProduct.productFactoryName.eq(searchInput))
            );
            case "factory" -> searchBuilder.and(orders.factoryName.eq(searchInput));
            case "store" -> searchBuilder.and(orders.storeName.eq(searchInput));
            case "setType" -> searchBuilder.and(orderProduct.setTypeName.eq(searchInput));
            case "classification" -> searchBuilder.and(orderProduct.classificationName.eq(searchInput));
            case "material" -> searchBuilder.and(orderProduct.materialName.eq(searchInput));
            case "color" -> searchBuilder.and(orderProduct.colorName.eq(searchInput));
            default -> searchBuilder.and(
                    orderProduct.productName.containsIgnoreCase(searchInput)
                            .or(orderProduct.productFactoryName.containsIgnoreCase(searchInput))
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
            booleanOption.and(orders.storeName.eq(optionCondition.getStoreName()));
        }

        if (StringUtils.hasText(optionCondition.getFactoryName())) {
            booleanOption.and(orders.factoryName.eq(optionCondition.getFactoryName()));
        }

        if (StringUtils.hasText(optionCondition.getSetTypeName())) {
            booleanOption.and(orderProduct.setTypeName.eq(optionCondition.getSetTypeName()));
        }

        if (StringUtils.hasText(optionCondition.getColorName())) {
            booleanOption.and(orderProduct.colorName.eq(optionCondition.getColorName()));
        }

        if (StringUtils.hasText(optionCondition.getClassificationName())) {
            booleanOption.and(orderProduct.classificationName.eq(optionCondition.getClassificationName()));
        }

        if (StringUtils.hasText(optionCondition.getMaterialName())) {
            booleanOption.and(orderProduct.materialName.eq(optionCondition.getMaterialName()));
        }

        return booleanOption;
    }

    private static BooleanExpression getOrdersStatusBuilder(OrderDto.OrderCondition orderCondition) {
        String startAt = orderCondition.getStartAt();
        String endAt = orderCondition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneId.of("Asia/Seoul").getRules().getOffset(start));
        OffsetDateTime endDateTime = end.atOffset(ZoneId.of("Asia/Seoul").getRules().getOffset(end));

        BooleanExpression createdBetween =
                orders.createAt.between(startDateTime, endDateTime);

        BooleanExpression sourceHistoryStatus = hasStatusHistory(SourceType.valueOf(orderCondition.getOrderStatus()));

        BooleanExpression status = orders.orderStatus.in(OrderStatus.valueOf(orderCondition.getOrderStatus()), OrderStatus.STOCK);

        return sourceHistoryStatus.and(status.and(createdBetween));
    }

    private static BooleanExpression getExpectBuilder(OrderDto.OrderCondition orderCondition) {
        String endAt = orderCondition.getEndAt();

        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime endDateTime = end.atOffset(ZoneId.of("Asia/Seoul").getRules().getOffset(end));

        BooleanExpression shippingAt =
                orders.shippingAt.loe(endDateTime);

        BooleanExpression status = orders.orderStatus.notIn(OrderStatus.STOCK);

        return status.and(shippingAt);
    }

    private static BooleanExpression getDeleteBuilder(OrderDto.OrderCondition orderCondition) {
        String startAt = orderCondition.getStartAt();
        String endAt = orderCondition.getEndAt();

        LocalDateTime start = LocalDate.parse(startAt).atStartOfDay(); // 예: 2025-08-04 00:00:00
        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime startDateTime = start.atOffset(ZoneId.of("Asia/Seoul").getRules().getOffset(start));
        OffsetDateTime endDateTime = end.atOffset(ZoneId.of("Asia/Seoul").getRules().getOffset(end));

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

        OffsetDateTime startDateTime = start.atOffset(ZoneId.of("Asia/Seoul").getRules().getOffset(start));
        OffsetDateTime endDateTime = end.atOffset(ZoneId.of("Asia/Seoul").getRules().getOffset(end));

        BooleanExpression createdBetween =
                orders.createAt.between(startDateTime, endDateTime);

        BooleanExpression statusIsReceiptOrWaiting =
                orders.productStatus.eq(ProductStatus.RECEIPT);

        OrderStatus requestedStatus = OrderStatus.valueOf(orderCondition.getOrderStatus());
        // WAIT 상태의 신규 주문도 포함 (비동기 처리 전 상태)
        BooleanExpression status = requestedStatus == OrderStatus.ORDER
                ? orders.orderStatus.in(OrderStatus.ORDER, OrderStatus.WAIT)
                : orders.orderStatus.eq(requestedStatus);

        return statusIsReceiptOrWaiting.and(status).and(createdBetween);
    }

    private static BooleanExpression getExpectStatusBuilder(OrderDto.ExpectCondition orderCondition) {
        String endAt = orderCondition.getEndAt();

        LocalDateTime end = LocalDate.parse(endAt).atTime(23, 59, 59); // 예: 2025-08-05 23:59:59

        OffsetDateTime endDateTime = end.atOffset(ZoneId.of("Asia/Seoul").getRules().getOffset(end));

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
            // 기본 정렬: 날짜(최신순) → 상태 → 상품명
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, orders.createAt));
            orderSpecifiers.add(new OrderSpecifier<>(Order.ASC, orders.orderStatus));
            orderSpecifiers.add(new OrderSpecifier<>(Order.ASC, orders.orderProduct.productName));
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

    private static BooleanExpression hasStatusHistory(SourceType sourceType) {
        if (sourceType == null) {
            return null;
        }

        return orders.flowCode.in(
                JPAExpressions
                        .selectDistinct(statusHistory.flowCode)
                        .from(statusHistory)
                        .where(statusHistory.sourceType.eq(sourceType))
        );
    }

}
