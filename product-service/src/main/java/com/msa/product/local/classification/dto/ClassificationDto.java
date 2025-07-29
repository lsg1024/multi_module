package com.msa.product.local.classification.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationDto {

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;
    private String note;

    @Getter
    @NoArgsConstructor
    public static class ResponseSingle {
        private String classificationId;
        private String classificationName;
        private String classificationNote;

        @Builder
        @QueryProjection
        public ResponseSingle(String classificationId, String classificationName, String classificationNote) {
            this.classificationId = classificationId;
            this.classificationName = classificationName;
            this.classificationNote = classificationNote;
        }
    }
}
