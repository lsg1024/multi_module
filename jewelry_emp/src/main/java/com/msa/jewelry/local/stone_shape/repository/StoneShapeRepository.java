package com.msa.jewelry.local.stone_shape.repository;

import com.msa.jewelry.local.stone_shape.entity.StoneShape;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoneShapeRepository extends JpaRepository<StoneShape, Long>, CustomStoneShapeRepository {
    boolean existsByStoneShapeName(String stoneName);
}
