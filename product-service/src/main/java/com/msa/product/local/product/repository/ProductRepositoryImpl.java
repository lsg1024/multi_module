package com.msa.product.local.product.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.product.local.classification.dto.QClassificationDto_ResponseSingle;
import com.msa.product.local.grade.WorkGrade;
import com.msa.product.local.material.dto.QMaterialDto_ResponseSingle;
import com.msa.product.local.product.dto.*;
import com.msa.product.local.product.entity.QProductWorkGradePolicy;
import com.msa.product.local.set.dto.QSetTypeDto_ResponseSingle;
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
import static com.msa.product.local.color.entity.QColor.color;
import static com.msa.product.local.material.entity.QMaterial.material;
import static com.msa.product.local.product.entity.QProduct.product;
import static com.msa.product.local.product.entity.QProductImage.productImage;
import static com.msa.product.local.product.entity.QProductStone.productStone;
import static com.msa.product.local.product.entity.QProductWorkGradePolicy.productWorkGradePolicy;
import static com.msa.product.local.product.entity.QProductWorkGradePolicyGroup.productWorkGradePolicyGroup;
import static com.msa.product.local.set.entity.QSetType.setType;
import static com.msa.product.local.stone.stone.entity.QStone.stone;
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

        QProductWorkGradePolicy productWorkGradePolicySub = new QProductWorkGradePolicy("productWorkGradePolicySub");

        BooleanBuilder builder = new BooleanBuilder();

        if (productName != null && !productName.isBlank()) {
            builder.and(product.productName.containsIgnoreCase(productName));
        }

        List<ProductDto.Page> content = query
                .select(new QProductDto_Page(
                        product.productId.stringValue(),
                        product.productName,
                        product.standardWeight.stringValue(),
                        productWorkGradePolicyGroup.color.colorName,
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
                .leftJoin(product.productWorkGradePolicyGroups, productWorkGradePolicyGroup)
                .leftJoin(productWorkGradePolicyGroup.gradePolicies, productWorkGradePolicy)
                .on(productWorkGradePolicy.grade.eq(WorkGrade.GRADE_1)
                        .and(productWorkGradePolicy.productWorkGradePolicyId.eq(
                                JPAExpressions
                                        .select(productWorkGradePolicySub.productWorkGradePolicyId.min())
                                        .from(productWorkGradePolicySub)
                                        .where(productWorkGradePolicySub.workGradePolicyGroup.product.eq(product))
                                        .where(productWorkGradePolicySub.grade.eq(WorkGrade.GRADE_1))
                        )))
                .leftJoin(stoneWorkGradePolicy).on(productStone.stone.stoneId.eq(stoneWorkGradePolicy.stone.stoneId)
                        .and(stoneWorkGradePolicy.grade.eq(WorkGrade.GRADE_1)))
                .where(
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault.isTrue()
                        .and(builder)
                )
                .groupBy(
                        product.productId,
                        product.productName,
                        product.standardWeight,
                        product.productNote,
                        productWorkGradePolicy.laborCost
                )
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

    @Override
    public ProductDetailDto findProductDetail(Long productId, WorkGrade grade) {
        ProductDetailDto result = query
                .select(new QProductDetailDto(
                        product.productId,
                        product.productName,
                        material.materialName,
                        color.colorName,
                        productWorkGradePolicy.laborCost
                ))
                .from(product)
                .join(product.material, material)
                .join(product.productWorkGradePolicyGroups, productWorkGradePolicyGroup)
                .join(productWorkGradePolicyGroup.color, color)
                .join(productWorkGradePolicyGroup.gradePolicies, productWorkGradePolicy)
                .where(
                        product.productId.eq(productId),
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault.isTrue(),
                        productWorkGradePolicy.grade.eq(grade)
                )
                .fetchOne();

        List<ProductDetailDto.StoneInfo> stoneCosts = query
                .select(new QProductDetailDto_StoneInfo(
                        stone.stoneId.stringValue(),
                        stone.stoneName,
                        stone.stoneWeight.stringValue(),
                        stone.stonePurchasePrice,
                        stoneWorkGradePolicy.laborCost,
                        productStone.stoneQuantity,
                        productStone.productStoneMain,
                        productStone.includeQuantity,
                        productStone.includeWeight,
                        productStone.includeLabor
                ))
                .from(productStone)
                .join(productStone.stone, stone)
                .join(stone.gradePolicies, stoneWorkGradePolicy)
                .where(
                        productStone.product.productId.eq(productId),
                        stoneWorkGradePolicy.grade.eq(grade)
                )
                .fetch();

        result.setStoneInfos(stoneCosts);

        return result;
    }
}
