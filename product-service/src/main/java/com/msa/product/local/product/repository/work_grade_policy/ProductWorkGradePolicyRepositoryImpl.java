package com.msa.product.local.product.repository.work_grade_policy;

import com.msa.product.local.product.dto.ProductWorkGradePolicyDto;
import com.msa.product.local.product.dto.QProductWorkGradePolicyDto_Response;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.msa.product.local.product.entity.QProduct.product;
import static com.msa.product.local.product.entity.QProductWorkGradePolicy.productWorkGradePolicy;

public class ProductWorkGradePolicyRepositoryImpl implements CustomProductWorkGradePolicy{

    private final JPAQueryFactory query;

    public ProductWorkGradePolicyRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<ProductWorkGradePolicyDto.Response> findWorkGradePolicyByProductId(Long productId) {
        return query
                .select(new QProductWorkGradePolicyDto_Response(
                        productWorkGradePolicy.productWorkGradePolicyId.stringValue(),
                        productWorkGradePolicy.grade.stringValue(),
                        productWorkGradePolicy.laborCost,
                        productWorkGradePolicy.productPolicyNote
                ))
                .from(productWorkGradePolicy)
                .join(productWorkGradePolicy.product, product)
                .where(productWorkGradePolicy.product.productId.eq(productId))
                .orderBy(productWorkGradePolicy.productWorkGradePolicyId.desc())
                .fetch();
    }
}
