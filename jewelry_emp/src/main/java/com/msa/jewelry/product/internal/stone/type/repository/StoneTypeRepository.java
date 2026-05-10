package com.msa.jewelry.product.internal.stone.type.repository;

import com.msa.jewelry.product.internal.stone.type.entity.StoneType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoneTypeRepository extends JpaRepository<StoneType, Long>, CustomStoneTypeRepository {
    boolean existsByStoneTypeName(String stoneTypeName);
}
