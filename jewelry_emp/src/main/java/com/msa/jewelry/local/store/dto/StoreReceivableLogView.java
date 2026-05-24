package com.msa.jewelry.local.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "판매 시점 거래처 미수금 잔액 변화 view — 판매 직전/직후 금/현금 잔액 + 최종 판매일")
public record StoreReceivableLogView(
        @Schema(description = "판매 직전 금 잔액 (문자열)", example = "12.345")
        String previousGoldBalance,
        @Schema(description = "판매 직전 현금 잔액 (문자열)", example = "1500000")
        String previousMoneyBalance,
        @Schema(description = "판매 직후 금 잔액 (문자열)", example = "15.678")
        String afterGoldBalance,
        @Schema(description = "판매 직후 현금 잔액 (문자열)", example = "2000000")
        String afterMoneyBalance,
        @Schema(description = "최종 판매일 (표시용 문자열)", example = "2026-05-16")
        String lastSaleDate
) {
}
