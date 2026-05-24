package com.msa.jewelry.product.internal.product.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "상품 상세 DTO — 등급별 가격(매입/공임) 포함 상세 정보")
public class ProductDetailDto {

    @Schema(description = "상품 ID", example = "1001")
    private Long productId;
    @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
    private String productName;
    @Schema(description = "제조사가 부여한 제품명", example = "R-2024-001")
    private String productFactoryName;
    @Schema(description = "분류 ID", example = "1")
    private Long classificationId;
    @Schema(description = "분류명", example = "반지")
    private String classificationName;
    @Schema(description = "세트 타입 ID", example = "1")
    private Long setTypeId;
    @Schema(description = "세트 타입명", example = "단품")
    private String setTypeName;
    @Schema(description = "등급 기준 상품 매입가 (원)", example = "300000")
    private Integer purchaseCost;
    @Schema(description = "등급 기준 상품 공임 (원)", example = "50000")
    private Integer laborCost;

    @QueryProjection
    public ProductDetailDto(Long productId, String productName, String productFactoryName, Long classificationId, String classificationName, Long setTypeId, String setTypeName, Integer purchaseCost, Integer laborCost) {
        this.productId = productId;
        this.productName = productName;
        this.productFactoryName = productFactoryName;
        this.classificationId = classificationId;
        this.classificationName = classificationName;
        this.setTypeId = setTypeId;
        this.setTypeName = setTypeName;
        this.purchaseCost = purchaseCost != null ? purchaseCost : 0;
        this.laborCost = laborCost != null ? laborCost : 0;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "상품 상세 내 스톤 정보 DTO (가격 포함)")
    public static class StoneInfo {
        @Schema(description = "스톤 ID", example = "301")
        private String stoneId;
        @Schema(description = "스톤명", example = "다이아 라운드 0.3ct")
        private String stoneName;
        @Schema(description = "스톤 무게(캐럿/그램)", example = "0.30")
        private String stoneWeight;
        @Schema(description = "스톤 매입 단가 (원)", example = "150000")
        private Integer purchaseCost;
        @Schema(description = "등급 기준 스톤 공임 (원)", example = "30000")
        private Integer laborCost;
        @Schema(description = "스톤 개수(알 수)", example = "4")
        private Integer quantity;
        @Schema(description = "메인 스톤 여부", example = "true")
        private boolean isMainStone;
        @Schema(description = "스톤 자체 포함 여부", example = "true")
        private boolean isIncludeStone;
        @Schema(description = "개수 가격 포함 여부", example = "true")
        private boolean isIncludeQuantity;
        @Schema(description = "단가 가격 포함 여부", example = "true")
        private boolean isIncludePrice;
        @Schema(description = "상품-스톤 비고", example = "측면 보조석 4알")
        private String stoneNote;

        @QueryProjection
        public StoneInfo(String stoneId, String stoneName, String stoneWeight, Integer purchaseCost, Integer laborCost, Integer quantity, boolean isMainStone, boolean isIncludeStone, boolean isIncludeQuantity, boolean isIncludePrice, String stoneNote) {
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneWeight = stoneWeight;
            this.purchaseCost = purchaseCost;
            this.laborCost = laborCost;
            this.quantity = quantity;
            this.isMainStone = isMainStone;
            this.isIncludeStone = isIncludeStone;
            this.isIncludeQuantity = isIncludeQuantity;
            this.isIncludePrice = isIncludePrice;
            this.stoneNote = stoneNote;
        }
    }
}
