package com.msa.product.local.stone.stone.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.product.global.excel.dto.StoneExcelDto;
import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.grade.WorkGrade;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.msa.product.local.product.entity.QProduct.product;
import static com.msa.product.local.product.entity.QProductImage.productImage;
import static com.msa.product.local.product.entity.QProductStone.productStone;
import static com.msa.product.local.stone.stone.entity.QStone.stone;
import static com.msa.product.local.stone.stone.entity.QStoneWorkGradePolicy.stoneWorkGradePolicy;

public class StoneRepositoryImpl implements CustomStoneRepository {

    private final JPAQueryFactory query;

    public StoneRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public Optional<Stone> findFetchJoinById(Long stoneId) {
        return Optional.ofNullable(
                query.selectFrom(stone)
                        .leftJoin(stone.gradePolicies, stoneWorkGradePolicy).fetchJoin()
                        .where(stone.stoneId.eq(stoneId))
                        .fetchOne()
        );
    }

    @Override
    public CustomPage<StoneDto.PageDto> findAllStones(String search, String searchField, String searchMin, String searchMax, String sortField, String sortOrder, Pageable pageable) {

        OrderSpecifier<?>[] specifiers = specifiers(sortField, sortOrder);

        BooleanBuilder builder = buildSearchConditions(search, searchField, searchMin, searchMax);

        List<Long> ids = query
                .select(stone.stoneId)
                .from(stone)
                .leftJoin(stone.gradePolicies, stoneWorkGradePolicy)
                .where(builder)
                .groupBy(stone.stoneId)
                .orderBy(specifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (ids.isEmpty()) {
            JPAQuery<Long> countQuery = query
                    .select(stone.countDistinct())
                    .from(stone)
                    .leftJoin(stone.gradePolicies, stoneWorkGradePolicy)
                    .where(builder);
            return new CustomPage<>(List.of(), pageable, countQuery.fetchOne());
        }

        List<Stone> stones = query
                .selectFrom(stone)
                .leftJoin(stone.gradePolicies).fetchJoin()
                .where(stone.stoneId.in(ids))
                .distinct()
                .fetch();

        List<Tuple> rawProductData = query
                .select(
                        productStone.stone.stoneId,
                        product.productId,
                        product.productName,
                        productImage.imagePath
                )
                .from(productStone)
                .join(productStone.product, product)
                .leftJoin(product.productImages, productImage)
                .where(productStone.stone.stoneId.in(ids)
                        .and(product.productDeleted.isFalse()))
                .fetch();

        Map<Long, List<StoneDto.ProductInfo>> stoneProductMap = rawProductData.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(productStone.stone.stoneId),
                        Collectors.mapping(
                                tuple -> StoneDto.ProductInfo.builder()
                                        .productId(tuple.get(product.productId))
                                        .productName(tuple.get(product.productName))
                                        .imagePath(tuple.get(productImage.imagePath))
                                        .build(),
                                Collectors.toList()
                        )
                ));

        // 5. 최종 결과 매핑
        List<StoneDto.PageDto> content = stones.stream()
                .map(s -> {
                    List<StoneDto.ProductInfo> productInfos = stoneProductMap.getOrDefault(s.getStoneId(), Collections.emptyList());

                    return StoneDto.PageDto.builder()
                            .stoneId(String.valueOf(s.getStoneId()))
                            .stoneName(s.getStoneName())
                            .stoneNote(s.getStoneNote())
                            .stoneWeight(s.getStoneWeight().stripTrailingZeros().toPlainString())
                            .stonePurchasePrice(s.getStonePurchasePrice())
                            .stoneWorkGradePolicyDto(
                                    s.getGradePolicies().stream()
                                            .map(p -> StoneWorkGradePolicyDto.Response.builder()
                                                    .workGradePolicyId(String.valueOf(p.getStoneWorkGradePolicyId()))
                                                    .grade(p.getGrade().name())
                                                    .laborCost(p.getLaborCost())
                                                    .build()
                                            ).toList()
                            )
                            .productInfos(productInfos)
                            .productCount(productInfos.size())
                            .build();
                })
                .sorted(Comparator.comparing(dto -> ids.indexOf(Long.parseLong(dto.getStoneId()))))
                .toList();

        JPAQuery<Long> total = query
                .select(stone.countDistinct())
                .from(stone)
                .leftJoin(stone.gradePolicies, stoneWorkGradePolicy)
                .where(builder);

        return new CustomPage<>(content, pageable, total.fetchOne());
    }

    private BooleanBuilder buildSearchConditions(String search, String searchField, String searchMin, String searchMax) {
        BooleanBuilder builder = new BooleanBuilder();

        // searchField가 없으면 기본적으로 stoneName 검색
        if (!StringUtils.hasText(searchField)) {
            if (StringUtils.hasText(search)) {
                builder.and(stone.stoneName.containsIgnoreCase(search));
            }
            return builder;
        }

        // searchField에 따른 조건 분기
        switch (searchField) {
            // 텍스트 검색 (search 값 사용)
            case "stoneName", "stoneType", "stoneShape", "stoneSize" -> {
                if (StringUtils.hasText(search)) {
                    builder.and(stone.stoneName.containsIgnoreCase(search));
                }
            }
            // 범위 검색 (searchMin, searchMax 사용)
            case "weight" -> {
                if (StringUtils.hasText(searchMin)) {
                    builder.and(stone.stoneWeight.goe(new BigDecimal(searchMin)));
                }
                if (StringUtils.hasText(searchMax)) {
                    builder.and(stone.stoneWeight.loe(new BigDecimal(searchMax)));
                }
            }
            case "purchasePrice" -> {
                if (StringUtils.hasText(searchMin)) {
                    builder.and(stone.stonePurchasePrice.goe(Integer.parseInt(searchMin)));
                }
                if (StringUtils.hasText(searchMax)) {
                    builder.and(stone.stonePurchasePrice.loe(Integer.parseInt(searchMax)));
                }
            }
            case "salePrice1" -> {
                builder.and(stoneWorkGradePolicy.grade.eq(WorkGrade.GRADE_1));
                if (StringUtils.hasText(searchMin)) {
                    builder.and(stoneWorkGradePolicy.laborCost.goe(Integer.parseInt(searchMin)));
                }
                if (StringUtils.hasText(searchMax)) {
                    builder.and(stoneWorkGradePolicy.laborCost.loe(Integer.parseInt(searchMax)));
                }
            }
            case "salePrice2" -> {
                builder.and(stoneWorkGradePolicy.grade.eq(WorkGrade.GRADE_2));
                if (StringUtils.hasText(searchMin)) {
                    builder.and(stoneWorkGradePolicy.laborCost.goe(Integer.parseInt(searchMin)));
                }
                if (StringUtils.hasText(searchMax)) {
                    builder.and(stoneWorkGradePolicy.laborCost.loe(Integer.parseInt(searchMax)));
                }
            }
            case "salePrice3" -> {
                builder.and(stoneWorkGradePolicy.grade.eq(WorkGrade.GRADE_3));
                if (StringUtils.hasText(searchMin)) {
                    builder.and(stoneWorkGradePolicy.laborCost.goe(Integer.parseInt(searchMin)));
                }
                if (StringUtils.hasText(searchMax)) {
                    builder.and(stoneWorkGradePolicy.laborCost.loe(Integer.parseInt(searchMax)));
                }
            }
            case "salePrice4" -> {
                builder.and(stoneWorkGradePolicy.grade.eq(WorkGrade.GRADE_4));
                if (StringUtils.hasText(searchMin)) {
                    builder.and(stoneWorkGradePolicy.laborCost.goe(Integer.parseInt(searchMin)));
                }
                if (StringUtils.hasText(searchMax)) {
                    builder.and(stoneWorkGradePolicy.laborCost.loe(Integer.parseInt(searchMax)));
                }
            }
        }

        return builder;
    }

    private OrderSpecifier<?>[] specifiers(String sortField, String sort) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (StringUtils.hasText(sortField)) {
            Order direction = "ASC".equalsIgnoreCase(sort) ? Order.ASC : Order.DESC;

            switch (sortField) {
                case "name" -> orderSpecifiers.add(new OrderSpecifier<>(direction, stone.stoneName));
                case "weight" -> orderSpecifiers.add(new OrderSpecifier<>(direction, stone.stoneWeight));
                case "count" -> {
                    var productCountSubQuery = JPAExpressions
                            .select(productStone.count())
                            .from(productStone)
                            .join(productStone.product, product)
                            .where(productStone.stone.eq(stone)
                                    .and(product.productDeleted.isFalse()));

                    orderSpecifiers.add(new OrderSpecifier<>(direction, productCountSubQuery));
                }
                case "purchase" -> orderSpecifiers.add(new OrderSpecifier<>(direction, stone.stonePurchasePrice));

                default -> orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, stone.stoneName));
            }
        } else {
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, stone.stoneName));
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

    @Override
    public List<StoneExcelDto> findStonesForExcel(String stoneName, String stoneShape, String stoneType) {
        List<Stone> stones = query
                .selectFrom(stone)
                .where(stoneNameEq(stoneName), stoneNameLike(stoneShape, stoneType))
                .orderBy(stone.stoneName.asc())
                .fetch();

        List<Long> stoneIds = stones.stream().map(Stone::getStoneId).toList();

        NumberExpression<Long> countExpr = productStone.count();

        Map<Long, Long> productCountMap = query
                .select(productStone.stone.stoneId, countExpr)
                .from(productStone)
                .join(productStone.product, product)
                .where(productStone.stone.stoneId.in(stoneIds)
                        .and(product.productDeleted.isFalse()))
                .groupBy(productStone.stone.stoneId)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(productStone.stone.stoneId),
                        tuple -> tuple.get(countExpr)
                ));

        return stones.stream()
                .map(s -> {
                    String[] nameParts = s.getStoneName().split("/");
                    String type = nameParts.length > 0 ? nameParts[0] : "";
                    String shape = nameParts.length > 1 ? nameParts[1] : "";
                    String size = nameParts.length > 2 ? nameParts[2] : "";

                    return StoneExcelDto.builder()
                            .stoneId(String.valueOf(s.getStoneId()))
                            .stoneName(s.getStoneName())
                            .stoneType(type)
                            .stoneShape(shape)
                            .stoneSize(size)
                            .stoneWeight(s.getStoneWeight().stripTrailingZeros().toPlainString())
                            .stonePurchasePrice(s.getStonePurchasePrice())
                            .stoneNote(s.getStoneNote())
                            .productCount(productCountMap.getOrDefault(s.getStoneId(), 0L).intValue())
                            .build();
                })
                .toList();
    }

    private BooleanExpression stoneNameEq(String stoneName) {
        return StringUtils.hasText(stoneName) ? stone.stoneName.containsIgnoreCase(stoneName) : null;
    }

    private BooleanBuilder stoneNameLike(String stoneShape, String stoneType) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(stoneShape)) {
            builder.and(stone.stoneName.containsIgnoreCase(stoneShape));
        }
        if (StringUtils.hasText(stoneType)) {
            builder.and(stone.stoneName.containsIgnoreCase(stoneType));
        }
        return builder;
    }
}
