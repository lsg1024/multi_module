package com.msa.jewelry.local.stone_type.entity;

import com.msa.jewelry.local.stone_type.dto.StoneTypeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "STONE_TYPE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "스톤 타입 마스터 — 다이아몬드/사파이어/루비/큐빅 등 보석 종류")
public class StoneType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STONE_TYPE_ID")
    @Schema(description = "스톤 타입 PK", example = "1")
    private Long stoneTypeId;
    @Column(name = "STONE_TYPE_NAME", unique = true)
    @Schema(description = "스톤 타입명 (고유)", example = "다이아몬드")
    private String stoneTypeName;
    @Column(name = "STONE_TYPE_NOTE")
    @Schema(description = "스톤 타입 비고", example = "천연 다이아몬드")
    private String stoneTypeNote;

    @Builder
    public StoneType(String stoneTypeName, String stoneTypeNote) {
        this.stoneTypeName = stoneTypeName;
        this.stoneTypeNote = stoneTypeNote;
    }

    public String getStoneTypeName() {
        return stoneTypeName;
    }

    public String getStoneTypeNote() {
        return stoneTypeNote;
    }

    public void updateStoneType(StoneTypeDto stoneTypeDto) {
        this.stoneTypeName = stoneTypeDto.getName();
        this.stoneTypeNote = stoneTypeDto.getNote();
    }
}

