package com.msa.jewelry.product.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 다른 모듈이 상품 상세(이름 + 분류 + 세트 + 등급별 가격) 정보를 조회할 때 사용하는 view.
 *
 * <p>기존 MSA 의 {@code ProductFeignClient.getProductInfo / getProductInfoByName} 호출이
 * 반환했던 {@code ProductDetailDto} 의 동기 등가물.
 *
 * <p>{@code purchaseCost} / {@code laborCost} 는 grade 별 정책으로 계산된 값.
 * grade 가 명시되지 않은 조회(by name) 의 경우 null 일 수 있음.
 *
 * @param productId           상품 ID
 * @param productName         상품명
 * @param productFactoryName  상품 제조사명 (Product.productFactoryName 그대로)
 * @param classificationId    분류 ID
 * @param classificationName  분류명
 * @param setTypeId           세트 타입 ID
 * @param setTypeName         세트 타입명
 * @param purchaseCost        매입 단가 (grade 기반, null 가능)
 * @param laborCost           공임 (grade 기반, null 가능)
 */
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
