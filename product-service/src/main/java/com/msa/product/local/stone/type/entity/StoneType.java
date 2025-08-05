package com.msa.product.local.stone.type.entity;

import com.msa.product.local.stone.type.dto.StoneTypeDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "STONE_TYPE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoneType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STONE_TYPE_ID")
    private Long stoneTypeId;
    @Column(name = "STONE_TYPE_NAME", unique = true)
    private String stoneTypeName;
    @Column(name = "STONE_TYPE_NOTE")
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

