package com.msa.jewelry.local.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "재고 view — 다른 모듈에서 flowCode 로 재고 핵심 필드를 조회하기 위한 읽기 전용 뷰.")
public record StockView(
        @Schema(description = "TSID 기반 전역 흐름 코드 (재고/주문 공통)", example = "445823472384938240")
        Long flowCode,
        @Schema(description = "재고 비즈니스 상태 (STOCK/RENTAL/SALE 등)", example = "STOCK")
        String orderStatus,
        @Schema(description = "상품 이름 (스냅샷)", example = "다이아 1ct 반지")
        String productName,
        @Schema(description = "재질 이름 (스냅샷)", example = "18K")
        String materialName,
        @Schema(description = "색상 이름 (스냅샷)", example = "옐로우골드")
        String colorName,
        @Schema(description = "금 무게 (g)", example = "3.250")
        BigDecimal goldWeight,
        @Schema(description = "스톤 무게 (g)", example = "0.500")
        BigDecimal stoneWeight
) {
}
