package com.msa.product.local.color.repository;

import com.msa.product.local.color.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Long>, CustomColorRepository {
    boolean existsByColorName(String colorName);
}
