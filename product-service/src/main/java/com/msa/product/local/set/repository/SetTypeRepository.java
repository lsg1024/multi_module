package com.msa.product.local.set.repository;

import com.msa.product.local.set.entity.SetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetTypeRepository extends JpaRepository<SetType, Long>, CustomSetTypeRepository {
    boolean existsBySetTypeName(String setTypeName);
}
