package com.msa.jewelry.local.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "상품 view — 다른 모듈이 상품 마스터 정보를 조회할 때 반환되는 단순 view")
public record ProductView(
        @Schema(description = "상품 ID", example = "1001") Long productId,
        @Schema(description = "상품명", example = "프로포즈 솔리테어 반지") String productName,
        @Schema(description = "재질명", example = "18K") String materialName,
        @Schema(description = "분류명", example = "반지") String classificationName,
        @Schema(description = "세트 타입명", example = "단품") String setTypeName,
        @Schema(description = "제조사 ID", example = "5") Long factoryId,
        @Schema(description = "제조사명", example = "한국주얼리") String factoryName,
        @Schema(description = "기준 무게 (그램)", example = "3.50") BigDecimal standardWeight
) {
}
