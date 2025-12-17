package com.msa.product.local.stone.stone.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public CustomPage<StoneDto.PageDto> findByAllOrderByAsc(String stoneName, Pageable pageable) {

        List<Long> ids = query
                .select(stone.stoneId)
                .from(stone)
                .where(stoneName != null ? stone.stoneName.containsIgnoreCase(stoneName) : null)
                .orderBy(stone.stoneName.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (ids.isEmpty()) {
            JPAQuery<Long> countQuery = query.select(stone.count()).from(stone)
                    .where(stoneName != null ? stone.stoneName.containsIgnoreCase(stoneName) : null);
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
                }).toList();

        // Total Count 쿼리
        Long total = query.select(stone.count())
                .from(stone)
                .where(stoneName != null ? stone.stoneName.containsIgnoreCase(stoneName) : null)
                .fetchOne();

        return new CustomPage<>(content, pageable, total);
    }
}
