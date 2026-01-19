package com.msa.order.global.kafka.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        private String material;
        private BigDecimal pureGoldBalance;
        private Integer moneyBalance;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS")
        private LocalDateTime SaleDate;

        @Builder
        public updateCurrentBalance(String eventId, String saleCode, String tenantId, String saleType, String type, Long id, String name, String material, BigDecimal pureGoldBalance, Integer moneyBalance, LocalDateTime saleDate) {
            this.eventId = eventId;
            this.saleCode = saleCode;
            this.tenantId = tenantId;
            this.saleType = saleType;
            this.type = type;
            this.id = id;
            this.name = name;
            this.material = material.toUpperCase();
            this.pureGoldBalance = pureGoldBalance;
            this.moneyBalance = moneyBalance;
            this.SaleDate = saleDate;
        }
    }
}
