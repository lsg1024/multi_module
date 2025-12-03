package com.msa.product.local.color.repository;

import com.msa.product.local.color.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Long>, CustomColorRepository {
    @Query("select c.colorName from Color c where c.colorId= :id")
    String findByColorName(Long id);
    boolean existsByColorName(String colorName);
    Optional<Color> findByColorName(String colorName);
}
