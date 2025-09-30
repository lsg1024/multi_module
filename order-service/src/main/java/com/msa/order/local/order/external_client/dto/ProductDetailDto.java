package com.msa.order.local.order.external_client.dto;

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
}
