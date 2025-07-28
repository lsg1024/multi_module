package com.msa.product.local.stone.type.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StoneTypeDto {
    private String name;
    private String note;

    @Getter
    @NoArgsConstructor
    public static class ResponseSingle {
        private String stoneTypeId;
        private String stoneTypeName;
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
