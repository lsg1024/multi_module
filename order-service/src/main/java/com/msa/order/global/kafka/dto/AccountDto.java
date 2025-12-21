package com.msa.order.global.kafka.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class AccountDto {

    @Getter
    @NoArgsConstructor
    public static class updateCurrentBalance {
        private String eventId;
        private String saleCode;
        private String tenantId;
        private String saleType; // sale or 결제...
        private String type; // store or factory
        private Long id;
        private String name;
        private BigDecimal pureGoldBalance;
        private Integer moneyBalance;

        @Builder
        public updateCurrentBalance(String eventId, String saleCode, String tenantId, String saleType, String type, Long id, String name, BigDecimal pureGoldBalance, Integer moneyBalance) {
            this.eventId = eventId;
            this.saleCode = saleCode;
            this.tenantId = tenantId;
            this.saleType = saleType;
            this.type = type;
            this.id = id;
            this.name = name;
            this.pureGoldBalance = pureGoldBalance;
            this.moneyBalance = moneyBalance;
        }
    }
}
