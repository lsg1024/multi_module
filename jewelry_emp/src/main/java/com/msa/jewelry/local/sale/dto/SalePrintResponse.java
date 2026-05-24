package com.msa.jewelry.local.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "판매 영수증 인쇄 응답 — 잔고/직전 거래일과 판매 라인 그룹을 묶은 응답.")
public class SalePrintResponse {
    @Schema(description = "마지막 판매 일자", example = "2026-05-15")
    private String lastSaleDate;
    @Schema(description = "마지막 결제 일자", example = "2026-05-10")
    private String lastPaymentDate;
    @Schema(description = "이전 순금 잔고 (g, 문자열)", example = "12.500")
    private String previousGoldBalance;
    @Schema(description = "이전 현금 잔고 (원, 문자열)", example = "1500000")
    private String previousMoneyBalance;
    @Schema(description = "거래 후 순금 잔고 (g, 문자열)", example = "10.250")
    private String afterGoldBalance;
    @Schema(description = "거래 후 현금 잔고 (원, 문자열)", example = "1200000")
    private String afterMoneyBalance;
    @Schema(description = "판매 영수증 그룹 목록")
    private List<SaleItemResponse> saleItemResponses;

    @Builder
    public SalePrintResponse(String lastSaleDate, String lastPaymentDate, String previousMoneyBalance, String previousGoldBalance, String afterGoldBalance, String afterMoneyBalance, List<SaleItemResponse> saleItemResponses) {
        this.lastSaleDate = lastSaleDate;
        this.lastPaymentDate = lastPaymentDate;
        this.previousMoneyBalance = previousMoneyBalance;
        this.previousGoldBalance = previousGoldBalance;
        this.afterGoldBalance = afterGoldBalance;
        this.afterMoneyBalance = afterMoneyBalance;
        this.saleItemResponses = saleItemResponses;
    }
}
