package com.msa.account.global.kafka.dto;

import com.msa.account.global.domain.dto.GoldHarryDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class KafkaEventDto {

    @Getter
    @NoArgsConstructor
    public static class UpdateLossDto {
        private String tenantId;
        private Long goldHarryId;
        private GoldHarryDto.Update goldHarryDto;

        @Builder
        public UpdateLossDto(String tenantId, Long goldHarryId, GoldHarryDto.Update goldHarryDto) {
            this.tenantId = tenantId;
            this.goldHarryId = goldHarryId;
            this.goldHarryDto = goldHarryDto;
        }
    }

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
        private String pureGoldBalance;
        private Integer moneyBalance;
    }
}
