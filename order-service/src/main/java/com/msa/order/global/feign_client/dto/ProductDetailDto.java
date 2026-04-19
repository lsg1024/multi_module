package com.msa.order.global.feign_client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@NoArgsConstructor
public class ProductDetailDto {
    private Long productId;
    private String productName;
    private String productFactoryName;
    private Long classificationId;
    private String classificationName;
    private Long setTypeId;
    private String setTypeName;
    private Integer purchaseCost;
    private Integer laborCost;

    /**
     * 상품의 스톤 매핑 정보 (product-service GET /api/product/stones 응답용)
     */
    @Getter
    @NoArgsConstructor
    public static class StoneInfo {
        private String stoneId;
        private String stoneName;
        private String stoneWeight;
        private Integer purchaseCost;
        private Integer laborCost;
        private Integer quantity;
        private boolean mainStone;
        private boolean includeStone;
        private boolean includeQuantity;
        private boolean includePrice;
        private String stoneNote;
    }
}
