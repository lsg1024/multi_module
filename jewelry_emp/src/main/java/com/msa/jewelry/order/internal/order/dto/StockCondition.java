package com.msa.jewelry.order.internal.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "재고 카운트 검색 조건 — 상품명/재질명/색상명으로 활성 재고를 집계할 때 사용.")
public record StockCondition(
        @Schema(description = "상품 이름 (productName)", example = "다이아 1ct 반지")
        String pn,
        @Schema(description = "재질 이름 (materialName)", example = "18K")
        String mn,
        @Schema(description = "색상 이름 (colorName)", example = "옐로우골드")
        String cn
) {}