package com.msa.product.local.classification.entity;

import com.msa.product.local.classification.dto.ClassificationDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CLASSIFICATION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Classification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CLASSIFICATION_ID")
    private Long classificationId;
    @Column(name = "CLASSIFICATION_NAME", unique = true)
    private String classificationName;
    @Column(name = "CLASSIFICATION_NOTE")
    private String classificationNote;

    @Builder
    public Classification(String classificationName, String classificationNote) {
        this.classificationName = classificationName;
        this.classificationNote = classificationNote;
    }

    public String getClassificationName() {
        return classificationName;
    }

    public String getClassificationNote() {
        return classificationNote;
    }

    public void updateClassification(ClassificationDto classificationDto) {
        this.classificationName = classificationDto.getName();
        this.classificationNote = classificationDto.getNote();
    }
}
