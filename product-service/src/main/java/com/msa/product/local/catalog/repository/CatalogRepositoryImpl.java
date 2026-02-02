package com.msa.product.local.catalog.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.product.local.catalog.dto.CatalogProductDto;
import com.msa.product.local.catalog.dto.CatalogStoneDto;
import com.msa.product.local.classification.dto.QClassificationDto_ResponseSingle;
import com.msa.product.local.material.dto.QMaterialDto_ResponseSingle;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.dto.QProductImageDto_Response;
import com.msa.product.local.set.dto.QSetTypeDto_ResponseSingle;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.msa.product.local.classification.entity.QClassification.classification;
import static com.msa.product.local.color.entity.QColor.color;
import static com.msa.product.local.material.entity.QMaterial.material;
import static com.msa.product.local.product.entity.QProduct.product;
import static com.msa.product.local.product.entity.QProductImage.productImage;
import static com.msa.product.local.product.entity.QProductStone.productStone;
import static com.msa.product.local.product.entity.QProductWorkGradePolicyGroup.productWorkGradePolicyGroup;
import static com.msa.product.local.set.entity.QSetType.setType;
import static com.msa.product.local.stone.stone.entity.QStone.stone;

@Repository
public class CatalogRepositoryImpl implements CatalogRepository {

    private final JPAQueryFactory query;

    public CatalogRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public CustomPage<CatalogProductDto.Page> findCatalogProducts(
            String productName, String classificationId,
            String setTypeId, String sortField, String sort, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();

        if (productName != null && !productName.isBlank()) {
            builder.and(product.productName.containsIgnoreCase(productName));
        }

        if (classificationId != null && !classificationId.isBlank()) {
            builder.and(product.classification.classificationId.eq(Long.parseLong(classificationId)));
        }

        if (setTypeId != null && !setTypeId.isBlank()) {
            builder.and(product.setType.setTypeId.eq(Long.parseLong(setTypeId)));
        }

        QProductImageDto_Response image = new QProductImageDto_Response(
                productImage.imageId.max().stringValue(),
                productImage.imagePath.max()
        );

        OrderSpecifier<?>[] orderSpecifiers = createOrderSpecifiers(sortField, sort);

        // 가격, 제조사 정보 없이 조회
        List<CatalogProductDto.Page> content = query
                .select(Projections.constructor(CatalogProductDto.Page.class,
                        product.productId.stringValue(),
                        product.productName,
                        product.standardWeight.stringValue(),
                        product.material.materialName,
                        color.colorName,
                        product.productNote,
                        image
                ))
                .from(product)
                .leftJoin(productImage).on(productImage.product.eq(product).and(productImage.imageMain.isTrue()))
                .leftJoin(product.material, material)
                .leftJoin(product.setType, setType)
                .leftJoin(product.classification, classification)
                .leftJoin(product.productWorkGradePolicyGroups, productWorkGradePolicyGroup)
                .on(productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault.isTrue())
                .leftJoin(productWorkGradePolicyGroup.color, color)
                .where(
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault.isTrue()
                                .and(builder)
                )
                .groupBy(
                        product.productId,
                        product.productName,
                        product.standardWeight,
                        material.materialName,
                        product.productNote,
                        color.colorName
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
                .leftJoin(productWorkGradePolicyGroup.color, color)
                .where(
                        productWorkGradePolicyGroup.productWorkGradePolicyGroupDefault.isTrue()
                                .and(builder)
                );

        if (!content.isEmpty()) {
            List<Long> productIds = content.stream()
                    .map(p -> Long.valueOf(p.getProductId()))
                    .toList();

            Map<Long, List<CatalogStoneDto.PageResponse>> stonesMap = loadStonesByProductIds(productIds);

            for (CatalogProductDto.Page dto : content) {
                Long pid = Long.valueOf(dto.getProductId());
                List<CatalogStoneDto.PageResponse> stones = stonesMap.getOrDefault(pid, Collections.emptyList());
                dto.getProductStones().addAll(stones);
            }
        }

        return new CustomPage<>(content, pageable, countQuery.fetchOne());
    }

    /**
     * 스톤 정보 조회 (가격 정보 제외)
     */
    private Map<Long, List<CatalogStoneDto.PageResponse>> loadStonesByProductIds(List<Long> productIds) {

        List<Tuple> rows = query
                .select(
                        product.productId,
                        productStone.productStoneId,
                        stone.stoneId,
                        stone.stoneName,
                        productStone.stoneQuantity,
                        productStone.mainStone,
                        productStone.includeStone
                )
                .from(productStone)
                .join(productStone.product, product)
                .join(productStone.stone, stone)
                .where(
                        product.productId.in(productIds)
                                .and(productStone.includeStone.isTrue()))
                .orderBy(product.productId.asc(), productStone.productStoneId.asc())
                .fetch();

        Map<Long, List<CatalogStoneDto.PageResponse>> result = new LinkedHashMap<>();

        for (Tuple t : rows) {
            Long productId = t.get(product.productId);
            Long productStoneId = t.get(productStone.productStoneId);
            Long stoneId = t.get(stone.stoneId);
            String stoneName = t.get(stone.stoneName);
            Integer quantity = Optional.ofNullable(t.get(productStone.stoneQuantity)).orElse(0);
            boolean main = Optional.ofNullable(t.get(productStone.mainStone)).orElse(false);
            boolean include = Optional.ofNullable(t.get(productStone.includeStone)).orElse(false);

            CatalogStoneDto.PageResponse resp = CatalogStoneDto.PageResponse.builder()
                    .productStoneId(productStoneId != null ? productStoneId.toString() : null)
                    .stoneId(stoneId != null ? stoneId.toString() : null)
                    .stoneName(stoneName != null ? stoneName : "")
                    .mainStone(main)
                    .includeStone(include)
                    .stoneQuantity(quantity)
                    .build();

            result.computeIfAbsent(productId, k -> new ArrayList<>()).add(resp);
        }

        return result;
    }

    @Override
    public CatalogProductDto.Detail findCatalogProductDetail(Long productId) {
        CatalogProductDto.Detail detail = query
                .select(Projections.constructor(CatalogProductDto.Detail.class,
                        product.productId.stringValue(),
                        product.productName,
                        product.standardWeight.stringValue(),
                        product.productRelatedNumber,
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
                        )
                ))
                .from(product)
                .leftJoin(product.setType, setType)
                .leftJoin(product.classification, classification)
                .leftJoin(product.material, material)
                .where(product.productId.eq(productId))
                .fetchOne();

        if (detail != null) {
            // 스톤 정보 별도 조회 (가격 정보 제외)
            List<CatalogStoneDto.Response> stones = query
                    .select(Projections.constructor(CatalogStoneDto.Response.class,
                            productStone.productStoneId.stringValue(),
                            stone.stoneId.stringValue(),
                            stone.stoneName,
                            stone.stoneWeight,
                            productStone.mainStone,
                            productStone.includeStone,
                            productStone.stoneQuantity,
                            productStone.productStoneNote
                    ))
                    .from(productStone)
                    .join(productStone.stone, stone)
                    .where(productStone.product.productId.eq(productId))
                    .fetch();
            detail.setProductStoneDtos(stones);

            // 이미지 정보 조회
            List<ProductImageDto.Response> images = query
                    .select(new QProductImageDto_Response(
                            productImage.imageId.stringValue(),
                            productImage.imagePath,
                            productImage.imageName,
                            productImage.imageOriginName,
                            productImage.imageMain
                    ))
                    .from(productImage)
                    .where(productImage.product.productId.eq(productId))
                    .fetch();
            detail.setProductImageDtos(images);
        }

        return detail;
    }

    @Override
    public List<CatalogProductDto.RelatedProduct> findRelatedProducts(Long productId, String relatedNumber) {
        if (relatedNumber == null || relatedNumber.isBlank()) {
            return Collections.emptyList();
        }

        return query
                .select(Projections.constructor(CatalogProductDto.RelatedProduct.class,
                        product.productId,
                        product.productName,
                        productImage.imagePath.coalesce("")
                ))
                .from(product)
                .leftJoin(productImage).on(
                        productImage.product.eq(product)
                                .and(productImage.imageMain.isTrue())
                )
                .where(
                        product.productRelatedNumber.eq(relatedNumber),
                        product.productId.ne(productId),
                        product.productDeleted.isFalse()
                )
                .orderBy(product.productName.asc())
                .fetch();
    }

    private OrderSpecifier<?>[] createOrderSpecifiers(String sortField, String sort) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (sort != null && StringUtils.hasText(sortField)) {
            Order direction = "ASC".equalsIgnoreCase(sort) ? Order.ASC : Order.DESC;

            switch (sortField) {
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
