package com.msa.jewelry.local.product.dto;

import com.msa.jewelry.local.stone.dto.StoneWorkGradePolicyDto;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "상품-스톤 매핑 DTO — 등록 시 사용")
public class ProductStoneDto {
    @Schema(description = "스톤 ID", example = "301")
    private String stoneId;
    @Schema(description = "메인 스톤 여부", example = "true")
    private boolean mainStone;
    @Schema(description = "스톤 자체 가격 산정 포함 여부", example = "true")
    private boolean includeStone;
    @Schema(description = "스톤 개수 가격 산정 포함 여부", example = "true")
    private boolean includeQuantity;
    @Schema(description = "스톤 단가 가격 산정 포함 여부", example = "true")
    private boolean includePrice;
    @Schema(description = "스톤 개수(알 수)", example = "4")
    private Integer stoneQuantity;
    @Schema(description = "상품-스톤 비고", example = "측면 보조석 4알")
    private String productStoneNote;

    @Builder
    public ProductStoneDto(String stoneId, boolean mainStone, boolean includeStone, boolean includeQuantity, boolean includePrice, Integer stoneQuantity, String productStoneNote) {
        this.stoneId = stoneId;
        this.mainStone = mainStone;
        this.includeStone = includeStone;
        this.includeQuantity = includeQuantity;
        this.includePrice = includePrice;
        this.stoneQuantity = stoneQuantity;
        this.productStoneNote = productStoneNote;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "상품-스톤 매핑 수정 요청 DTO")
    public static class Request {
        @Schema(description = "상품-스톤 매핑 ID (신규면 null)", example = "7001")
        private String productStoneId;
        @Schema(description = "스톤 ID", example = "301")
        private String stoneId;
        @Schema(description = "메인 스톤 여부", example = "true")
        private boolean mainStone;
        @Schema(description = "스톤 자체 가격 산정 포함 여부", example = "true")
        private boolean includeStone;
        @Schema(description = "스톤 개수 가격 산정 포함 여부", example = "true")
        private boolean includeQuantity;
        @Schema(description = "스톤 단가 가격 산정 포함 여부", example = "true")
        private boolean includePrice;
        @Schema(description = "스톤 개수(알 수)", example = "4")
        private Integer stoneQuantity;
        @Schema(description = "상품-스톤 비고", example = "측면 보조석 4알")
        private String productStoneNote;

    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "상품 페이지 목록용 스톤 응답 DTO")
    public static class PageResponse {
        @Schema(description = "상품-스톤 매핑 ID", example = "7001")
        private String productStoneId;
        @Schema(description = "스톤 ID", example = "301")
        private String stoneId;
        @Schema(description = "스톤명", example = "다이아 라운드 0.3ct")
        private String stoneName;
        @Schema(description = "메인 스톤 여부", example = "true")
        private boolean mainStone;
        @Schema(description = "스톤 자체 가격 산정 포함 여부", example = "true")
        private boolean includeStone;
        @Schema(description = "스톤 개수 가격 산정 포함 여부", example = "true")
        private boolean includeQuantity;
        @Schema(description = "스톤 단가 가격 산정 포함 여부", example = "true")
        private boolean includePrice;
        @Schema(description = "스톤 개수(알 수)", example = "4")
        private Integer stoneQuantity;
        @Schema(description = "조회 등급 기준 스톤 공임 (원)", example = "30000")
        private Integer laborCost;
        @Schema(description = "스톤 매입가 (원)", example = "150000")
        private Integer purchasePrice;
        @Builder
        public PageResponse(String productStoneId, String stoneId, String stoneName, boolean mainStone, boolean includeStone, boolean includeQuantity, boolean includePrice, Integer stoneQuantity, Integer laborCost, Integer purchasePrice) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.includeQuantity = includeQuantity;
            this.includePrice = includePrice;
            this.stoneQuantity = stoneQuantity;
            this.laborCost = laborCost;
            this.purchasePrice = purchasePrice;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "상품-스톤 매핑 상세 응답 DTO")
    public static class Response {
        @Schema(description = "상품-스톤 매핑 ID", example = "7001")
        private String productStoneId;
        @Schema(description = "스톤 ID", example = "301")
        private String stoneId;
        @Schema(description = "스톤명", example = "다이아 라운드 0.3ct")
        private String stoneName;
        @Schema(description = "스톤 무게(캐럿)", example = "0.30")
        private BigDecimal stoneWeight;
        @Schema(description = "스톤 매입 단가 (원)", example = "150000")
        private Integer stonePurchase;
        @Schema(description = "메인 스톤 여부", example = "true")
        private boolean mainStone;
        @Schema(description = "스톤 자체 가격 산정 포함 여부", example = "true")
        private boolean includeStone;
        @Schema(description = "스톤 개수 가격 산정 포함 여부", example = "true")
        private boolean includeQuantity;
        @Schema(description = "스톤 단가 가격 산정 포함 여부", example = "true")
        private boolean includePrice;
        @Schema(description = "스톤 개수(알 수)", example = "4")
        private Integer stoneQuantity;
        @Schema(description = "상품-스톤 비고", example = "측면 보조석 4알")
        private String productStoneNote;
        @Schema(description = "스톤 등급별 공임 정책 목록")
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos;

        @Builder
        public Response(String productStoneId, String stoneId, String stoneName, Integer stoneQuantity, boolean mainStone, boolean includeStone, boolean includeQuantity, boolean includePrice, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneQuantity = stoneQuantity;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.includeQuantity = includeQuantity;
            this.includePrice = includePrice;
            this.stoneWorkGradePolicyDtos = stoneWorkGradePolicyDtos;
        }

        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName, BigDecimal stoneWeight, Integer stonePurchase, boolean mainStone, boolean includeStone, boolean includeQuantity, boolean includePrice, Integer stoneQuantity, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos, String productStoneNote) {
            essential(productStoneId, stoneId, stoneName, stoneWeight, stonePurchase, mainStone, includeStone, includeQuantity, includePrice, stoneQuantity, productStoneNote);
            this.stoneWorkGradePolicyDtos = stoneWorkGradePolicyDtos;
        }

        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName, BigDecimal stoneWeight, Integer stonePurchase, boolean mainStone, boolean includeStone, boolean includeQuantity, boolean includePrice, Integer stoneQuantity, String productStoneNote) {
            essential(productStoneId, stoneId, stoneName, stoneWeight, stonePurchase, mainStone, includeStone, includeQuantity, includePrice, stoneQuantity, productStoneNote);
        }

        private void essential(String productStoneId, String stoneId, String stoneName, BigDecimal stoneWeight, Integer stonePurchase, boolean mainStone, boolean includeStone, boolean includeQuantity, boolean includePrice, Integer stoneQuantity, String productStoneNote) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneWeight = stoneWeight;
            this.stonePurchase = stonePurchase;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.includeQuantity = includeQuantity;
            this.includePrice = includePrice;
            this.stoneQuantity = stoneQuantity;
            this.productStoneNote = productStoneNote;
        }
    }
}
