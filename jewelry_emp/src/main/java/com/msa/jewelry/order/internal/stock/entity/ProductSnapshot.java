package com.msa.jewelry.order.internal.stock.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "재고에 박힌 상품 스냅샷 — 거래 당시 상품의 이름/재질/색/공임/중량 등을 박제. Stock 에 Embedded.")
public class ProductSnapshot {
    @Column(name = "PRODUCT_ID", updatable = false)
    @Schema(description = "원본 상품 ID (product 모듈 FK)", example = "501")
    private Long id;
    @Column(name = "PRODUCT_NAME")
    @Schema(description = "상품 이름 (스냅샷)", example = "다이아 1ct 반지")
    private String productName;
    @Column(name = "PRODUCT_FACTORY_NAME")
    @Schema(description = "제조사 표기명 (스냅샷)", example = "삼성공방")
    private String productFactoryName;
    @Column(name = "PRODUCT_SIZE")
    @Schema(description = "상품 사이즈", example = "15호")
    private String size;
    @Column(name = "IS_PRODUCT_WEIGHT_SALE")
    @Schema(description = "중량 판매 여부", example = "false")
    private boolean isProductWeightSale;
    @Column(name = "PRODUCT_LABOR_COST") // 상품 매출 비용
    @Schema(description = "상품 매출 공임", example = "120000")
    private Integer productLaborCost;
    @Column(name = "PRODUCT_ADD_LABOR_COST") // 상품 추가 매출 비용
    @Schema(description = "상품 추가 매출 공임", example = "20000")
    private Integer productAddLaborCost;
    @Column(name = "PRODUCT_PURCHASE_COST") // 상품 매입 비용
    @Schema(description = "상품 매입 비용 (원가)", example = "500000")
    private Integer productPurchaseCost;
    @Column(name = "MATERIAL_ID")
    @Schema(description = "재질 ID (Material FK)", example = "1")
    private Long materialId;
    @Column(name = "MATERIAL_NAME")
    @Schema(description = "재질 이름 (스냅샷)", example = "18K")
    private String materialName;
    @Column(name = "COLOR_ID")
    @Schema(description = "색상 ID (Color FK)", example = "3")
    private Long colorId;
    @Column(name = "COLOR_NAME")
    @Schema(description = "색상 이름 (스냅샷)", example = "옐로우골드")
    private String colorName;
    @Column(name = "CLASSIFICATION_ID")
    @Schema(description = "분류 ID (Classification FK)", example = "2")
    private Long classificationId;
    @Column(name = "CLASSIFICATION_NAME")
    @Schema(description = "분류 이름 (스냅샷)", example = "반지")
    private String classificationName;
    @Column(name = "SET_TYPE_ID") // 세트
    @Schema(description = "세트 타입 ID (SetType FK)", example = "4")
    private Long setTypeId;
    @Column(name = "SET_TYPE_NAME")
    @Schema(description = "세트 타입 이름 (스냅샷)", example = "단품")
    private String setTypeName;
    @Column(name = "ASSISTANT_STONE")
    @Schema(description = "보조석 포함 여부", example = "true")
    private boolean assistantStone = false; //보조석
    @Column(name = "ASSISTANT_STONE_ID")
    @Schema(description = "보조석 ID", example = "10")
    private Long assistantStoneId;
    @Column(name = "ASSISTANT_STONE_NAME")
    @Schema(description = "보조석 이름 (스냅샷)", example = "큐빅")
    private String assistantStoneName; //보조석
    @Column(name = "ASSISTANT_STONE_CREATE_AT")
    @Schema(description = "보조석 생성 일시", example = "2026-05-16T14:30:00")
    private LocalDateTime assistantStoneCreateAt;
    @Column(name = "GOLD_WEIGHT", precision = 10, scale = 3) // 상품 총 무게
    @Schema(description = "상품 금 무게 (g)", example = "3.250")
    private BigDecimal goldWeight;
    @Column(name = "STONE_WEIGHT", precision = 10, scale = 3) // 상품 스톤 총 무게
    @Schema(description = "상품 스톤 무게 (g)", example = "0.500")
    private BigDecimal stoneWeight;

    @Builder
    private ProductSnapshot(Long id, String productName, String productFactoryName, String size, boolean isProductWeightSale, Integer productLaborCost,
                            Integer productAddLaborCost, Integer productPurchaseCost, Long materialId, String materialName, Long colorId, String colorName,
                            Long classificationId, String classificationName, Long setTypeId, String setTypeName, boolean assistantStone, Long assistantStoneId, String assistantStoneName, LocalDateTime assistantStoneCreateAt, BigDecimal goldWeight, BigDecimal stoneWeight) {
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

    public void updateProduct(String productName, String materialName, String classificationName, String colorName, String setTypeName, boolean assistantStone, Long assistantStoneId, String assistantStoneName, LocalDateTime assistantStoneCreateAt) {
        this.productName = productName;
        this.materialName = materialName;
        this.classificationName = classificationName;
        this.colorName = colorName;
        this.setTypeName = setTypeName;
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
    }

    /**
     * 사이즈/중량 정보를 부분 업데이트한다.
     * null 로 전달된 필드는 기존 값을 유지한다 (payload 누락 시 덮어쓰기 방지).
     */
    public void updateProductWeightAndSize(String size, BigDecimal productWeight, BigDecimal stoneWeight) {
        if (size != null) {
            this.size = size;
        }
        if (productWeight != null) {
            this.goldWeight = productWeight;
        }
        if (stoneWeight != null) {
            this.stoneWeight = stoneWeight;
        }
    }

    /**
     * 보조석 관련 필드를 업데이트한다. boolean 필드는 항상 갱신되며,
     * 문자열/ID/날짜는 null 이 아닐 때만 덮어쓴다.
     */
    public void updateAssistantStone(boolean assistantStone, Long assistantStoneId, String assistantStoneName, LocalDateTime assistantStoneCreateAt) {
        this.assistantStone = assistantStone;
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

    public void updateProductCost(Integer productPurchaseCost, Integer productLaborCost, Integer productAddLaborCost) {
        if (productPurchaseCost != null) {
            this.productPurchaseCost = productPurchaseCost;
        }
        if (productLaborCost != null) {
            this.productLaborCost = productLaborCost;
        }
        if (productAddLaborCost != null) {
            this.productAddLaborCost = productAddLaborCost;
        }
    }

    public void updateProductAddCost(Integer addProductLaborCost) {
        if (addProductLaborCost != null) {
            this.productAddLaborCost = addProductLaborCost;
        }
    }

}

