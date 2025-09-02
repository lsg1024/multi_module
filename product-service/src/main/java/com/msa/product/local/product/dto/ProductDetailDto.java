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
    private Integer purchaseCost;
    private Integer laborCost;
    private List<StoneInfo> StoneInfos;

    public void setStoneInfos(List<StoneInfo> storeCosts) {
        this.StoneInfos = storeCosts;
    }

    @QueryProjection
    public ProductDetailDto(Long productId, String productName, String materialName, String colorName, Integer purchaseCost, Integer laborCost) {
        this.productId = productId;
        this.productName = productName;
        this.materialName = materialName;
        this.colorName = colorName;
        this.purchaseCost = purchaseCost;
        this.laborCost = laborCost;
    }

    @Getter
    @NoArgsConstructor
    public static class StoneInfo {
        private String stoneId;
        private String stoneName;
        private String stoneWeight;
        private Integer purchaseCost;
        private Integer laborCost;
        private Integer quantity;
        private boolean isMainStone;
        private boolean isIncludeStone;
        private String stoneNote;

        @QueryProjection
        public StoneInfo(String stoneId, String stoneName, String stoneWeight, Integer purchaseCost, Integer laborCost, Integer quantity, boolean isMainStone, boolean isIncludeStone, String stoneNote) {
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneWeight = stoneWeight;
            this.purchaseCost = purchaseCost;
            this.laborCost = laborCost;
            this.quantity = quantity;
            this.isMainStone = isMainStone;
            this.isIncludeStone = isIncludeStone;
            this.stoneNote = stoneNote;
        }
    }
}
