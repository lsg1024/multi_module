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
    @Column(name = "IS_PRODUCT_WEIGHT_SALE")
    private boolean isProductWeightSale;
    @Column(name = "PRODUCT_LABOR_COST") // 상품 매출 비용
    private Integer productLaborCost;
    @Column(name = "PRODUCT_ADD_LABOR_COST") // 상품 추가 매출 비용
    private Integer productAddLaborCost;
    @Column(name = "PRODUCT_PURCHASE_COST") // 상품 매입 비용
    private Integer productPurchaseCost;
    @Column(name = "MATERIAL_ID")
    private Long materialId;
    @Column(name = "MATERIAL_NAME")
    private String materialName;
    @Column(name = "COLOR_ID")
    private Long colorId;
    @Column(name = "COLOR_NAME")
    private String colorName;
    @Column(name = "CLASSIFICATION_ID")
    private Long classificationId;
    @Column(name = "CLASSIFICATION_NAME")
    private String classificationName;
    @Column(name = "SET_TYPE_ID") // 세트
    private Long setTypeId;
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
    private ProductSnapshot(Long id, String productName, String productFactoryName, String size, boolean isProductWeightSale, Integer productLaborCost,
                            Integer productAddLaborCost, Integer productPurchaseCost, Long materialId, String materialName, Long colorId, String colorName,
                            Long classificationId, String classificationName, Long setTypeId, String setTypeName, boolean assistantStone, Long assistantStoneId, String assistantStoneName, OffsetDateTime assistantStoneCreateAt, BigDecimal goldWeight, BigDecimal stoneWeight) {
        this.id = id;
        this.productName = productName;
        this.productFactoryName = productFactoryName;
        this.size = size;
        this.isProductWeightSale = isProductWeightSale;
        this.productLaborCost = productLaborCost;
        this.productAddLaborCost = productAddLaborCost;
        this.productPurchaseCost = productPurchaseCost;
        this.materialId = materialId;
        this.materialName = materialName;
        this.colorId = colorId;
        this.colorName = colorName;
        this.classificationId = classificationId;
        this.classificationName = classificationName;
        this.setTypeId = setTypeId;
        this.setTypeName = setTypeName;
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
        this.goldWeight = goldWeight;
        this.stoneWeight = stoneWeight;
    }

    public void updateProduct(String productName, Integer productLaborCost, Integer productPurchaseCost, String materialName, String classificationName, String colorName, String setTypeName, boolean assistantStone, Long assistantStoneId, String assistantStoneName, OffsetDateTime assistantStoneCreateAt) {
        this.productName = productName;
        this.productLaborCost = productLaborCost;
        this.productPurchaseCost = productPurchaseCost;
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

    public void updateAssistantStone(boolean assistantStone, Long assistantStoneId, String assistantStoneName, OffsetDateTime assistantStoneCreateAt) {
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
    }

    public void updateProductCost(Integer productPurchaseCost, Integer productLaborCost, Integer productAddLaborCost) {
        this.productPurchaseCost = productPurchaseCost;
        this.productLaborCost = productLaborCost;
        this.productAddLaborCost = productAddLaborCost;
    }

    public void updateProductAddCost(Integer addProductLaborCost) {
        this.productAddLaborCost = addProductLaborCost;
    }
}

