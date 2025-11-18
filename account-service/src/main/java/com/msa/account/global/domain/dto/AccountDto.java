package com.msa.account.global.domain.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.msa.account.global.domain.dto.util.ExchangeEnumUtil.*;

public class AccountDto {

    @Getter
    @NoArgsConstructor
    public static class accountResponse {
        private Long accountId;
        private String accountName;
        private String tradeType;
        private String level;
        private String goldHarryLoss;
        private String goldWeight;
        private String moneyAmount;
        private String lastPaymentDate;
        private String businessOwnerName;
        private String businessOwnerNumber;
        private String businessNumber1;
        private String businessNumber2;
        private String faxNumber;
        private String address;
        private String note;

        @Builder
        @QueryProjection
        public accountResponse(Long accountId, String accountName, String goldWeight, String moneyAmount, String businessOwnerName, String businessOwnerNumber, String businessNumber1, String businessNumber2, String faxNumber, String note, String level, String tradeType, String goldHarryLoss, String lastPaymentDate, String address) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.goldWeight = goldWeight;
            this.moneyAmount = moneyAmount;
            this.businessOwnerName = businessOwnerName;
            this.businessOwnerNumber = businessOwnerNumber;
            this.businessNumber1 = businessNumber1;
            this.businessNumber2 = businessNumber2;
            this.faxNumber = faxNumber;
            this.note = note;
            this.level = getLevelTypeTitle(level);
            this.tradeType = getTradeTypeTitle(tradeType);
            this.goldHarryLoss = goldHarryLoss;
            this.lastPaymentDate = lastPaymentDate;
            this.address = address;
        }
    }
}
