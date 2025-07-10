package com.msa.account.global.domain.dto;

import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.entity.OptionLevel;
import com.msa.account.global.domain.entity.OptionTradeType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CommonOptionDto {

    @Getter
    @NoArgsConstructor
    public static class CommonOptionInfo {
        private String tradeType;
        private String level;
        private String goldHarryId;
        private String goldHarryLoss;

        @Builder
        @QueryProjection
        public CommonOptionInfo(String tradeType, String level, String goldHarryId, String goldHarryLoss) {
            this.tradeType = tradeType;
            this.level = level;
            this.goldHarryId = goldHarryId;
            this.goldHarryLoss = goldHarryLoss;
        }

        public CommonOption toEntity(GoldHarry goldHarry) {
            return CommonOption.builder()
                    .optionTradeType(OptionTradeType.valueOf(this.tradeType))
                    .optionLevel(OptionLevel.valueOf(this.level))
                    .goldHarry(goldHarry)
                    .goldHarryLoss(goldHarry.getGoldHarryLoss().toString())
                    .build();
        }
    }

}
