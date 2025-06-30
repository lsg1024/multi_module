package com.account.global.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class AccountDto {

    @Getter
    @AllArgsConstructor
    public static class accountInfo {
        private String createBy;
        private String manager;
        private String businessName;
        private String businessOwnerName;
        private String businessNumber1;
        private String businessNumber2;
        private String faxNumber;
        private String address;
        private String tradePlace;
        private String note;
        private String level;
        private String tradeType;
        private String goldLoss;

        @Builder
        public accountInfo(String createBy, String manager, String businessName, String businessOwnerName, String businessNumber1, String businessNumber2, String faxNumber, String address, String tradePlace, String note, String level, String tradeType, String goldLoss) {
            this.createBy = createBy;
            this.manager = manager;
            this.businessName = businessName;
            this.businessOwnerName = businessOwnerName;
            this.businessNumber1 = businessNumber1;
            this.businessNumber2 = businessNumber2;
            this.faxNumber = faxNumber;
            this.address = address;
            this.tradePlace = tradePlace;
            this.note = note;
            this.level = level;
            this.tradeType = tradeType;
            this.goldLoss = goldLoss;
        }
    }

}
