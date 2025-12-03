package com.msa.product.local.material.repository;

import com.msa.product.local.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long>, CustomMaterialRepository {
    @Query("select m.materialName from Material m where m.materialId= :id")
    String findByMaterialName(Long id);
    boolean existsByMaterialName(String name);
    Optional<Material> findByMaterialName(String materialName);
}
