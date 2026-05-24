package com.msa.jewelry.local.stone_type.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스톤 타입 요청 DTO — 다이아몬드/사파이어 등 보석 종류 마스터")
public class StoneTypeDto {
    @Schema(description = "스톤 타입명", example = "다이아몬드")
    private String name;
    @Schema(description = "스톤 타입 비고", example = "천연 다이아몬드")
    private String note;

    @Getter
    @NoArgsConstructor
    @Schema(description = "스톤 타입 단건 응답 DTO")
    public static class ResponseSingle {
        @Schema(description = "스톤 타입 ID", example = "1")
        private String stoneTypeId;
        @Schema(description = "스톤 타입명", example = "다이아몬드")
        private String stoneTypeName;
        @Schema(description = "스톤 타입 비고", example = "천연 다이아몬드")
        private String stoneTypeNote;

        @Builder
        @QueryProjection
        public ResponseSingle(String stoneTypeId, String stoneTypeName, String stoneTypeNote) {
            this.stoneTypeId = stoneTypeId;
            this.stoneTypeName = stoneTypeName;
            this.stoneTypeNote = stoneTypeNote;
        }
    }
}
