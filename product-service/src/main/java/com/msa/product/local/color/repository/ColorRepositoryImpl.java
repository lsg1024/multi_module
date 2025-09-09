package com.msa.product.local.color.repository;

import com.msa.product.local.color.dto.ColorDto;
import com.msa.product.local.color.dto.QColorDto_ResponseSingle;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.msa.product.local.color.entity.QColor.color;

public class ColorRepositoryImpl implements CustomColorRepository{

    private final JPAQueryFactory query;

    public ColorRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<ColorDto.ResponseSingle> findAllOrderByAsc(String colorName) {

        BooleanExpression name = colorName != null ? color.colorName.contains(colorName) : null;
        return query
                .select(new QColorDto_ResponseSingle(
                        color.colorId.stringValue(),
                        color.colorName,
                        color.colorNote
                ))
                .from(color)
                .where(name)
                .orderBy(color.colorName.asc())
                .fetch();
    }
}
