package com.msa.jewelry.order.internal.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Table(name = "ORDER_PRODUCT")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "주문 상품 엔티티 — 주문(Orders)에 1:1 로 매핑된 상품 스냅샷 (이름/재질/색/공임 등 거래 당시 값을 박제).")
public class OrderProduct {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_PRODUCT_ID")
    @Schema(description = "주문 상품 PK", example = "1001")
    private Long orderProductId;
    @Column(name = "PRODUCT_ID")
    @Schema(description = "상품 ID (product 모듈 FK)", example = "501")
    private Long productId;
    @Column(name = "PRODUCT_NAME")
    @Schema(description = "상품 이름 (거래 당시 스냅샷)", example = "다이아 1ct 반지")
    private String productName;
    @Column(name = "PRODUCT_FACTORY_NAME")
    @Schema(description = "제조사 표기명 (거래 당시 스냅샷)", example = "삼성공방")
    private String productFactoryName;
    @Column(name = "PRODUCT_SIZE")
    @Schema(description = "상품 사이즈 (반지 호수, 목걸이 길이 등)", example = "15호")
    private String productSize;
    @Column(name = "IS_PRODUCT_WEIGHT_SALE")
    @Schema(description = "중량 판매 여부 (true: 중량 기반, false: 정가 판매)", example = "false")
    private boolean isProductWeightSale;
    @Column(name = "GOLD_WEIGHT", precision = 10, scale = 3)
    @Schema(description = "금 무게 (g)", example = "3.250")
    private BigDecimal goldWeight;
    @Column(name = "STONE_WEIGHT", precision = 10, scale = 3)
    @Schema(description = "스톤 무게 (g)", example = "0.500")
    private BigDecimal stoneWeight;
    @Column(name = "ORDER_MAIN_STONE_NOTE")
    @Schema(description = "메인 스톤 비고", example = "1.0ct VS1 G")
    private String orderMainStoneNote;
    @Column(name = "ORDER_ASSISTANCE_STONE_NOTE")
    @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
    private String orderAssistanceStoneNote;
    @Column(name = "PRODUCT_PURCHASE_COST")
    @Schema(description = "상품 매입 비용 (공장 기준 원가)", example = "500000")
    private Integer productPurchaseCost;
    @Column(name = "PRODUCT_LABOR_COST")
    @Schema(description = "상품 공임 — 매장 grade 에 따라 가격 책정", example = "120000")
    private Integer productLaborCost; // 상점 grade 등급에 따라 가격
    @Column(name = "PRODUCT_ADD_LABOR_COST")
    @Schema(description = "상품 추가 공임", example = "20000")
    private Integer productAddLaborCost;
    @Column(name = "MATERIAL_ID")
    @Schema(description = "재질 ID (product.Material FK)", example = "1")
    private Long materialId; // 재질
    @Column(name = "MATERIAL_NAME")
    @Schema(description = "재질 이름 (스냅샷)", example = "18K")
    private String materialName; // 재질
    @Column(name = "CLASSIFICATION_ID")
    @Schema(description = "분류 ID (product.Classification FK)", example = "2")
    private Long classificationId; // 분류
    @Column(name = "CLASSIFICATION_NAME")
    @Schema(description = "분류 이름 (스냅샷)", example = "반지")
    private String classificationName; // 분류
    @Column(name = "COLOR_ID")
    @Schema(description = "색상 ID (product.Color FK)", example = "3")
    private Long colorId; // 색
    @Column(name = "COLOR_NAME")
    @Schema(description = "색상 이름 (스냅샷)", example = "옐로우골드")
    private String colorName; // 색
    @Column(name = "SET_TYPE_ID") // 세트
    @Schema(description = "세트 타입 ID (product.SetType FK)", example = "4")
    private Long setTypeId;
    @Column(name = "SET_TYPE_NAME") // 세트
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
    private String assistantStoneName;
    @Column(name = "ASSISTANT_STONE_CREATE_AT")
    @Schema(description = "보조석 생성 일시", example = "2026-05-16T14:30:00")
    private LocalDateTime assistantStoneCreateAt;
    @Column(name = "STONE_ADD_LABOR_COST") // 추가 스톤 매출 비용
    @Schema(description = "추가 스톤 매출 공임", example = "30000")
    private Integer stoneAddLaborCost;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    @Schema(description = "소속 주문 (Orders)")
    private Orders order;

    @Builder
    public OrderProduct(Long productId, String productName, String productFactoryName, String productSize, boolean isProductWeightSale, BigDecimal goldWeight, BigDecimal stoneWeight, String orderMainStoneNote, String orderAssistanceStoneNote, Integer productPurchaseCost, Integer productLaborCost, Integer productAddLaborCost, Long materialId, String materialName, Long classificationId, String classificationName, Long colorId, String colorName, Long setTypeId, String setTypeName, boolean assistantStone, Long assistantStoneId, String assistantStoneName, LocalDateTime assistantStoneCreateAt, Integer stoneAddLaborCost, Orders order) {
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

    /**
     * 주문 상품의 속성(이름/재질/색/분류/세트타입 등)을 부분 업데이트한다.
     * <p>
     * 각 파라미터가 {@code null} 이면 기존 값을 유지한다.
     * 빈 문자열("")도 덮어쓰지 않는다 — 클라이언트가 이름 필드를 payload 에서 누락했을 때
     * 기존 productName 이 공백으로 초기화되는 사고를 막기 위함.
     * 명시적으로 값을 지우고 싶으면 별도 전용 메서드를 추가할 것.
     */
    public void updateOrderProduct(String productName, String productFactoryName, Long materialId, String materialName, Long colorId, String colorName, Long classificationId, String classificationName, Long setTypeId, String setTypeName) {
        if (productName != null && !productName.isEmpty()) {
            this.productName = productName;
        }
        if (productFactoryName != null && !productFactoryName.isEmpty()) {
            this.productFactoryName = productFactoryName;
        }
        if (materialId != null) {
            this.materialId = materialId;
        }
        if (materialName != null && !materialName.isEmpty()) {
            this.materialName = materialName;
        }
        if (colorId != null) {
            this.colorId = colorId;
        }
        if (colorName != null && !colorName.isEmpty()) {
            this.colorName = colorName;
        }
        if (classificationId != null) {
            this.classificationId = classificationId;
        }
        if (classificationName != null && !classificationName.isEmpty()) {
            this.classificationName = classificationName;
        }
        if (setTypeId != null) {
            this.setTypeId = setTypeId;
        }
        if (setTypeName != null && !setTypeName.isEmpty()) {
            this.setTypeName = setTypeName;
        }
    }

    public void updateOrderProductAssistantStone(boolean assistantStone, Long assistantStoneId, String assistantStoneName, LocalDateTime assistantStoneCreateAt) {
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


    public void updateOrderProductInfo(Long productId, BigDecimal stoneWeight, Integer productPurchaseCost, Integer productLaborCost, Integer productAddLaborCost, String mainStoneNote, String assistanceStoneNote, String productSize) {
        this.productId = productId;
        this.stoneWeight = stoneWeight;
        this.productPurchaseCost = productPurchaseCost;
        this.productLaborCost = productLaborCost;
        this.productAddLaborCost = productAddLaborCost;
        this.orderMainStoneNote = mainStoneNote;
        this.orderAssistanceStoneNote = assistanceStoneNote;
        this.productSize = productSize;
    }

    public void updateDetails(String productName, String classificationName, String setTypeName, String materialName, String colorName, Boolean isAssistantStone, Long assistantStoneId, String assistantStoneName, LocalDateTime assistantStoneCreateAt) {
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

    public void updateOrderProductInfo(BigDecimal stoneWeight, Integer productPurchaseCost, Integer productLaborCost, Integer productAddLaborCost, String mainStoneNote, String assistanceStoneNote, String productSize) {
        this.stoneWeight = stoneWeight;
        this.productPurchaseCost = productPurchaseCost;
        this.productLaborCost = productLaborCost;
        this.productAddLaborCost = productAddLaborCost;
        this.orderMainStoneNote = mainStoneNote;
        this.orderAssistanceStoneNote = assistanceStoneNote;
        this.productSize = productSize;
    }
}