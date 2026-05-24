package com.msa.jewelry.product.internal.classification.entity;

import com.msa.jewelry.product.internal.classification.dto.ClassificationDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "CLASSIFICATION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "상품 분류 마스터 — 반지/목걸이/팔찌 등의 분류 카테고리")
public class Classification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CLASSIFICATION_ID")
    @Schema(description = "분류 PK", example = "1")
    private Long classificationId;
    @Column(name = "CLASSIFICATION_NAME", unique = true)
    @Schema(description = "분류명 (고유)", example = "반지")
    private String classificationName;
    @Column(name = "CLASSIFICATION_NOTE")
    @Schema(description = "분류 비고", example = "결혼·약혼·패션")
    private String classificationNote;
    @Column(name = "DEFAULT_ID")
    @Schema(description = "기본값(시스템 제공) 여부 — true 면 삭제 불가", example = "false")
    private boolean defaultId;

    @Builder
    public Classification(Long classificationId, String classificationName, String classificationNote) {
        this.classificationId = classificationId;
        this.classificationName = classificationName;
        this.classificationNote = classificationNote;
    }

    public void updateClassification(ClassificationDto classificationDto) {
        this.classificationName = classificationDto.getName();
        this.classificationNote = classificationDto.getNote();
    }
    public boolean isDeletable() {
        return !defaultId;
    }
}
