package com.msa.product.local.stone.shape.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StoneShapeDto {
    private String name;
    private String note;

    @Getter
    @NoArgsConstructor
    public static class ResponseSingle {
        private String stoneShapeId;
        private String stoneShapeName;
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
