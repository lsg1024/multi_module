package com.msa.jewelry.local.stone_shape.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스톤 모양 요청 DTO — 원형/사각형 등 모양 마스터 생성/수정")
public class StoneShapeDto {
    @Schema(description = "스톤 모양명", example = "라운드")
    private String stoneShapeName;
    @Schema(description = "스톤 모양 비고", example = "원형 브릴리언트 컷")
    private String stoneShapeNote;

    @Getter
    @NoArgsConstructor
    @Schema(description = "스톤 모양 단건 응답 DTO")
    public static class ResponseSingle {
        @Schema(description = "스톤 모양 ID", example = "1")
        private String stoneShapeId;
        @Schema(description = "스톤 모양명", example = "라운드")
        private String stoneShapeName;
        @Schema(description = "스톤 모양 비고", example = "원형 브릴리언트 컷")
        private String stoneShapeNote;

        @Builder
        @QueryProjection
        public ResponseSingle(String stoneShapeId, String stoneShapeName, String stoneShapeNote) {
            this.stoneShapeId = stoneShapeId;
            this.stoneShapeName = stoneShapeName;
            this.stoneShapeNote = stoneShapeNote;
        }
    }
}
