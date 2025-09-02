package com.msa.product.local.classification.repository;

import com.msa.product.local.classification.dto.ClassificationDto;
import com.msa.product.local.classification.dto.QClassificationDto_ResponseSingle;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.msa.product.local.classification.entity.QClassification.classification;

public class ClassificationRepositoryImpl implements CustomClassificationRepository {

    private final JPAQueryFactory query;

    public ClassificationRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<ClassificationDto.ResponseSingle> findAllOrderByAsc(String classificationName) {

        BooleanExpression isClassificationName = classificationName != null ? classification.classificationName.contains(classificationName) : null;

        return query
                .select(new QClassificationDto_ResponseSingle(
                        classification.classificationId.stringValue(),
                        classification.classificationName,
                        classification.classificationNote
                ))
                .from(classification)
                .where(isClassificationName)
                .orderBy(classification.classificationName.asc())
                .fetch();
    }
}
