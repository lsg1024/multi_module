package com.msa.product.local.product.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductDetailDto {

    private Long productId;
    private String productName;
    private Long classificationId;
    private String classificationName;
    private Long setTypeId;
    private String setTypeName;
    private Integer purchaseCost;
    private Integer laborCost;

    @QueryProjection
    public ProductDetailDto(Long productId, String productName, Long classificationId, String classificationName, Long setTypeId, String setTypeName, Integer purchaseCost, Integer laborCost) {
        this.productId = productId;
        this.productName = productName;
        this.classificationId = classificationId;
        this.classificationName = classificationName;
        this.setTypeId = setTypeId;
        this.setTypeName = setTypeName;
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
