package com.msa.jewelry.account.internal.transaction_history.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "매입(Purchase) 단건 DTO — 거래 일자/유형/금/금액/세션코드/거래처 평탄화")
public class PurchaseDto {
    @Schema(description = "거래 일시", example = "2026-05-16T14:30:00")
    private LocalDateTime transactionDate;
    @Schema(description = "거래 유형", example = "SALE")
    private String transactionType;
    @Schema(description = "금 수량(돈)", example = "3.333")
    private BigDecimal goldAmount;
    @Schema(description = "현금 금액(원)", example = "500000")
    private Long moneyAmount;
    @Schema(description = "판매 세션 코드 (TSID)", example = "445823472384938240")
    private String saleCode;
    @Schema(description = "거래처 PK (문자열)", example = "10")
    private String accountId;
    @Schema(description = "거래 비고", example = "신규 매입")
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
