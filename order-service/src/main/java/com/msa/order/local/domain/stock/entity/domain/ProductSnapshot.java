package com.msa.order.local.domain.stock.entity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSnapshot {
    @Column(name = "PRODUCT_ID", updatable = false)
    private Long id;
    @Column(name = "PRODUCT_NAME", updatable = false)
    private String name;
    @Column(name = "PRODUCT_SIZE")
    private String size;
    @Column(name = "IS_PRODUCT_WEIGHT_SALE")
    private boolean isProductWeightSale;
    @Column(name = "PRODUCT_LABOR_COST")
    private Integer laborCost;
    @Column(name = "PRODUCT_ADD_LABOR_COST")
    private Integer addLaborCost;
    @Column(name = "PRODUCT_PURCHASE_COST")
    private Integer productPurchaseCost;
    @Column(name = "MATERIAL_NAME")
    private String materialName;
    @Column(name = "COLOR_NAME")
    private String colorName;
    @Column(name = "CLASSIFICATION_NAME")
    private String classificationName;
    @Column(name = "PRODUCT_WEIGHT", precision = 8, scale = 2)
    private BigDecimal productWeight;
    @Column(name = "STONE_WEIGHT", precision = 8, scale = 2)
    private BigDecimal stoneWeight;

    @Builder
    private ProductSnapshot(Long id, String name, String size, boolean isProductWeightSale, Integer laborCost,
                            Integer addLaborCost, Integer productPurchaseCost, String materialName, String colorName,
                            String classificationName, BigDecimal productWeight, BigDecimal stoneWeight) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.isProductWeightSale = isProductWeightSale;
        this.laborCost = laborCost;
        this.addLaborCost = addLaborCost;
        this.productPurchaseCost = productPurchaseCost;
        this.materialName = materialName;
        this.colorName = colorName;
        this.classificationName = classificationName;
        this.productWeight = productWeight;
        this.stoneWeight = stoneWeight;
    }

    public void updateProduct(String productName, Integer productLaborCost, String materialName, String classificationName, String colorName) {
        this.name = productName;
        this.laborCost = productLaborCost;
        this.materialName = materialName;
        this.classificationName = classificationName;
        this.colorName = colorName;
    }
}

