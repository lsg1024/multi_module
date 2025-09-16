package com.msa.product.local.product.dto;

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
    private boolean mainStone;
    private boolean includeStone;
    private Integer stoneQuantity;
    private String productStoneNote;

    @Builder
    public ProductStoneDto(String stoneId, boolean mainStone, boolean includeStone, Integer stoneQuantity, String productStoneNote) {
        this.stoneId = stoneId;
        this.mainStone = mainStone;
        this.includeStone = includeStone;
        this.stoneQuantity = stoneQuantity;
        this.productStoneNote = productStoneNote;
    }

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String productStoneId;
        private String stoneId;
        private boolean mainStone;
        private boolean includeStone;
        private Integer stoneQuantity;
        private String productStoneNote;

    }

    @Getter
    @NoArgsConstructor
    public static class PageResponse {
        private String productStoneId;
        private String stoneId;
        private String stoneName;
        private boolean mainStone;
        private boolean includeStone;
        private Integer stoneQuantity;
        private Integer laborCost;
        private Integer purchasePrice;
        @Builder
        public PageResponse(String productStoneId, String stoneId, String stoneName, boolean mainStone, boolean includeStone, Integer stoneQuantity, Integer laborCost, Integer purchasePrice) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.stoneQuantity = stoneQuantity;
            this.laborCost = laborCost;
            this.purchasePrice = purchasePrice;
        }
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
        private boolean mainStone;
        private boolean includeStone;
        private Integer stoneQuantity;
        private String productStoneNote;
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos;

        @Builder
        public Response(String productStoneId, String stoneId, String stoneName, Integer stoneQuantity, boolean mainStone, boolean includeStone, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneQuantity = stoneQuantity;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.stoneWorkGradePolicyDtos = stoneWorkGradePolicyDtos;
        }

        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName, BigDecimal stoneWeight, Integer stonePurchase, boolean mainStone, boolean includeStone, Integer stoneQuantity, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos, String productStoneNote) {
            essential(productStoneId, stoneId, stoneName, stoneWeight, stonePurchase, mainStone, includeStone, stoneQuantity, productStoneNote);
            this.stoneWorkGradePolicyDtos = stoneWorkGradePolicyDtos;
        }


        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName, BigDecimal stoneWeight, Integer stonePurchase, boolean mainStone, boolean includeStone, Integer stoneQuantity, String productStoneNote) {
            essential(productStoneId, stoneId, stoneName, stoneWeight, stonePurchase, mainStone, includeStone, stoneQuantity, productStoneNote);
        }

        private void essential(String productStoneId, String stoneId, String stoneName, BigDecimal stoneWeight, Integer stonePurchase, boolean mainStone, boolean includeStone, Integer stoneQuantity, String productStoneNote) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneWeight = stoneWeight;
            this.stonePurchase = stonePurchase;
            this.mainStone = mainStone;
            this.includeStone = includeStone;
            this.stoneQuantity = stoneQuantity;
            this.productStoneNote = productStoneNote;
        }
    }
}
