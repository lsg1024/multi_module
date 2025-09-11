package com.msa.product.local.set.repository;

import com.msa.product.local.set.entity.SetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SetTypeRepository extends JpaRepository<SetType, Long>, CustomSetTypeRepository {
    boolean existsBySetTypeName(String setTypeName);
    @Query("select s.setTypeName from SetType s where s.setTypeId= :id")
    String findByMaterialName(Long id);
}
