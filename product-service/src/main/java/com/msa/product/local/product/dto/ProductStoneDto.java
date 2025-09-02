package com.msa.product.local.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class ProductStoneDto {
    private String stoneId;
    @JsonProperty("isMainStone")
    private boolean isMainStone;
    @JsonProperty("isIncludeStone")
    private boolean isIncludeStone;
    private Integer stoneQuantity;
    private String productStoneNote;

    @Builder
    public ProductStoneDto(String stoneId, boolean isMainStone, boolean isIncludeStone, Integer stoneQuantity, String productStoneNote) {
        this.stoneId = stoneId;
        this.isMainStone = isMainStone;
        this.isIncludeStone = isIncludeStone;
        this.stoneQuantity = stoneQuantity;
        this.productStoneNote = productStoneNote;
    }

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String productStoneId;
        private String stoneId;
        private String stoneName;
        private boolean isMainStone;
        private boolean isIncludeStone;
        private Integer stoneQuantity;
        private String productStoneNote;
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String productStoneId;
        private String stoneId;
        private String stoneName;
        private BigDecimal stoneWeight;
        private Integer stonePurchase;
        private boolean isMainStone;
        private boolean isIncludeStone;
        private Integer stoneQuantity;
        private String productStoneNote;
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos;

        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName, BigDecimal stoneWeight, Integer stonePurchase, boolean isMainStone, boolean isIncludeStone, Integer stoneQuantity, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos, String productStoneNote) {
            essential(productStoneId, stoneId, stoneName, stoneWeight, stonePurchase, isMainStone, isIncludeStone, stoneQuantity, productStoneNote);
            this.stoneWorkGradePolicyDtos = stoneWorkGradePolicyDtos;
        }


        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName, BigDecimal stoneWeight, Integer stonePurchase, boolean isMainStone, boolean isIncludeStone, Integer stoneQuantity, String productStoneNote) {
            essential(productStoneId, stoneId, stoneName, stoneWeight, stonePurchase, isMainStone, isIncludeStone, stoneQuantity, productStoneNote);
        }

        private void essential(String productStoneId, String stoneId, String stoneName, BigDecimal stoneWeight, Integer stonePurchase, boolean isMainStone, boolean isIncludeStone, Integer stoneQuantity, String productStoneNote) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneWeight = stoneWeight;
            this.stonePurchase = stonePurchase;
            this.isMainStone = isMainStone;
            this.isIncludeStone = isIncludeStone;
            this.stoneQuantity = stoneQuantity;
            this.productStoneNote = productStoneNote;
        }
    }
}
