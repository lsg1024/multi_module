package com.msa.jewelry.local.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 대표 이미지 view — 다건 일괄 조회 결과 element")
public record ProductImageView(
        @Schema(description = "상품 ID", example = "1001") Long productId,
        @Schema(description = "대표 이미지 경로 (없으면 null)", example = "https://cdn.example.com/products/abc.jpg") String imagePath
) {
}
