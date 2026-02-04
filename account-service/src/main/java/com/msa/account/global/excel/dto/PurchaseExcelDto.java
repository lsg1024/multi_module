package com.msa.account.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PurchaseExcelDto {

    private Long accountId;
    private String accountName;
    private String grade;
    private String goldWeight;
    private String moneyAmount;
    private String note;

    @Builder
    @QueryProjection
    public PurchaseExcelDto(Long accountId, String accountName, String grade,
                            String goldWeight, String moneyAmount, String note) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.grade = grade != null ? grade : "";
        this.goldWeight = goldWeight != null ? goldWeight : "0";
        this.moneyAmount = moneyAmount != null ? moneyAmount : "0";
        this.note = note != null ? note : "";
    }
}
