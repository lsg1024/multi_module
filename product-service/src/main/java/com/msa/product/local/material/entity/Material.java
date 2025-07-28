package com.msa.product.local.material.entity;

import com.msa.product.local.material.dto.MaterialDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Table(name = "MATERIAL")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Material {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MATERIAL_ID")
    private Long materialId;
    @Column(name = "MATERIAL_NAME", unique = true)
    private String materialName;
    @Column(name = "MATERIAL_GOLD_PURITY_PERCENT", precision = 5, scale = 2)
    private BigDecimal materialGoldPurityPercent;

    @Builder
    public Material(String materialName, BigDecimal materialGoldPurityPercent) {
        this.materialName = materialName;
        this.materialGoldPurityPercent = materialGoldPurityPercent;
    }

    public String getMaterialName() {
        return materialName;
    }

    public BigDecimal getMaterialGoldPurityPercent() {
        return materialGoldPurityPercent;
    }

    public void updateMaterial(MaterialDto materialDto) {
        this.materialName = materialDto.getName();
        this.materialGoldPurityPercent = new BigDecimal(materialDto.getGoldPurityPercent());
    }
}
