package com.msa.product.local.stone.type.repository;

import com.msa.product.local.stone.type.dto.QStoneTypeDto_ResponseSingle;
import com.msa.product.local.stone.type.dto.StoneTypeDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.msa.product.local.stone.shape.entity.QStoneShape.stoneShape;

public class StoneTypeRepositoryImpl implements CustomStoneTypeRepository {

    private final JPAQueryFactory query;

    public StoneTypeRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<StoneTypeDto.ResponseSingle> findByStoneTypeAllOrderByAsc(String stoneTypeName) {
        return query
                .select(new QStoneTypeDto_ResponseSingle(
                        stoneShape.stoneShapeId.stringValue(),
                        stoneShape.stoneShapeName,
                        stoneShape.stoneShapeNote
                ))
                .from(stoneShape)
                .where(stoneShape.stoneShapeName.contains(stoneTypeName))
                .orderBy(stoneShape.stoneShapeName.asc())
                .fetch();
    }
}
