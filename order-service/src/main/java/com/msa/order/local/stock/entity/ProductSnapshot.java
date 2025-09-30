package com.msa.order.local.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSnapshot {
    @Column(name = "PRODUCT_ID", updatable = false)
    private Long id;
    @Column(name = "PRODUCT_NAME")
    private String productName;
    @Column(name = "PRODUCT_FACTORY_NAME")
    private String productFactoryName;
    @Column(name = "PRODUCT_SIZE")
    private String size;
    @Column(name = "IS_GOLD_WEIGHT_SALE")
    private boolean isGoldWeightSale;
    @Column(name = "PRODUCT_LABOR_COST") // 상품 매출 비용
    private Integer laborCost;
    @Column(name = "PRODUCT_ADD_LABOR_COST") // 상품 추가 매출 비용
    private Integer addLaborCost;
    @Column(name = "PRODUCT_PURCHASE_COST") // 상품 매입 비용
    private Integer productPurchaseCost;
    @Column(name = "MATERIAL_NAME")
    private String materialName;
    @Column(name = "COLOR_NAME")
    private String colorName;
    @Column(name = "CLASSIFICATION_NAME")
    private String classificationName;
    @Column(name = "SET_TYPE_NAME")
    private String setTypeName;
    @Column(name = "ASSISTANT_STONE")
    private boolean assistantStone = false; //보조석
    @Column(name = "ASSISTANT_STONE_ID")
    private Long assistantStoneId;
    @Column(name = "ASSISTANT_STONE_NAME")
    private String assistantStoneName; //보조석
    @Column(name = "ASSISTANT_STONE_CREATE_AT")
    private OffsetDateTime assistantStoneCreateAt;
    @Column(name = "GOLD_WEIGHT", precision = 10, scale = 3) // 상품 총 무게
    private BigDecimal goldWeight;
    @Column(name = "STONE_WEIGHT", precision = 10, scale = 3) // 상품 스톤 총 무게
    private BigDecimal stoneWeight;

    @Builder
    private ProductSnapshot(Long id, String productName, String productFactoryName, String size, boolean isGoldWeightSale, Integer laborCost,
                            Integer addLaborCost, Integer productPurchaseCost, String materialName, String colorName,
                            String classificationName, String setTypeName, boolean assistantStone, Long assistantStoneId, String assistantStoneName, OffsetDateTime assistantStoneCreateAt, BigDecimal goldWeight, BigDecimal stoneWeight) {
        this.id = id;
        this.productName = productName;
        this.productFactoryName = productFactoryName;
        this.size = size;
        this.isGoldWeightSale = isGoldWeightSale;
        this.laborCost = laborCost;
        this.addLaborCost = addLaborCost;
        this.productPurchaseCost = productPurchaseCost;
        this.materialName = materialName;
        this.colorName = colorName;
        this.classificationName = classificationName;
        this.setTypeName = setTypeName;
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
        this.goldWeight = goldWeight;
        this.stoneWeight = stoneWeight;
    }

    public void updateProduct(String productName, Integer productLaborCost, String materialName, String classificationName, String colorName, String setTypeName, boolean assistantStone, Long assistantStoneId, String assistantStoneName, OffsetDateTime assistantStoneCreateAt) {
        this.productName = productName;
        this.laborCost = productLaborCost;
        this.materialName = materialName;
        this.classificationName = classificationName;
        this.colorName = colorName;
        this.setTypeName = setTypeName;
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
    }

    public void updateProductWeightAndSize(String size, BigDecimal productWeight, BigDecimal stoneWeight) {
        this.size = size;
        this.goldWeight = productWeight;
        this.stoneWeight = stoneWeight;
    }
}

