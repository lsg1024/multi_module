package com.msa.product.local.stone.shape.entity;

import com.msa.product.local.stone.shape.dto.StoneShapeDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "STONE_SHAPE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoneShape {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STONE_SHAPE_ID")
    private Long stoneShapeId;
    @Column(name = "STONE_SHAPE_NAME")
    private String stoneShapeName;
    @Column(name = "STONE_SHAPE_NOTE")
    private String stoneShapeNote;

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

    public void updateStoneShape(StoneShapeDto stoneShapeDto) {
        this.stoneShapeName = stoneShapeDto.getName();
        this.stoneShapeNote = stoneShapeDto.getNote();
    }
}
