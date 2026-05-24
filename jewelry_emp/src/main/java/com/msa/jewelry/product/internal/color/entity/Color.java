package com.msa.jewelry.product.internal.color.entity;

import com.msa.jewelry.product.internal.color.dto.ColorDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "COLOR")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "색상 마스터 — 옐로골드/화이트골드/로즈골드 등")
public class Color {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COLOR_ID")
    @Schema(description = "색상 PK", example = "1")
    private Long colorId;
    @Column(name = "COLOR_NAME", unique = true)
    @Schema(description = "색상명 (고유)", example = "옐로골드")
    private String colorName;
    @Column(name = "COLOR_NOTE")
    @Schema(description = "색상 비고", example = "노란빛이 강한 골드")
    private String colorNote;
    @Column(name = "DEFAULT_ID")
    @Schema(description = "기본값(시스템 제공) 여부", example = "false")
    private boolean defaultId;

    @Builder
    public Color(Long colorId, String colorName, String colorNote) {
        this.colorId = colorId;
        this.colorName = colorName;
        this.colorNote = colorNote;
    }

    public void updateColor(ColorDto colorDto) {
        this.colorName = colorDto.getName();
        this.colorNote = colorDto.getNote();
    }
    public boolean isDeletable() {
        return defaultId;
    }
}
