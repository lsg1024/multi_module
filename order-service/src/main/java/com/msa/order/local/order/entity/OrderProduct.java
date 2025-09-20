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
    @Column(name = "PRODUCT_SIZE")
    private String productSize;
    @Column(name = "IS_GOLD_WEIGHT_SALE")
    private boolean isGoldWeightSale;
    @Column(name = "GOLD_WEIGHT", precision = 10, scale = 3)
    private BigDecimal goldWeight;
    @Column(name = "STONE_WEIGHT", precision = 10, scale = 3)
    private BigDecimal stoneWeight;
    @Column(name = "STONE_TOTAL_ADD_LABOR_COST")
    private Integer stoneTotalAddLaborCost;
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
    @Column(name = "MATERIAL_NAME")
    private String materialName; // 재질
    @Column(name = "CLASSIFICATION_NAME")
    private String classificationName; // 분류
    @Column(name = "COLOR_NAME")
    private String colorName; // 색
    @Column(name = "SET_TYPE") // 세트
    private String setType;
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
    public OrderProduct(Long productId, String productName, String productSize, boolean isGoldWeightSale, BigDecimal goldWeight, BigDecimal stoneWeight, Integer stoneTotalAddLaborCost, String orderMainStoneNote, String orderAssistanceStoneNote, Integer productPurchaseCost, Integer productLaborCost, Integer productAddLaborCost, String materialName, String classificationName, String colorName, String setType, boolean assistantStone, String assistantStoneName, OffsetDateTime assistantStoneCreateAt, Orders order) {
        this.productId = productId;
        this.productName = productName;
        this.productSize = productSize;
        this.isGoldWeightSale = isGoldWeightSale;
        this.goldWeight = goldWeight;
        this.stoneWeight = stoneWeight;
        this.stoneTotalAddLaborCost = stoneTotalAddLaborCost;
        this.orderMainStoneNote = orderMainStoneNote;
        this.orderAssistanceStoneNote = orderAssistanceStoneNote;
        this.productPurchaseCost = productPurchaseCost;
        this.productLaborCost = productLaborCost;
        this.productAddLaborCost = productAddLaborCost;
        this.materialName = materialName;
        this.classificationName = classificationName;
        this.colorName = colorName;
        this.setType = setType;
        this.assistantStone = assistantStone;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
        this.order = order;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }

    public void updateOrderProduct(String productName, Integer productPurchaseCost, Integer productLaborCost, String materialName, String colorName, String classificationName, String setType, boolean assistantStone, String assistantStoneName, OffsetDateTime assistantStoneCreateAt) {
        commonOrder(productName, productPurchaseCost, productLaborCost, materialName, colorName, classificationName, setType);
        this.assistantStone = assistantStone;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
    }


    public void updateOrderProduct(String productName, Integer productPurchaseCost, Integer productLaborCost, String materialName, String colorName, String classificationName, String setType) {
        commonOrder(productName, productPurchaseCost, productLaborCost, materialName, colorName, classificationName, setType);
    }



    private void commonOrder(String productName, Integer productPurchaseCost, Integer productLaborCost, String materialName, String colorName, String classificationName, String setType) {
        this.productName = productName;
        this.productPurchaseCost = productPurchaseCost;
        this.productLaborCost = productLaborCost;
        this.materialName = materialName;
        this.colorName = colorName;
        this.classificationName = classificationName;
        this.setType = setType;
    }

    public void updateOrderProductInfo(Long productId, BigDecimal stoneWeight, Integer productAddLaborCost, Integer stoneTotalAddLaborCost, String mainStoneNote, String assistanceStoneNote, String productSize, String classificationName, String setTypeName) {
        this.productId = productId;
        this.stoneWeight = stoneWeight;
        this.productAddLaborCost = productAddLaborCost;
        this.orderMainStoneNote = mainStoneNote;
        this.orderAssistanceStoneNote = assistanceStoneNote;
        this.stoneTotalAddLaborCost = stoneTotalAddLaborCost;
        this.productSize = productSize;
        this.classificationName = classificationName;
        this.setType = setTypeName;
    }
}