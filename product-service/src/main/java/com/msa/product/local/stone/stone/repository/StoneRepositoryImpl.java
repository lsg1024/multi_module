package com.msa.product.local.stone.stone.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

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
    public CustomPage<StoneDto.PageDto> findAllStones(String stoneName, String stoneShape, String stoneType, String sortField, String sort, Pageable pageable) {

        OrderSpecifier<?>[] specifiers = specifiers(sortField, sort);

        List<Long> ids = query
                .select(stone.stoneId)
                .from(stone)
                .where(stoneNameEq(stoneName), stoneNameLike(stoneShape, stoneType))
                .orderBy(specifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (ids.isEmpty()) {
            JPAQuery<Long> countQuery = query
                    .select(stone.count())
                    .from(stone)
                    .where(stoneNameEq(stoneName), stoneNameLike(stoneShape, stoneType));
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

        // Total Count 쿼리
        JPAQuery<Long> total = query
                .select(stone.count())
                .from(stone)
                .where(stoneNameEq(stoneName), stoneNameLike(stoneShape, stoneType));

        return new CustomPage<>(content, pageable, total.fetchOne());
    }

    private BooleanBuilder stoneNameLike(String stoneShape, String stoneType) {
        BooleanBuilder builder = new BooleanBuilder();

        // stoneShape
        if (StringUtils.hasText(stoneShape)) {
            builder.or(stone.stoneName.containsIgnoreCase(stoneShape));
        }

        // stoneType
        if (StringUtils.hasText(stoneType)) {
            builder.or(stone.stoneName.containsIgnoreCase(stoneType));
        }

        return builder;
    }

    private BooleanExpression stoneNameEq(String stoneName) {
        return stoneName != null ? stone.stoneName.containsIgnoreCase(stoneName) : null;
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
}
