package com.msa.account.local.transaction_history.domain.dto;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransactionPage {
    private String saleCode;
    private String accountId;
    private String accountName;
    private String accountHarry;
    private String createDate;
    private String goldAmount;
    private String moneyAmount;
    private String tradeType;
    private String transactionNote;

    @QueryProjection
    public TransactionPage(String saleCode, String accountId, String accountName, String accountHarry, String createDate, String goldAmount, String moneyAmount, SaleStatus tradeType, String transactionNote) {
        this.saleCode = saleCode;
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountHarry = accountHarry;
        this.createDate = createDate.substring(0, 10);
        this.goldAmount = goldAmount;
        this.moneyAmount = moneyAmount;
        this.tradeType = tradeType.getDisplayName();
        this.transactionNote = transactionNote;
    }
}
