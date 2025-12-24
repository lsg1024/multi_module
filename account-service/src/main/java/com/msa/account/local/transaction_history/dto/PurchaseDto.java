package com.msa.account.local.transaction_history.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class PurchaseDto {
    private OffsetDateTime transactionDate;
    private String transactionType;
    private BigDecimal goldAmount;
    private Long moneyAmount;
    private String saleCode;
    private String accountId;
    private String transactionNote;

    @Builder
    public PurchaseDto(String transactionType, BigDecimal goldAmount, Long moneyAmount, String saleCode, String accountId, String transactionNote) {
        this.transactionType = transactionType;
        this.goldAmount = goldAmount;
        this.moneyAmount = moneyAmount;
        this.saleCode = saleCode;
        this.accountId = accountId;
        this.transactionNote = transactionNote;
    }
}
