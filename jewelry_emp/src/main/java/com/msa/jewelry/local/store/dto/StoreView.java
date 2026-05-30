package com.msa.jewelry.local.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래처(매장) 정보 외부 노출용 view — 다른 모듈이 거래처를 참조할 때 사용")
public record StoreView(
        @Schema(description = "거래처 PK", example = "10")
        Long storeId,
        @Schema(description = "거래처명", example = "강남금은방")
        String storeName,
        @Schema(description = "거래처 등급 (A/B/C 등)", example = "A")
        String storeGrade,
        @Schema(description = "거래처 수수료(허리) — 문자열로 보관된 손모율", example = "1.5")
        String storeHarry,
        @Schema(description = "거래 유형 (BUY/SELL)", example = "SELL")
        String tradeType,
        @Schema(description = "과거 판매분 적용 여부", example = "false")
        boolean applyPastSales
) {
}
