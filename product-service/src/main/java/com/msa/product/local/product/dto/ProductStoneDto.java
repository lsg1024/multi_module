package com.msa.product.local.product.dto;

import com.msa.product.local.product.entity.ProductStone;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductStoneDto {
    private String stoneId;
    private boolean includeQuantity;
    private boolean includeWeight;
    private boolean includeLabor;
    private Integer stoneQuantity;

    @Builder
    public ProductStoneDto(String stoneId, boolean includeQuantity, boolean includeWeight, boolean includeLabor, Integer stoneQuantity) {
        this.stoneId = stoneId;
        this.includeQuantity = includeQuantity;
        this.includeWeight = includeWeight;
        this.includeLabor = includeLabor;
        this.stoneQuantity = stoneQuantity;
    }

    @Builder
    public static class Response {
        private String productStoneId;
        private boolean includeQuantity;
        private boolean includeWeight;
        private boolean includeLabor;
        private Integer stoneQuantity;

        public static Response fromEntity(ProductStone productStone) {
            return Response.builder()
                    .productStoneId(productStone.getProductStoneId().toString())
                    .includeWeight(productStone.getIncludeWeight())
                    .includeLabor(productStone.getIncludeLabor())
                    .includeQuantity(productStone.getIncludeQuantity())
                    .build();
        }
    }
}
