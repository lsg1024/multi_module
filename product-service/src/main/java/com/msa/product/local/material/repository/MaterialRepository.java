package com.msa.product.local.material.repository;

import com.msa.product.local.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long>, CustomMaterialRepository {
    boolean existsByMaterialName(String name);
}
