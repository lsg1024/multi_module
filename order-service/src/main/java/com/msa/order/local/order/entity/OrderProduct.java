package com.msa.order.local.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Table(name = "ORDER_PRODUCT")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderProduct {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_PRODUCT_ID")
    private Long orderProductId;
    @Column(name = "PRODUCT_ID")
    private Long productId;
    @Column(name = "PRODUCT_NAME")
    private String productName;
//    @Column(name = "PRODUCT_FACTORY_NAME")
//    private String productFactoryName;
    @Column(name = "PRODUCT_SIZE")
    private String productSize;
    @Column(name = "IS_GOLD_WEIGHT_SALE")
    private boolean isGoldWeightSale;
    @Column(name = "GOLD_WEIGHT", precision = 10, scale = 3)
    private BigDecimal goldWeight;
    @Column(name = "STONE_WEIGHT", precision = 10, scale = 3)
    private BigDecimal stoneWeight;
    @Column(name = "ORDER_MAIN_STONE_NOTE")
    private String orderMainStoneNote;
    @Column(name = "ORDER_ASSISTANCE_STONE_NOTE")
    private String orderAssistanceStoneNote;
    @Column(name = "PRODUCT_PURCHASE_COST")
    private Integer productPurchaseCost;
    @Column(name = "PRODUCT_LABOR_COST")
    private Integer productLaborCost; // 상점 grade 등급에 따라 가격
    @Column(name = "PRODUCT_ADD_LABOR_COST")
    private Integer productAddLaborCost;
    @Column(name = "MATERIAL_ID")
    private Long materialId; // 재질
    @Column(name = "MATERIAL_NAME")
    private String materialName; // 재질
    @Column(name = "CLASSIFICATION_ID")
    private Long classificationId; // 분류
    @Column(name = "CLASSIFICATION_NAME")
    private String classificationName; // 분류
    @Column(name = "COLOR_ID")
    private Long colorId; // 색
    @Column(name = "COLOR_NAME")
    private String colorName; // 색
    @Column(name = "SET_TYPE_ID") // 세트
    private Long setTypeId;
    @Column(name = "SET_TYPE_NAME") // 세트
    private String setTypeName;
    @Column(name = "ASSISTANT_STONE")
    private boolean assistantStone = false;
    @Column(name = "ASSISTANT_STONE_NAME")
    private String assistantStoneName; //보조석
    @Column(name = "ASSISTANT_STONE_CREATE_AT")
    private OffsetDateTime assistantStoneCreateAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Orders order;

    @Builder
    public OrderProduct(Long productId, String productName, String productSize, boolean isGoldWeightSale, BigDecimal goldWeight, BigDecimal stoneWeight, String orderMainStoneNote, String orderAssistanceStoneNote, Integer productPurchaseCost, Integer productLaborCost, Integer productAddLaborCost, Long materialId, String materialName, Long classificationId, String classificationName, Long colorId, String colorName, Long setTypeId, String setTypeName, boolean assistantStone, String assistantStoneName, OffsetDateTime assistantStoneCreateAt, Orders order) {
        this.productId = productId;
        this.productName = productName;
//        this.productFactoryName = productFactoryName;
        this.productSize = productSize;
        this.isGoldWeightSale = isGoldWeightSale;
        this.goldWeight = goldWeight;
        this.stoneWeight = stoneWeight;
        this.orderMainStoneNote = orderMainStoneNote;
        this.orderAssistanceStoneNote = orderAssistanceStoneNote;
        this.productPurchaseCost = productPurchaseCost;
        this.productLaborCost = productLaborCost;
        this.productAddLaborCost = productAddLaborCost;
        this.materialId = materialId;
        this.materialName = materialName;
        this.classificationId = classificationId;
        this.classificationName = classificationName;
        this.colorId = colorId;
        this.colorName = colorName;
        this.setTypeId = setTypeId;
        this.setTypeName = setTypeName;
        this.assistantStone = assistantStone;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
        this.order = order;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }

    public void updateOrderProduct(String productName, Integer productPurchaseCost, Integer laborCost, Long materialId, String materialName, Long colorId, String colorName, Long classificationId, String classificationName, Long setTypeId, String setTypeName, boolean assistantStone, String assistantStoneName, OffsetDateTime assistantStoneCreateAt) {
        this.productName = productName;
        this.productPurchaseCost = productPurchaseCost;
        this.productAddLaborCost = laborCost;
        this.materialId = materialId;
        this.materialName = materialName;
        this.colorId = colorId;
        this.colorName = colorName;
        this.classificationId = classificationId;
        this.classificationName = classificationName;
        this.setTypeId = setTypeId;
        this.setTypeName = setTypeName;
        this.assistantStone = assistantStone;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
    }

    public void updateOrderProduct(String productName, Integer productPurchaseCost, Integer laborCost, Long materialId, String materialName, Long colorId, String colorName, Long classificationId, String classificationName, Long setTypeId, String setTypeName) {
        this.productName = productName;
        this.productPurchaseCost = productPurchaseCost;
        this.productAddLaborCost = laborCost;
        this.materialId = materialId;
        this.materialName = materialName;
        this.colorId = colorId;
        this.colorName = colorName;
        this.classificationId = classificationId;
        this.classificationName = classificationName;
        this.setTypeId = setTypeId;
        this.setTypeName = setTypeName;
    }

    public void updateOrderProductInfo(Long productId, BigDecimal stoneWeight, Integer productAddLaborCost, String mainStoneNote, String assistanceStoneNote, String productSize) {
        this.productId = productId;
        this.stoneWeight = stoneWeight;
        this.productAddLaborCost = productAddLaborCost;
        this.orderMainStoneNote = mainStoneNote;
        this.orderAssistanceStoneNote = assistanceStoneNote;
        this.productSize = productSize;
    }

    public void updateDetails(String productName, Integer productPurchaseCost, Integer laborCost, String classificationName, String setTypeName, String materialName, String colorName, Boolean isAssistantStone, String assistantStoneName, OffsetDateTime assistantStoneCreateAt) {
        if (productName != null) {
            this.productName = productName;
        }
        if (productPurchaseCost != null) {
            this.productPurchaseCost = productPurchaseCost;
        }
        if (laborCost != null) {
            this.productLaborCost = laborCost;
        }
        if (classificationName != null) {
            this.classificationName = classificationName;
        }
        if (setTypeName != null) {
            this.setTypeName = setTypeName;
        }
        if (materialName != null) {
            this.materialName = materialName;
        }
        if (colorName != null) {
            this.colorName = colorName;
        }
        if (isAssistantStone != null) {
            this.assistantStone = isAssistantStone;
            if (!isAssistantStone) {
                this.assistantStoneName = null;
                this.assistantStoneCreateAt = null;
            }
        }
        if (assistantStoneName != null) {
            this.assistantStoneName = assistantStoneName;
        }
        if (assistantStoneCreateAt != null) {
            this.assistantStoneCreateAt = assistantStoneCreateAt;
        }
    }

    public void updateOrderProductInfo(BigDecimal stoneWeight, Integer productAddLaborCost, String mainStoneNote, String assistanceStoneNote, String productSize) {
        this.stoneWeight = stoneWeight;
        this.productAddLaborCost = productAddLaborCost;
        this.orderMainStoneNote = mainStoneNote;
        this.orderAssistanceStoneNote = assistanceStoneNote;
        this.productSize = productSize;
    }
}