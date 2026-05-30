package com.msa.jewelry.local.transaction_history.dto;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "거래 이력 페이지 응답 — 판매 세션 코드 기준 거래 단건 평탄화")
public class TransactionPage {
    @Schema(description = "판매 세션 코드", example = "445823472384938240")
    private String saleCode;
    @Schema(description = "거래처 PK", example = "10")
    private String accountId;
    @Schema(description = "거래처명", example = "강남금은방")
    private String accountName;
    @Schema(description = "거래처 수수료(허리) — 24K 는 1.00 고정", example = "1.5")
    private String accountHarry;
    @Schema(description = "거래 일자 (yyyy-MM-dd)", example = "2026-05-16")
    private String createDate;
    @Schema(description = "재질 (14K/18K/24K 등)", example = "18K")
    private String material;
    @Schema(description = "금 수량(돈)", example = "3.333")
    private String goldAmount;
    @Schema(description = "현금 금액(원)", example = "500000")
    private String moneyAmount;
    @Schema(description = "거래 유형 (표시명)", example = "판매")
    private String tradeType;
    @Schema(description = "거래 비고", example = "신규 매입")
    private String transactionNote;

    @QueryProjection
    public TransactionPage(String saleCode, String accountId, String accountName, String accountHarry, String createDate, String material, String goldAmount, String moneyAmount, SaleStatus tradeType, String transactionNote) {
        this.saleCode = saleCode;
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountHarry = material.equals("24K") ? "1.00" : accountHarry;
        this.createDate = createDate.substring(0, 10);
        this.material = material;
        this.goldAmount = goldAmount;
        this.moneyAmount = moneyAmount;
        this.tradeType = tradeType.getDisplayName();
        this.transactionNote = transactionNote;
    }
}
