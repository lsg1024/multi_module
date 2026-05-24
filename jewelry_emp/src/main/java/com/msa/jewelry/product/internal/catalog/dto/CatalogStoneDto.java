package com.msa.jewelry.product.internal.catalog.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "카탈로그용 스톤 DTO 묶음 — 가격 정보 제외")
public class CatalogStoneDto {

    /**
     * 카탈로그 목록용 스톤 정보
     * 가격 정보 제외: laborCost, purchasePrice
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "카탈로그 목록용 스톤 응답 DTO (가격 제외)")
    public static class PageResponse {
        @Schema(description = "상품-스톤 매핑 ID", example = "7001")
        private String productStoneId;
        @Schema(description = "스톤 ID", example = "301")
        private String stoneId;
        @Schema(description = "스톤명", example = "다이아 라운드 0.3ct")
        private String stoneName;
        @Schema(description = "메인 스톤 여부", example = "true")
        private boolean mainStone;
        @Schema(description = "스톤 자체 가격 포함 여부", example = "true")
        private boolean includeStone;
        @Schema(description = "스톤 개수 가격 포함 여부", example = "true")
        private boolean includeQuantity;
        @Schema(description = "스톤 단가 가격 포함 여부", example = "true")
        private boolean includePrice;
        @Schema(description = "스톤 개수(알 수)", example = "4")
        private Integer stoneQuantity;

        @Builder
        public PageResponse(String productStoneId, String stoneId, String stoneName,
                            boolean mainStone, boolean includeStone, boolean includeQuantity,
                            boolean includePrice, Integer stoneQuantity) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.includeQuantity = includeQuantity;
            this.includePrice = includePrice;
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
    @Schema(description = "카탈로그 상세용 스톤 응답 DTO (가격 제외)")
    public static class Response {
        @Schema(description = "상품-스톤 매핑 ID", example = "7001")
        private String productStoneId;
        @Schema(description = "스톤 ID", example = "301")
        private String stoneId;
        @Schema(description = "스톤명", example = "다이아 라운드 0.3ct")
        private String stoneName;
        @Schema(description = "스톤 무게(캐럿/그램)", example = "0.30")
        private BigDecimal stoneWeight;
        @Schema(description = "메인 스톤 여부", example = "true")
        private boolean mainStone;
        @Schema(description = "스톤 자체 가격 포함 여부", example = "true")
        private boolean includeStone;
        @Schema(description = "스톤 개수 가격 포함 여부", example = "true")
        private boolean includeQuantity;
        @Schema(description = "스톤 단가 가격 포함 여부", example = "true")
        private boolean includePrice;
        @Schema(description = "스톤 개수(알 수)", example = "4")
        private Integer stoneQuantity;
        @Schema(description = "상품-스톤 비고", example = "측면 보조석 4알")
        private String productStoneNote;

        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName,
                        BigDecimal stoneWeight, boolean mainStone, boolean includeStone,
                        boolean includeQuantity, boolean includePrice,
                        Integer stoneQuantity, String productStoneNote) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneWeight = stoneWeight;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.includeQuantity = includeQuantity;
            this.includePrice = includePrice;
            this.stoneQuantity = stoneQuantity;
            this.productStoneNote = productStoneNote;
        }
    }
}
