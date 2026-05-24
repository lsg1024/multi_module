package com.msa.jewelry.local.material.entity;

import com.msa.jewelry.local.material.dto.MaterialDto;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "재질 마스터 — 14K/18K/24K/PT900 등 금속 종류")
public class Material {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MATERIAL_ID")
    @Schema(description = "재질 PK", example = "1")
    private Long materialId;
    @Column(name = "MATERIAL_NAME", unique = true)
    @Schema(description = "재질명 (고유)", example = "18K")
    private String materialName;
    @Column(name = "MATERIAL_GOLD_PURITY_PERCENT", precision = 5, scale = 2)
    @Schema(description = "금 함량 퍼센트 (18K = 75.00, 24K = 99.99)", example = "75.00")
    private BigDecimal materialGoldPurityPercent;
    @Column(name = "DEFAULT_ID")
    @Schema(description = "기본값(시스템 제공) 여부", example = "false")
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
