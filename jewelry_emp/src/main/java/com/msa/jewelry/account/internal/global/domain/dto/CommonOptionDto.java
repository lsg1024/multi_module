package com.msa.jewelry.account.internal.global.domain.dto;

import com.msa.jewelry.account.internal.global.domain.entity.CommonOption;
import com.msa.jewelry.account.internal.global.domain.entity.GoldHarry;
import com.msa.jewelry.account.internal.global.domain.entity.OptionLevel;
import com.msa.jewelry.account.internal.global.domain.entity.OptionTradeType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CommonOptionDto {

    @Getter
    @NoArgsConstructor
    public static class CommonOptionInfo {
        private String tradeType;
        private String grade;
        private String goldHarryId;

        @Builder
        @QueryProjection
        public CommonOptionInfo(String tradeType, String grade, String goldHarryId) {
            this.tradeType = tradeType;
            this.grade = grade;
            this.goldHarryId = goldHarryId;
        }

        public CommonOption toEntity(GoldHarry goldHarry) {
            return CommonOption.builder()
                    .optionTradeType(OptionTradeType.valueOf(this.tradeType))
                    .optionLevel(OptionLevel.valueOf(this.grade))
                    .goldHarry(goldHarry)
                    .goldHarryLoss(goldHarry.getGoldHarryLoss().toString())
                    .build();
        }
    }

}
