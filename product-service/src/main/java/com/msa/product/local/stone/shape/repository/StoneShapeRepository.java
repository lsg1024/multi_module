package com.msa.product.local.stone.shape.repository;

import com.msa.product.local.stone.shape.entity.StoneShape;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoneShapeRepository extends JpaRepository<StoneShape, Long>, CustomStoneShapeRepository {
    boolean existsByStoneShapeName(String stoneName);
}
