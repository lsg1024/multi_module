package com.msa.product.local.product.repository.work_grade_policy_group.work_grade_policy;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

public class ProductWorkGradePolicyRepositoryImpl implements CustomProductWorkGradePolicy {

    private final JPAQueryFactory query;

    public ProductWorkGradePolicyRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

}
