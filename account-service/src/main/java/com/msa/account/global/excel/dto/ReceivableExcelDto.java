package com.msa.account.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReceivableExcelDto {

    private Long accountId;
    private String accountName;
    private String grade;
    private String goldWeight;
    private String moneyAmount;
    private String lastSaleDate;
    private String lastPaymentDate;
    private String note;

    @Builder
    @QueryProjection
    public ReceivableExcelDto(Long accountId, String accountName, String grade,
                              String goldWeight, String moneyAmount,
                              String lastSaleDate, String lastPaymentDate, String note) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.grade = grade != null ? grade : "";
        this.goldWeight = goldWeight != null ? goldWeight : "0";
        this.moneyAmount = moneyAmount != null ? moneyAmount : "0";
        this.lastSaleDate = lastSaleDate != null ? lastSaleDate : "";
        this.lastPaymentDate = lastPaymentDate != null ? lastPaymentDate : "";
        this.note = note != null ? note : "";
    }
}
