package com.msa.product.local.product.repository;

import com.msa.product.local.classification.dto.QClassificationDto_ResponseSingle;
import com.msa.product.local.material.dto.QMaterialDto_ResponseSingle;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.dto.QProductDto_Detail;
import com.msa.product.local.set.dto.QSetTypeDto_ResponseSingle;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.Collections;

import static com.msa.product.local.classification.entity.QClassification.classification;
import static com.msa.product.local.material.entity.QMaterial.material;
import static com.msa.product.local.product.entity.QProduct.product;
import static com.msa.product.local.set.entity.QSetType.setType;

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
}
