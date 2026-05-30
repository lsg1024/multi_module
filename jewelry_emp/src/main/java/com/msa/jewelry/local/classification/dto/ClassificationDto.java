package com.msa.jewelry.local.classification.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상품 분류 요청 DTO — 생성/수정 시 사용")
public class ClassificationDto {

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    @Schema(description = "분류명", example = "반지")
    private String name;
    @Schema(description = "분류 비고", example = "결혼·약혼·패션")
    private String note;

    @Getter
    @NoArgsConstructor
    @Schema(description = "분류 단건 응답 DTO")
    public static class ResponseSingle {
        @Schema(description = "분류 ID", example = "1")
        private String classificationId;
        @Schema(description = "분류명", example = "반지")
        private String classificationName;
        @Schema(description = "분류 비고", example = "결혼·약혼·패션")
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
