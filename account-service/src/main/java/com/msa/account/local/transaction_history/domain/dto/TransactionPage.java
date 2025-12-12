package com.msa.account.local.transaction_history.domain.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransactionPage {
    private String accountId; // eventId;
    private String accountName;
    private String createDate;
    private String goldAmount;
    private String moneyAmount;
    private String tradeType;

    @QueryProjection
    public TransactionPage(String accountId, String accountName, String createDate, String goldAmount, String moneyAmount, String tradeType) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.createDate = createDate;
        this.goldAmount = goldAmount;
        this.moneyAmount = moneyAmount;
        this.tradeType = tradeType;
    }
}
