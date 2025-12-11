package com.msa.product.local.product.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.product.local.classification.dto.QClassificationDto_ResponseSingle;
import com.msa.product.local.grade.WorkGrade;
import com.msa.product.local.material.dto.QMaterialDto_ResponseSingle;
import com.msa.product.local.product.dto.*;
import com.msa.product.local.set.dto.QSetTypeDto_ResponseSingle;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

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
                .leftJoin(product.setType, setType)
                .leftJoin(product.classification, classification)
                .leftJoin(product.material, material)
                .where(product.productId.eq(productId))
                .fetchOne();
    }

    @Override
    public CustomPage<ProductDto.Page> findByAllProductName(String productName, String factoryName, String classificationId, String setTypeId, String level,String sortField, String sort,  Pageable pageable) {

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

        WorkGrade grade;
        if (!StringUtils.hasText(level)) {
            grade = WorkGrade.GRADE_1;
        } else {
            grade = WorkGrade.fromLevel(level);
        }

        QProductImageDto_Response image = new QProductImageDto_Response(
                productImage.imageId.max().stringValue(),
                productImage.imagePath.max()
        );

        OrderSpecifier<?>[] orderSpecifiers = createOrderSpecifiers(sortField, sort);

        List<ProductDto.Page> content = query
                .select(new QProductDto_Page(
                        product.productId.stringValue(),
                        product.productName,
                        product.productFactoryName,
                        product.standardWeight.stringValue(),
                        material.materialName,
                        product.productNote,
                        productWorkGradePolicyGroup.productPurchasePrice.stringValue(),
                        productWorkGradePolicy.laborCost.coalesce(0).stringValue(),
                        product.factoryId.stringValue(),
                        product.factoryName,
                        image
                ))
                .from(product)
                .leftJoin(productImage).on(productImage.product.eq(product).and(productImage.imageMain.isTrue()))
                .leftJoin(product.material, material)
                .leftJoin(product.setType, setType)
                .leftJoin(product.classification, classification)
                .leftJoin(product.productWorkGradePolicyGroups, productWorkGradePolicyGroup)
                .leftJoin(productWorkGradePolicyGroup.gradePolicies, productWorkGradePolicy)
                .on(productWorkGradePolicy.grade.eq(grade))
                .where(
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault.isTrue()
                                .and(builder)
                )
                .groupBy(
                        product.productId,
                        product.productName,
                        product.productFactoryName,
                        product.standardWeight,
                        material.materialName,
                        product.productNote,
                        productWorkGradePolicyGroup.productPurchasePrice,
                        productWorkGradePolicy.laborCost,
                        product.factoryId,
                        product.factoryName
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers)
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(product.countDistinct())
                .from(product)
                .leftJoin(product.material, material)
                .leftJoin(product.setType, setType)
                .leftJoin(product.classification, classification)
                .leftJoin(product.productWorkGradePolicyGroups, productWorkGradePolicyGroup)
                .where(
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault.isTrue()
                                .and(builder)
                );

        if (!content.isEmpty()) {
            List<Long> productIds = content.stream()
                    .map(p -> Long.valueOf(p.getProductId()))
                    .toList();

            Map<Long, List<ProductStoneDto.PageResponse>> stonesMap = loadStonesByProductIds(productIds, grade);

            for (ProductDto.Page dto : content) {
                Long pid = Long.valueOf(dto.getProductId());
                List<ProductStoneDto.PageResponse> stones = stonesMap.getOrDefault(pid, Collections.emptyList());
                dto.getProductStones().addAll(stones);
            }
        }

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    private Map<Long, List<ProductStoneDto.PageResponse>> loadStonesByProductIds(List<Long> productIds, WorkGrade grade) {

        List<Tuple> rows = query
                .select(
                        product.productId,
                        productStone.productStoneId,
                        stone.stoneId,
                        stone.stoneName,
                        productStone.stoneQuantity,
                        productStone.mainStone,
                        productStone.includeStone,
                        stone.stonePurchasePrice,
                        stoneWorkGradePolicy.stoneWorkGradePolicyId,
                        stoneWorkGradePolicy.grade,
                        stoneWorkGradePolicy.laborCost
                )
                .from(productStone)
                .join(productStone.product, product)
                .join(productStone.stone, stone)
                .leftJoin(stone.gradePolicies, stoneWorkGradePolicy).on(stoneWorkGradePolicy.grade.eq(grade))
                .where(
                        product.productId.in(productIds)
                        .and(productStone.includeStone.isTrue()))
                .orderBy(product.productId.asc(), productStone.productStoneId.asc())
                .fetch();

        Map<Long, List<ProductStoneDto.PageResponse>> result = new LinkedHashMap<>();

        for (Tuple t : rows) {
            Long productId = t.get(product.productId);
            Long productStoneId = t.get(productStone.productStoneId);
            Long stoneId = t.get(stone.stoneId);
            String stoneName = t.get(stone.stoneName);
            Integer quantity = Optional.ofNullable(t.get(productStone.stoneQuantity)).orElse(0);
            boolean main = Optional.ofNullable(t.get(productStone.mainStone)).orElse(false);
            boolean include = Optional.ofNullable(t.get(productStone.includeStone)).orElse(false);
            Integer cost = t.get(stoneWorkGradePolicy.laborCost);
            Integer purchasePrice = t.get(stone.stonePurchasePrice);

            ProductStoneDto.PageResponse resp = new ProductStoneDto.PageResponse(
                    productStoneId != null ? productStoneId.toString() : null,
                    stoneId != null ? stoneId.toString() : null,
                    stoneName != null ? stoneName : "",
                    main, include, quantity, cost,
                    purchasePrice);

            result.computeIfAbsent(productId, k -> new ArrayList<>()).add(resp);
        }

        return result;
    }
    @Override
    public ProductDetailDto findProductDetail(Long productId, WorkGrade grade) {

        List<WorkGrade> candidateGrades = Arrays.stream(WorkGrade.values())
                .filter(g -> g.ordinal() <= grade.ordinal())
                .toList();

        return query
                .select(new QProductDetailDto(
                        product.productId,
                        product.productName,
                        product.productFactoryName,
                        classification.classificationId,
                        classification.classificationName,
                        setType.setTypeId,
                        setType.setTypeName,
                        productWorkGradePolicyGroup.productPurchasePrice,
                        productWorkGradePolicy.laborCost
                ))
                .from(product)
                .leftJoin(product.classification, classification)
                .leftJoin(product.setType, setType)
                .leftJoin(product.productWorkGradePolicyGroups, productWorkGradePolicyGroup)
                .leftJoin(productWorkGradePolicyGroup.color, color)
                .leftJoin(productWorkGradePolicyGroup.gradePolicies, productWorkGradePolicy)
                .where(
                        product.productId.eq(productId),
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault.isTrue(),
                        productWorkGradePolicy.grade.in(candidateGrades)
                )
                .orderBy(productWorkGradePolicy.grade.desc())
                .limit(1)
                .fetchOne();

    }

    private OrderSpecifier<?>[] createOrderSpecifiers(String sortField, String sort) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (sort != null && StringUtils.hasText(sortField)) {
            Order direction = "ASC".equalsIgnoreCase(sort) ? Order.ASC : Order.DESC;

            switch (sortField) {
                case "factory" -> orderSpecifiers.add(new OrderSpecifier<>(direction, product.factoryName));
                case "setType" -> orderSpecifiers.add(new OrderSpecifier<>(direction, product.setType.setTypeName));
                case "classification" -> orderSpecifiers.add(new OrderSpecifier<>(direction, product.classification.classificationName));
                case "productName" -> orderSpecifiers.add(new OrderSpecifier<>(direction, product.productName));

                default -> {
                    orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, product.createDate));
                    orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, product.productId));
                }
            }
        } else {
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, product.createDate));
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, product.productId));
        }
        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }
}
