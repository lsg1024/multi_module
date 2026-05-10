package com.msa.jewelry.product.internal.stone.shape.repository;

import com.msa.jewelry.product.internal.stone.shape.entity.StoneShape;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoneShapeRepository extends JpaRepository<StoneShape, Long>, CustomStoneShapeRepository {
    boolean existsByStoneShapeName(String stoneName);
}
