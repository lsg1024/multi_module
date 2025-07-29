package com.msa.product.local.material.repository;

import com.msa.product.local.material.dto.MaterialDto;
import com.msa.product.local.material.dto.QMaterialDto_ResponseSingle;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.msa.product.local.material.entity.QMaterial.material;

public class MaterialRepositoryImpl implements CustomMaterialRepository{

    private final JPAQueryFactory query;

    public MaterialRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }

    @Override
    public List<MaterialDto.ResponseSingle> findAllOrderByAsc() {
        return query
                .select(new QMaterialDto_ResponseSingle(
                        material.materialId.stringValue(),
                        material.materialName,
                        material.materialGoldPurityPercent.stringValue()
                ))
                .from(material)
                .orderBy(material.materialName.asc())
                .fetch();
    }
}
