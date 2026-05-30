package com.msa.jewelry.local.transaction_history.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래 후 현재 잔액 응답 — 금/현금 잔액 스냅샷")
public record TransactionDto(
        @Schema(description = "현재 금 미수 잔액 (문자열)", example = "12.345")
        String currentGoldBalance,
        @Schema(description = "현재 현금 미수 잔액 (문자열)", example = "1500000")
        String currentMoneyBalance) {
}
