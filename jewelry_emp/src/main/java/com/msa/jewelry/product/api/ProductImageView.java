package com.msa.jewelry.product.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 다른 모듈이 상품 메인 이미지를 조회할 때 사용하는 view.
 *
 * <p>기존 ProductClient.getProductImages 의 동기 등가물 (반환 element).
 *
 * @param productId  상품 ID
 * @param imagePath  메인 이미지 경로 (없으면 null)
 */
@Schema(description = "상품 대표 이미지 view — 다건 일괄 조회 결과 element")
public record ProductImageView(
        @Schema(description = "상품 ID", example = "1001") Long productId,
        @Schema(description = "대표 이미지 경로 (없으면 null)", example = "https://cdn.example.com/products/abc.jpg") String imagePath
) {
}
