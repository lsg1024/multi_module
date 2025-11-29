package com.msa.product.local.set.repository;

import com.msa.product.local.set.dto.QSetTypeDto_ResponseSingle;
import com.msa.product.local.set.dto.SetTypeDto;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.msa.product.local.set.entity.QSetType.setType;


public class SetTypeRepositoryImpl implements CustomSetTypeRepository{

    private final JPAQueryFactory query;

    public SetTypeRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }


    @Override
    public List<SetTypeDto.ResponseSingle> findAllOrderByAsc(String setName) {
        BooleanExpression name = setName != null ? setType.setTypeName.contains(setName) : null;
        return query
                .select(new QSetTypeDto_ResponseSingle(
                        setType.setTypeId.stringValue(),
                        setType.setTypeName,
                        setType.setTypeNote
                ))
                .from(setType)
                .where(name)
                .orderBy(setType.setTypeName.asc())
                .fetch();
    }
}
