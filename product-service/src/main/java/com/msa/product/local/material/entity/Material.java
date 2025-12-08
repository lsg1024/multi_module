package com.msa.product.local.material.entity;

import com.msa.product.local.material.dto.MaterialDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
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
    @Column(name = "DEFAULT_ID")
    private boolean defaultId;

    @Builder
    public Material(Long materialId, String materialName, BigDecimal materialGoldPurityPercent) {
        this.materialId = materialId;
        this.materialName = materialName;
        this.materialGoldPurityPercent = materialGoldPurityPercent;
    }

    public void updateMaterial(MaterialDto materialDto) {
        this.materialName = materialDto.getName();
        this.materialGoldPurityPercent = new BigDecimal(materialDto.getGoldPurityPercent());
    }

    public boolean isDeletable() {
        return defaultId;
    }
}
