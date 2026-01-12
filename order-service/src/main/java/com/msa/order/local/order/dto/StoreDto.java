package com.msa.order.local.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StoreDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private Long storeId;
        private String storeName;
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private Long storeId;
        private String storeName;
        private String grade;
        private String storeHarry;

        @Builder
        public Response(Long storeId, String storeName, String grade, String storeHarry) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.grade = grade;
            this.storeHarry = storeHarry;
        }

    }

    @Getter
    @NoArgsConstructor
    public static class accountResponse {
        private Long accountId;
        private String accountName;
        private String tradeType;
        private String grade;
        private String goldHarryLoss;
        private String previousGoldBalance;
        private String previousMoneyBalance;
        private String afterGoldBalance;
        private String afterMoneyBalance;
        private String lastSaleDate;
        private String lastPaymentDate;
        private String businessOwnerName;
        private String businessOwnerNumber;
        private String businessNumber1;
        private String businessNumber2;
        private String faxNumber;
        private String address;
        private String note;

        @Override
        public String toString() {
            return "accountResponse{" +
                    "accountId=" + accountId +
                    ", accountName='" + accountName + '\'' +
                    ", tradeType='" + tradeType + '\'' +
                    ", grade='" + grade + '\'' +
                    ", goldHarryLoss='" + goldHarryLoss + '\'' +
                    ", previousGoldBalance='" + previousGoldBalance + '\'' +
                    ", previousMoneyBalance='" + previousMoneyBalance + '\'' +
                    ", afterGoldBalance='" + afterGoldBalance + '\'' +
                    ", afterMoneyBalance='" + afterMoneyBalance + '\'' +
                    ", lastSaleDate='" + lastSaleDate + '\'' +
                    ", lastPaymentDate='" + lastPaymentDate + '\'' +
                    ", businessOwnerName='" + businessOwnerName + '\'' +
                    ", businessOwnerNumber='" + businessOwnerNumber + '\'' +
                    ", businessNumber1='" + businessNumber1 + '\'' +
                    ", businessNumber2='" + businessNumber2 + '\'' +
                    ", faxNumber='" + faxNumber + '\'' +
                    ", address='" + address + '\'' +
                    ", note='" + note + '\'' +
                    '}';
        }
    }
}
