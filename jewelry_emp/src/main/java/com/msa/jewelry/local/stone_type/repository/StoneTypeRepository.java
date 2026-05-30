package com.msa.jewelry.local.stone_type.repository;

import com.msa.jewelry.local.stone_type.entity.StoneType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoneTypeRepository extends JpaRepository<StoneType, Long>, CustomStoneTypeRepository {
    boolean existsByStoneTypeName(String stoneTypeName);
}
