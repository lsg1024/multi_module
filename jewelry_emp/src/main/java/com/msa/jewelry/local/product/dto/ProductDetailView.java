package com.msa.jewelry.local.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 상세 view — 등급별 가격(매입/공임) 포함. grade 가 없으면 가격은 null 가능.")
public record ProductDetailView(
        @Schema(description = "상품 ID", example = "1001") Long productId,
        @Schema(description = "상품명", example = "프로포즈 솔리테어 반지") String productName,
        @Schema(description = "제조사가 부여한 제품명", example = "R-2024-001") String productFactoryName,
        @Schema(description = "분류 ID", example = "1") Long classificationId,
        @Schema(description = "분류명", example = "반지") String classificationName,
        @Schema(description = "세트 타입 ID", example = "1") Long setTypeId,
        @Schema(description = "세트 타입명", example = "단품") String setTypeName,
        @Schema(description = "등급 기준 상품 매입가 (원, null 가능)", example = "300000") Integer purchaseCost,
        @Schema(description = "등급 기준 상품 공임 (원, null 가능)", example = "50000") Integer laborCost
) {
}
