package com.msa.product.local.product.dto;

import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProductStoneDto {
    private String stoneId;
    private boolean productStoneMain;
    private boolean includeQuantity;
    private boolean includeWeight;
    private boolean includeLabor;
    private Integer stoneQuantity;

    @Builder
    public ProductStoneDto(String stoneId, boolean productStoneMain, boolean includeQuantity, boolean includeWeight, boolean includeLabor, Integer stoneQuantity) {
        this.stoneId = stoneId;
        this.productStoneMain = productStoneMain;
        this.includeQuantity = includeQuantity;
        this.includeWeight = includeWeight;
        this.includeLabor = includeLabor;
        this.stoneQuantity = stoneQuantity;
    }

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String productStoneId;
        private String stoneId;
        private String stoneName;
        private boolean productStoneMain;
        private boolean includeQuantity;
        private boolean includeWeight;
        private boolean includeLabor;
        private Integer stoneQuantity;
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String productStoneId;
        private String stoneId;
        private String stoneName;
        private boolean productStoneMain;
        private boolean includeQuantity;
        private boolean includeWeight;
        private boolean includeLabor;
        private Integer stoneQuantity;
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos;

        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName, boolean productStoneMain, boolean includeQuantity, boolean includeWeight, boolean includeLabor, Integer stoneQuantity, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDtos) {
            essential(productStoneId, stoneId, stoneName, productStoneMain, includeQuantity, includeWeight, includeLabor, stoneQuantity);
            this.stoneWorkGradePolicyDtos = stoneWorkGradePolicyDtos;
        }


        @Builder
        @QueryProjection
        public Response(String productStoneId, String stoneId, String stoneName, boolean productStoneMain, boolean includeQuantity, boolean includeWeight, boolean includeLabor, Integer stoneQuantity) {
            essential(productStoneId, stoneId, stoneName, productStoneMain, includeQuantity, includeWeight, includeLabor, stoneQuantity);
        }

        private void essential(String productStoneId, String stoneId, String stoneName, boolean mainStone, boolean includeQuantity, boolean includeWeight, boolean includeLabor, Integer stoneQuantity) {
            this.productStoneId = productStoneId;
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.productStoneMain = mainStone;
            this.includeQuantity = includeQuantity;
            this.includeWeight = includeWeight;
            this.includeLabor = includeLabor;
            this.stoneQuantity = stoneQuantity;
        }
    }
}
