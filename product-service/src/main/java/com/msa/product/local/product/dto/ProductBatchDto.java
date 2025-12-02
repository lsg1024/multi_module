package com.msa.product.local.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProductBatchDto {

    private String factoryName;
    private String productFactoryName;
    private String productName;
    private String setTypeName;
    private String classificationName;
    private String materialName; // materialId
    private String standardWeight;
    private String productNote;
    private List<BatchPolicyGroup> productWorkGradePolicyGroupDto;
    private List<BatchStone> productStoneDtos;

    @Getter
    @NoArgsConstructor
    public static class BatchPolicyGroup {
        private Integer productPurchasePrice;
        private String colorName;
        private List<ProductWorkGradePolicyDto> policyDtos;
        private String note;
    }

    @Getter
    @NoArgsConstructor
    public static class BatchStone {
        private String stoneName;
        private boolean mainStone;
        private boolean includeStone;
        private Integer stoneQuantity;
        private String productStoneNote;
    }

}
