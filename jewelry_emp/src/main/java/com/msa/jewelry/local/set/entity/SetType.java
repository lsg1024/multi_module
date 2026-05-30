package com.msa.jewelry.local.set.entity;

import com.msa.jewelry.local.set.dto.SetTypeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "SET_TYPE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "세트 타입 마스터 — 단품/세트 등 상품 구성 유형")
public class SetType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SET_TYPE_ID")
    @Schema(description = "세트 타입 PK", example = "1")
    private Long setTypeId;
    @Column(name = "SET_TYPE_NAME", unique = true)
    @Schema(description = "세트 타입명 (고유)", example = "단품")
    private String setTypeName;
    @Column(name = "SET_TYPE_NOTE")
    @Schema(description = "세트 타입 비고", example = "낱개 판매 단위")
    private String setTypeNote;
    @Column(name = "DEFAULT_ID")
    @Schema(description = "기본값(시스템 제공) 여부 — true 면 삭제 불가", example = "false")
    private boolean defaultId = false;

    @Builder
    public SetType(Long setTypeId, String setTypeName, String setTypeNote) {
        this.setTypeId = setTypeId;
        this.setTypeName = setTypeName;
        this.setTypeNote = setTypeNote;
    }

    public void updateSetType(SetTypeDto setTypeDto) {
        this.setTypeName = setTypeDto.getName();
        this.setTypeNote = setTypeDto.getNote();
    }
    public boolean isDeletable() {
        return !defaultId;
    }
}
