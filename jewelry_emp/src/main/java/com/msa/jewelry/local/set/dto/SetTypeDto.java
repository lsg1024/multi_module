package com.msa.jewelry.local.set.dto;

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
@Schema(description = "세트 타입 요청 DTO — 단품/세트 등 세트 타입 마스터 생성/수정")
public class SetTypeDto {

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    @Schema(description = "세트 타입명", example = "단품")
    private String name;
    @Schema(description = "세트 타입 비고", example = "낱개 판매 단위")
    private String note;

    @Getter
    @NoArgsConstructor
    @Schema(description = "세트 타입 단건 응답 DTO")
    public static class ResponseSingle {
        @Schema(description = "세트 타입 ID", example = "1")
        private String setTypeId;
        @Schema(description = "세트 타입명", example = "단품")
        private String setTypeName;
        @Schema(description = "세트 타입 비고", example = "낱개 판매 단위")
        private String setTypeNote;

        @Builder
        @QueryProjection
        public ResponseSingle(String setTypeId, String setTypeName, String setTypeNote) {
            this.setTypeId = setTypeId;
            this.setTypeName = setTypeName;
            this.setTypeNote = setTypeNote;
        }
    }

}
