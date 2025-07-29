package com.msa.product.local.stone.type.repository;

import com.msa.product.local.stone.type.entity.StoneType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoneTypeRepository extends JpaRepository<StoneType, Long>, CustomStoneTypeRepository {
    boolean existsByStoneTypeName(String stoneTypeName);
}
