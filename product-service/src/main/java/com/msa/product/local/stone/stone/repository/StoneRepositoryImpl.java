package com.msa.product.local.stone.stone.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

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

        BooleanExpression cond = stoneName != null ? stone.stoneName.contains(stoneName) : null;

        List<Long> ids = query
                .select(stone.stoneId)
                .from(stone)
                .where(cond)
                .orderBy(stone.stoneName.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (ids.isEmpty()) {
            JPAQuery<Long> queryCount = query.select(stone.count()).from(stone).where(cond);
            return new CustomPage<>(List.of(), pageable, queryCount.fetchOne());
        }

        List<Stone> stones = query
                .selectFrom(stone)
                .leftJoin(stone.gradePolicies, stoneWorkGradePolicy).fetchJoin()
                .where(stone.stoneId.in(ids))
                .orderBy(stone.stoneName.asc(), stoneWorkGradePolicy.grade.asc())
                .distinct()
                .fetch();

        List<StoneDto.PageDto> content = stones.stream()
                .map(s -> StoneDto.PageDto.builder()
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
                        .build()
                ).toList();

        JPAQuery<Long> countQuery = query
                .select(stone.count())
                .from(stone)
                .where(cond);

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }
}
