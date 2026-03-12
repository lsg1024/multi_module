package com.msa.account.local.ledger.domain.dto;

import com.msa.account.local.ledger.domain.entity.AssetType;
import com.msa.account.local.ledger.domain.entity.Ledger;
import com.msa.account.local.ledger.domain.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LedgerDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "날짜는 필수입니다")
        private LocalDate ledgerDate;
        @NotNull(message = "자산 유형은 필수입니다")
        private AssetType assetType;
        @NotNull(message = "거래 유형은 필수입니다")
        private TransactionType transactionType;
        private BigDecimal goldAmount;
        private Long moneyAmount;
        private String description;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        @NotNull(message = "날짜는 필수입니다")
        private LocalDate ledgerDate;
        @NotNull(message = "거래 유형은 필수입니다")
        private TransactionType transactionType;
        private BigDecimal goldAmount;
        private Long moneyAmount;
        private String description;
    }

    @Getter
    @Builder
    public static class LedgerResponse {
        private Long ledgerId;
        private LocalDate ledgerDate;
        private AssetType assetType;
        private TransactionType transactionType;
        private BigDecimal goldAmount;
        private Long moneyAmount;
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
    public static class BalanceResponse {
        private BigDecimal totalGold;
        private Long totalMoney;
    }
}
