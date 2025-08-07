package com.msa.product.local.product.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductWorkGradePolicyGroupInfo {

    private Long groupId;
    private Integer productPurchasePrice;
    private String colorName;

    @QueryProjection
    public ProductWorkGradePolicyGroupInfo(Long groupId, Integer productPurchasePrice, String colorName) {
        this.groupId = groupId;
        this.productPurchasePrice = productPurchasePrice;
        this.colorName = colorName;
    }
}