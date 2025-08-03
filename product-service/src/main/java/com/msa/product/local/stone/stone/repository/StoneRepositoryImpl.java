package com.msa.product.local.stone.stone.repository;

import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.common.global.util.CustomPage;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        List<Stone> stones = query
                .selectFrom(stone)
                .join(stone.gradePolicies, stoneWorkGradePolicy).fetchJoin()
                .where(stone.stoneName.eq(stoneName))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(stone.stoneName.asc())
                .fetch();

        List<StoneDto.PageDto> content = stones.stream()
                .map(stone -> StoneDto.PageDto.builder()
                        .stoneId(stone.getStoneId().toString())
                        .stoneName(stone.getStoneName())
                        .stoneNote(stone.getStoneNote())
                        .stoneWeight(stone.getStoneWeight().stripTrailingZeros().toPlainString())
                        .stonePurchasePrice(stone.getStonePurchasePrice())
                        .stoneWorkGradePolicyDto(stone.getGradePolicies())
                        .build()).collect(Collectors.toList());

        JPAQuery<Long> countQuery = query
                .select(stone.count())
                .from(stone)
                .where(stone.stoneName.eq(stoneName));

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }
}
