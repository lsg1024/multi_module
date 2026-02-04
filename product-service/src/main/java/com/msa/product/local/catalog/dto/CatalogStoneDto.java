package com.msa.product.local.catalog.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 카탈로그용 스톤 DTO (가격 정보 제외)
 */
@Getter
@NoArgsConstructor
public class CatalogStoneDto {

    /**
     * 카탈로그 목록용 스톤 정보
     * 가격 정보 제외: laborCost, purchasePrice
     */
    @Getter
    @NoArgsConstructor
    public static class PageResponse {
        private String productStoneId;
        private String stoneId;
        private String stoneName;
        private boolean mainStone;
        private boolean includeStone;
        private Integer stoneQuantity;

        @Builder
        public PageResponse(String productStoneId, String stoneId, String stoneName,
                            boolean mainStone, boolean includeStone, Integer stoneQuantity) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.stoneQuantity = stoneQuantity;
        }
    }

    /**
     * 카탈로그 상세용 스톤 정보
     * 가격 정보 제외: stonePurchase, laborCost
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String productStoneId;
        private String stoneId;
        private String stoneName;
        private BigDecimal stoneWeight;
        private boolean mainStone;
        private boolean includeStone;
        private Integer stoneQuantity;
        private String productStoneNote;

        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName,
                        BigDecimal stoneWeight, boolean mainStone, boolean includeStone,
                        Integer stoneQuantity, String productStoneNote) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneWeight = stoneWeight;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.stoneQuantity = stoneQuantity;
            this.productStoneNote = productStoneNote;
        }
    }
}
