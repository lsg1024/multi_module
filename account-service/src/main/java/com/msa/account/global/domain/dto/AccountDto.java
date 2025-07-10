package com.msa.account.global.domain.dto;

import com.msa.account.global.domain.entity.OptionTradeType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AccountDto {

    @Getter
    @NoArgsConstructor
    public static class accountInfo {
        private String createAt;
        private String businessName;
        private String businessOwnerName;
        private String businessNumber1;
        private String businessNumber2;
        private String faxNumber;
        private String address;
        private String note;
        private String level;
        private String tradeType;
        private String goldLoss;

        @Builder
        @QueryProjection
        public accountInfo(String createAt,String businessName, String businessOwnerName, String businessNumber1, String businessNumber2, String faxNumber, String address, String note, String level, String tradeType, String goldLoss) {
            this.createAt = createAt;
            this.businessName = businessName;
            this.businessOwnerName = businessOwnerName;
            this.businessNumber1 = businessNumber1;
            this.businessNumber2 = businessNumber2;
            this.faxNumber = faxNumber;
            this.address = address;
            this.note = note;
            this.level = level;
            this.tradeType = tradeType;
            this.goldLoss = goldLoss;
        }

        public void getTradeTypeTitle() {
            this.tradeType = OptionTradeType.getTitleByKey(tradeType);
        }

        public void getLevelTypeLevel() {
            this.level = OptionTradeType.getTitleByKey(level);
        }

    }
}
