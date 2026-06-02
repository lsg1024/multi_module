package com.msa.jewelry.local.stone_shape.entity;

import com.msa.jewelry.local.stone_shape.dto.StoneShapeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "STONE_SHAPE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "스톤 모양 마스터 — 원형/사각형/하트 등 보석 컷팅 모양")
public class StoneShape {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STONE_SHAPE_ID")
    @Schema(description = "스톤 모양 PK", example = "1")
    private Long stoneShapeId;
    @Column(name = "STONE_SHAPE_NAME", unique = true)
    @Schema(description = "스톤 모양명 (고유)", example = "라운드")
    private String stoneShapeName;
    @Column(name = "STONE_SHAPE_NOTE")
    @Schema(description = "스톤 모양 비고", example = "원형 브릴리언트 컷")
    private String stoneShapeNote;
    @Column(name = "STONE_SHAPE_DEFAULT", nullable = false)
    @Schema(description = "기본(보호) 마스터 여부 — true 면 삭제 불가", example = "false")
    private boolean stoneShapeDefault = false;

    @Builder
    public StoneShape(String stoneShapeName, String stoneShapeNote) {
        this.stoneShapeName = stoneShapeName;
        this.stoneShapeNote = stoneShapeNote;
    }

    public String getStoneShapeName() {
        return stoneShapeName;
    }

    public String getStoneShapeNote() {
        return stoneShapeNote;
    }

    public boolean isStoneShapeDefault() {
        return stoneShapeDefault;
    }

    public void updateStoneShape(StoneShapeDto stoneShapeDto) {
        this.stoneShapeName = stoneShapeDto.getStoneShapeName();
        this.stoneShapeNote = stoneShapeDto.getStoneShapeNote();
    }
}
