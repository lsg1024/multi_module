package com.msa.jewelry.product.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 상품 view DTO.
 *
 * <p>실제 Product 엔티티에는 color / goldWeight / productLaborCost 필드가 없으며
 * (Product 는 카탈로그 마스터, 가격·무게는 ProductWorkGradePolicy 와 ProductStone 에 분산),
 * 따라서 view 도 핵심 식별자와 마스터 데이터만 노출한다.
 *
 * @param productId           상품 ID
 * @param productName         상품명
 * @param materialName        재질명 (Product.material → Material.materialName)
 * @param classificationName  분류명
 * @param setTypeName         세트 타입
 * @param factoryId           제조사 ID
 * @param factoryName         제조사명
 * @param standardWeight      기준 무게 (Product.standardWeight)
 */
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
