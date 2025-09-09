package com.msa.product.local.stone.shape.repository;

import com.msa.product.local.stone.shape.dto.QStoneShapeDto_ResponseSingle;
import com.msa.product.local.stone.shape.dto.StoneShapeDto;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.msa.product.local.stone.shape.entity.QStoneShape.stoneShape;

public class StoneShapeRepositoryImpl implements CustomStoneShapeRepository{

    private final JPAQueryFactory query;

    public StoneShapeRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<StoneShapeDto.ResponseSingle> findByStoneShapeAllOrderByAsc(String stoneShapeName) {

        BooleanExpression searchCondition = stoneShapeName != null ? stoneShape.stoneShapeName.containsIgnoreCase(stoneShapeName) : null;

        return query
                .select(new QStoneShapeDto_ResponseSingle(
                        stoneShape.stoneShapeId.stringValue(),
                        stoneShape.stoneShapeName,
                        stoneShape.stoneShapeNote
                ))
                .from(stoneShape)
                .where(searchCondition)
                .orderBy(stoneShape.stoneShapeName.asc())
                .fetch();
    }
}
