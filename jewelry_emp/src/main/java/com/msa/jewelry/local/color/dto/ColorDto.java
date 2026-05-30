package com.msa.jewelry.local.color.dto;

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
@Schema(description = "색상 요청 DTO — 색상 마스터 생성/수정")
public class ColorDto {
    @NotBlank(message = "이름은 필수 입력 값입니다.")
    @Schema(description = "색상명", example = "옐로골드")
    private String name;
    @Schema(description = "색상 비고", example = "노란빛이 강한 골드")
    private String note;

    @Getter
    @NoArgsConstructor
    @Schema(description = "색상 단건 응답 DTO")
    public static class ResponseSingle {
        @Schema(description = "색상 ID", example = "1")
        private String colorId;
        @Schema(description = "색상명", example = "옐로골드")
        private String colorName;
        @Schema(description = "색상 비고", example = "노란빛이 강한 골드")
        private String colorNote;

        @Builder
        @QueryProjection
        public ResponseSingle(String colorId, String colorName, String colorNote) {
            this.colorId = colorId;
            this.colorName = colorName;
            this.colorNote = colorNote;
        }
    }
}
