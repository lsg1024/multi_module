package com.msa.product.local.product.repository;

import com.msa.product.local.grade.WorkGrade;
import com.msa.product.local.classification.dto.QClassificationDto_ResponseSingle;
import com.msa.product.local.material.dto.QMaterialDto_ResponseSingle;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.dto.QProductDto_Detail;
import com.msa.product.local.product.dto.QProductDto_Page;
import com.msa.product.local.set.dto.QSetTypeDto_ResponseSingle;
import com.msacommon.global.util.CustomPage;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static com.msa.product.local.classification.entity.QClassification.classification;
import static com.msa.product.local.material.entity.QMaterial.material;
import static com.msa.product.local.product.entity.QProduct.product;
import static com.msa.product.local.product.entity.QProductImage.productImage;
import static com.msa.product.local.product.entity.QProductStone.productStone;
import static com.msa.product.local.product.entity.QProductWorkGradePolicy.productWorkGradePolicy;
import static com.msa.product.local.set.entity.QSetType.setType;
import static com.msa.product.local.stone.stone.entity.QStoneWorkGradePolicy.stoneWorkGradePolicy;

public class ProductRepositoryImpl implements CustomProductRepository {

    private final JPAQueryFactory query;

    public ProductRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public ProductDto.Detail findByProductId(Long productId) {
        return query
                .select(new QProductDto_Detail(
                        product.productId.stringValue(),
                        product.factoryId,
                        product.factoryName,
                        product.productFactoryName,
                        product.productName,
                        product.standardWeight.stringValue(),
                        product.productNote,
                        new QSetTypeDto_ResponseSingle(
                                setType.setTypeId.stringValue(),
                                setType.setTypeName,
                                setType.setTypeNote
                        ),
                        new QClassificationDto_ResponseSingle(
                                classification.classificationId.stringValue(),
                                classification.classificationName,
                                classification.classificationNote
                        ),
                        new QMaterialDto_ResponseSingle(
                                material.materialId.stringValue(),
                                material.materialName,
                                material.materialGoldPurityPercent.stringValue()
                        ),
                        Expressions.constant(Collections.emptyList()), // gradePolicyDtos
                        Expressions.constant(Collections.emptyList()), // productStoneDtos
                        Expressions.constant(Collections.emptyList())  // productImageDtos
                ))
                .from(product)
                .join(product.setType, setType)
                .join(product.classification, classification)
                .join(product.material, material)
                .where(product.productId.eq(productId))
                .fetchOne();
    }

    @Override
    public CustomPage<ProductDto.Page> findByAllProductName(String productName, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();

        if (productName != null && !productName.isBlank()) {
            builder.and(product.productName.containsIgnoreCase(productName));
        }

        List<ProductDto.Page> content = query
                .select(new QProductDto_Page(
                        product.productId.stringValue(),
                        product.productName,
                        product.standardWeight.stringValue(),
                        product.productNote,
                        productWorkGradePolicy.laborCost.coalesce(0).stringValue(),
                        productStone.stoneQuantity.multiply(stoneWorkGradePolicy.laborCost).coalesce(0).sum().stringValue(),
                        JPAExpressions
                                .select(productImage.imagePath)
                                .from(productImage)
                                .where(productImage.product.productId.eq(product.productId))
                                .limit(1)
                ))
                .from(product)
                .leftJoin(productStone).on(product.productId.eq(productStone.product.productId))
                .leftJoin(productWorkGradePolicy).on(product.productId.eq(productWorkGradePolicy.product.productId)
                        .and(productWorkGradePolicy.grade.eq(WorkGrade.GRADE_1)))
                .leftJoin(stoneWorkGradePolicy).on(productStone.stone.stoneId.eq(stoneWorkGradePolicy.stone.stoneId)
                        .and(stoneWorkGradePolicy.grade.eq(WorkGrade.GRADE_1)))
                .where(builder)
                .groupBy(product.productId, product.productName, product.standardWeight, product.productNote, productWorkGradePolicy.laborCost)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(product.createDate.desc())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(product.count())
                .from(product)
                .where(builder);

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }
}
