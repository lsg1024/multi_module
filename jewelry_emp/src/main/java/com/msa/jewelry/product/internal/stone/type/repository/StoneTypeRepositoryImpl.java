package com.msa.jewelry.product.internal.stone.type.repository;

import com.msa.jewelry.product.internal.stone.type.dto.QStoneTypeDto_ResponseSingle;
import com.msa.jewelry.product.internal.stone.type.dto.StoneTypeDto;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.msa.jewelry.product.internal.stone.type.entity.QStoneType.stoneType;

public class StoneTypeRepositoryImpl implements CustomStoneTypeRepository {

    private final JPAQueryFactory query;

    public StoneTypeRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<StoneTypeDto.ResponseSingle> findByStoneTypeAllOrderByAsc(String stoneTypeName) {

        BooleanExpression searchCondition = stoneTypeName != null ? stoneType.stoneTypeName.containsIgnoreCase(stoneTypeName) : null;

        return query
                .select(new QStoneTypeDto_ResponseSingle(
                        stoneType.stoneTypeId.stringValue(),
                        stoneType.stoneTypeName,
                        stoneType.stoneTypeNote
                ))
                .from(stoneType)
                .where(searchCondition)
                .orderBy(stoneType.stoneTypeName.asc())
                .fetch();
    }
}
