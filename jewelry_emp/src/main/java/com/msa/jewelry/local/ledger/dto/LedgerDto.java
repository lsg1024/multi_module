package com.msa.jewelry.local.ledger.dto;

import com.msa.jewelry.local.ledger.entity.AssetType;
import com.msa.jewelry.local.ledger.entity.Ledger;
import com.msa.jewelry.local.ledger.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "장부(Ledger) DTO 묶음 — 등록/수정 요청 및 응답 형태")
public class LedgerDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "장부 등록 요청")
    public static class CreateRequest {
        @NotNull(message = "날짜는 필수입니다")
        @Schema(description = "장부 기록일", example = "2026-05-16")
        private LocalDate ledgerDate;
        @NotNull(message = "자산 유형은 필수입니다")
        @Schema(description = "자산 유형 (GOLD/MONEY)", example = "GOLD")
        private AssetType assetType;
        @NotNull(message = "거래 유형은 필수입니다")
        @Schema(description = "거래 유형 (입금/출금 등)", example = "DEPOSIT")
        private TransactionType transactionType;
        @Schema(description = "금 금액(g)", example = "12.345")
        private BigDecimal goldAmount;
        @Schema(description = "현금 금액(원)", example = "1500000")
        private Long moneyAmount;
        @Schema(description = "장부 적요", example = "금 시세 차익 입고")
        private String description;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "장부 수정 요청")
    public static class UpdateRequest {
        @NotNull(message = "날짜는 필수입니다")
        @Schema(description = "장부 기록일", example = "2026-05-16")
        private LocalDate ledgerDate;
        @NotNull(message = "거래 유형은 필수입니다")
        @Schema(description = "거래 유형 (입금/출금 등)", example = "DEPOSIT")
        private TransactionType transactionType;
        @Schema(description = "금 금액(g)", example = "12.345")
        private BigDecimal goldAmount;
        @Schema(description = "현금 금액(원)", example = "1500000")
        private Long moneyAmount;
        @Schema(description = "장부 적요", example = "금 시세 차익 입고")
        private String description;
    }

    @Getter
    @Builder
    @Schema(description = "장부 응답")
    public static class LedgerResponse {
        @Schema(description = "장부 PK", example = "1001")
        private Long ledgerId;
        @Schema(description = "장부 기록일", example = "2026-05-16")
        private LocalDate ledgerDate;
        @Schema(description = "자산 유형", example = "GOLD")
        private AssetType assetType;
        @Schema(description = "거래 유형", example = "DEPOSIT")
        private TransactionType transactionType;
        @Schema(description = "금 금액(g)", example = "12.345")
        private BigDecimal goldAmount;
        @Schema(description = "현금 금액(원)", example = "1500000")
        private Long moneyAmount;
        @Schema(description = "장부 적요", example = "금 시세 차익 입고")
        private String description;

        public static LedgerResponse from(Ledger ledger) {
            return LedgerResponse.builder()
                    .ledgerId(ledger.getLedgerId())
                    .ledgerDate(ledger.getLedgerDate())
                    .assetType(ledger.getAssetType())
                    .transactionType(ledger.getTransactionType())
                    .goldAmount(ledger.getGoldAmount())
                    .moneyAmount(ledger.getMoneyAmount())
                    .description(ledger.getDescription())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "장부 잔액 응답 — 자산별 누적 합계")
    public static class BalanceResponse {
        @Schema(description = "총 금 잔액(g)", example = "30.500")
        private BigDecimal totalGold;
        @Schema(description = "총 현금 잔액(원)", example = "5000000")
        private Long totalMoney;
    }
}
