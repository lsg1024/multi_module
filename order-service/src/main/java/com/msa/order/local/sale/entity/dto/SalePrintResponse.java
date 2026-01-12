package com.msa.order.local.sale.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SalePrintResponse {
    private String lastSaleDate;
    private String lastPaymentDate;
    private String businessOwnerNumber;
    private String faxNumber;
    private String previousGoldBalance;
    private String previousMoneyBalance;
    private String afterGoldBalance;
    private String afterMoneyBalance;
    private List<SaleItemResponse> saleItemResponses;

    @Builder
    public SalePrintResponse(String lastSaleDate, String lastPaymentDate, String businessOwnerNumber, String faxNumber, String previousMoneyBalance, String previousGoldBalance, String afterGoldBalance, String afterMoneyBalance, List<SaleItemResponse> saleItemResponses) {
        this.lastSaleDate = lastSaleDate;
        this.lastPaymentDate = lastPaymentDate;
        this.businessOwnerNumber = businessOwnerNumber;
        this.faxNumber = faxNumber;
        this.previousMoneyBalance = previousMoneyBalance;
        this.previousGoldBalance = previousGoldBalance;
        this.afterGoldBalance = afterGoldBalance;
        this.afterMoneyBalance = afterMoneyBalance;
        this.saleItemResponses = saleItemResponses;
    }
}
