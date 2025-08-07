package com.msa.product.local.product.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProductDetailDto {

    private Long productId;
    private String productName;
    private String materialName;
    private String colorName;
    private Integer laborCost;
    private List<StoneInfo> StoneInfos;

    public void setStoneInfos(List<StoneInfo> storeCosts) {
        this.StoneInfos = storeCosts;
    }

    @QueryProjection
    public ProductDetailDto(Long productId, String productName, String materialName, String colorName, Integer laborCost) {
        this.productId = productId;
        this.productName = productName;
        this.materialName = materialName;
        this.colorName = colorName;
        this.laborCost = laborCost;
    }

    @Getter
    @NoArgsConstructor
    public static class StoneInfo {
        private String stoneId;
        private String stoneName;
        private String stoneWeight;
        private String purchaseCost;
        private Integer laborCost;
        private Integer quantity;
        private boolean productStoneMain;
        private boolean includeQuantity;
        private boolean includeWeight;
        private boolean includeLabor;

        @QueryProjection
        public StoneInfo(String stoneId, String stoneName, String stoneWeight, String purchaseCost, Integer laborCost, Integer quantity, boolean productStoneMain, boolean includeQuantity, boolean includeWeight, boolean includeLabor) {
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneWeight = stoneWeight;
            this.purchaseCost = purchaseCost;
            this.laborCost = laborCost;
            this.quantity = quantity;
            this.productStoneMain = productStoneMain;
            this.includeQuantity = includeQuantity;
            this.includeWeight = includeWeight;
            this.includeLabor = includeLabor;
        }
    }
}
