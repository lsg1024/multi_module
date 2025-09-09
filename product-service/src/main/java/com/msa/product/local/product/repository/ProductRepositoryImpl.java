package com.msa.product.local.product.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.product.local.classification.dto.QClassificationDto_ResponseSingle;
import com.msa.product.local.grade.WorkGrade;
import com.msa.product.local.material.dto.QMaterialDto_ResponseSingle;
import com.msa.product.local.product.dto.*;
import com.msa.product.local.product.entity.QProductWorkGradePolicy;
import com.msa.product.local.set.dto.QSetTypeDto_ResponseSingle;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

import java.util.*;

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
    public CustomPage<ProductDto.Page> findByAllProductName(String productName, String factoryName, String classificationId, String setTypeId, Pageable pageable) {

        QProductWorkGradePolicy productWorkGradePolicySub = new QProductWorkGradePolicy("productWorkGradePolicySub");

        BooleanBuilder builder = new BooleanBuilder();

        if (productName != null && !productName.isBlank()) {
            builder.and(product.productName.containsIgnoreCase(productName));
        }

        if (factoryName != null && !factoryName.isBlank()) {
            builder.and(product.factoryName.containsIgnoreCase(factoryName));
        }

        if (classificationId != null && !classificationId.isBlank()) {
            builder.and(product.classification.classificationId.eq(Long.parseLong(classificationId)));
        }

        if (setTypeId != null && !setTypeId.isBlank()) {
            builder.and(product.setType.setTypeId.eq(Long.parseLong(setTypeId)));
        }

        List<ProductDto.Page> content = query
                .select(new QProductDto_Page(
                        product.productId.stringValue(),
                        product.productName,
                        product.standardWeight.stringValue(),
                        material.materialName,
                        product.productNote,
                        productWorkGradePolicyGroup.productPurchasePrice.stringValue(),
                        productWorkGradePolicy.laborCost.coalesce(0).stringValue(),
                        JPAExpressions
                                .select(productImage.imagePath)
                                .from(productImage)
                                .where(productImage.product.productId.eq(product.productId))
                                .limit(1),
                        Expressions.constant(Collections.emptyList())
                ))
                .from(product)
                .leftJoin(productStone).on(product.productId.eq(productStone.product.productId))
                .leftJoin(product.material, material)
                .leftJoin(product.setType, setType)
                .leftJoin(product.classification, classification)
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
                .leftJoin(product.productWorkGradePolicyGroups, productWorkGradePolicyGroup)
                .where(
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault.isTrue()
                                .and(builder)
                );

        if (!content.isEmpty()) {
            List<Long> productIds = content.stream()
                    .map(p -> Long.valueOf(p.getProductId()))
                    .toList();

            Map<Long, List<ProductStoneDto.PageResponse>> stonesMap = loadStonesByProductIds(productIds);

            for (ProductDto.Page dto : content) {
                Long pid = Long.valueOf(dto.getProductId());
                List<ProductStoneDto.PageResponse> stones = stonesMap.getOrDefault(pid, Collections.emptyList());
                dto.getProductStones().addAll(stones);
            }
        }

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    private Map<Long, List<ProductStoneDto.PageResponse>> loadStonesByProductIds(List<Long> productIds) {

        List<Tuple> rows = query
                .select(
                        product.productId,                        // 0
                        productStone.productStoneId,              // 1
                        stone.stoneId,                            // 2
                        stone.stoneName,                          // 3
                        productStone.stoneQuantity,
                        productStone.isMainStone,
                        productStone.isIncludeStone,
                        stoneWorkGradePolicy.stoneWorkGradePolicyId, // 7 (nullable)
                        stoneWorkGradePolicy.grade,               // 8 (nullable)
                        stoneWorkGradePolicy.laborCost            // 9 (nullable)
                )
                .from(productStone)
                .join(productStone.product, product)
                .join(productStone.stone, stone)
                .leftJoin(stone.gradePolicies, stoneWorkGradePolicy).on(stoneWorkGradePolicy.grade.eq(WorkGrade.GRADE_1))
                .where(
                        product.productId.in(productIds)
                        .and(productStone.isIncludeStone.isTrue()))
                .orderBy(product.productId.asc(), productStone.productStoneId.asc())
                .fetch();

        Map<Long, List<ProductStoneDto.PageResponse>> result = new LinkedHashMap<>();

        for (Tuple t : rows) {
            Long productId   = t.get(product.productId);
            Long productStoneId  = t.get(productStone.productStoneId);
            Long stoneId   = t.get(stone.stoneId);
            String stoneName = t.get(stone.stoneName);
            Integer quantity  = Optional.ofNullable(t.get(productStone.stoneQuantity)).orElse(0);
            boolean main    = Optional.ofNullable(t.get(productStone.isMainStone)).orElse(false);
            boolean include = Optional.ofNullable(t.get(productStone.isIncludeStone)).orElse(false);
            Long    policyId = t.get(stoneWorkGradePolicy.stoneWorkGradePolicyId);
            Integer cost  = t.get(stoneWorkGradePolicy.laborCost);

            ProductStoneDto.PageResponse resp = new ProductStoneDto.PageResponse(
                    productStoneId != null ? productStoneId.toString() : null,
                    stoneId  != null ? stoneId.toString()  : null,
                    stoneName != null ? stoneName : "",
                    main, include, quantity, cost
            );

            result.computeIfAbsent(productId, k -> new ArrayList<>()).add(resp);
        }

        return result;
    }

    @Override
    public ProductDetailDto findProductDetail(Long productId, WorkGrade grade) {
        ProductDetailDto result = query
                .select(new QProductDetailDto(
                        product.productId,
                        product.productName,
                        material.materialName,
                        color.colorName,
                        productWorkGradePolicyGroup.productPurchasePrice,
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
                        productStone.isMainStone,
                        productStone.isIncludeStone,
                        productStone.productStoneNote
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
