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
    @Column(name = "PRODUCT_FACTORY_NAME")
    private String productFactoryName;
    @Column(name = "PRODUCT_SIZE")
    private String productSize;
    @Column(name = "IS_PRODUCT_WEIGHT_SALE")
    private boolean isProductWeightSale;
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
    private boolean assistantStone = false; //보조석
    @Column(name = "ASSISTANT_STONE_ID")
    private Long assistantStoneId;
    @Column(name = "ASSISTANT_STONE_NAME")
    private String assistantStoneName;
    @Column(name = "ASSISTANT_STONE_CREATE_AT")
    private OffsetDateTime assistantStoneCreateAt;
    @Column(name = "STONE_ADD_LABOR_COST") // 추가 스톤 매출 비용
    private Integer stoneAddLaborCost;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Orders order;

    @Builder
    public OrderProduct(Long productId, String productName, String productFactoryName, String productSize, boolean isProductWeightSale, BigDecimal goldWeight, BigDecimal stoneWeight, String orderMainStoneNote, String orderAssistanceStoneNote, Integer productPurchaseCost, Integer productLaborCost, Integer productAddLaborCost, Long materialId, String materialName, Long classificationId, String classificationName, Long colorId, String colorName, Long setTypeId, String setTypeName, boolean assistantStone, Long assistantStoneId, String assistantStoneName, OffsetDateTime assistantStoneCreateAt, Integer stoneAddLaborCost, Orders order) {
        this.productId = productId;
        this.productName = productName;
        this.productFactoryName = productFactoryName;
        this.productSize = productSize;
        this.isProductWeightSale = isProductWeightSale;
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
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
        this.stoneAddLaborCost = stoneAddLaborCost;
        this.order = order;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }

    public void updateOrderProduct(String productName, String productFactoryName, Long materialId, String materialName, Long colorId, String colorName, Long classificationId, String classificationName, Long setTypeId, String setTypeName) {
        this.productName = productName;
        this.productFactoryName = productFactoryName;
        this.materialId = materialId;
        this.materialName = materialName;
        this.colorId = colorId;
        this.colorName = colorName;
        this.classificationId = classificationId;
        this.classificationName = classificationName;
        this.setTypeId = setTypeId;
        this.setTypeName = setTypeName;
    }

    public void updateOrderProductAssistantStone(boolean assistantStone, Long assistantStoneId, String assistantStoneName, OffsetDateTime assistantStoneCreateAt) {
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
    }

    public void updateOrderProductAssistantStoneFail(boolean assistantStone, Long assistantStoneId, String assistantStoneName) {
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneName = assistantStoneName;
    }


    public void updateOrderProductInfo(Long productId, BigDecimal stoneWeight, Integer productAddLaborCost, String mainStoneNote, String assistanceStoneNote, String productSize) {
        this.productId = productId;
        this.stoneWeight = stoneWeight;
        this.productAddLaborCost = productAddLaborCost;
        this.orderMainStoneNote = mainStoneNote;
        this.orderAssistanceStoneNote = assistanceStoneNote;
        this.productSize = productSize;
    }

    public void updateDetails(String productName, String classificationName, String setTypeName, String materialName, String colorName, Boolean isAssistantStone, Long assistantStoneId, String assistantStoneName, OffsetDateTime assistantStoneCreateAt) {
        if (productName != null) {
            this.productName = productName;
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
        // 보조석 관련 - assistantStone 플래그와 관계없이 값이 있으면 설정
        if (isAssistantStone != null) {
            this.assistantStone = isAssistantStone;
        }
        if (assistantStoneId != null) {
            this.assistantStoneId = assistantStoneId;
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