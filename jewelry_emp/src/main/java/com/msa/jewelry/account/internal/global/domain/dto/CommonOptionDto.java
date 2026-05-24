package com.msa.jewelry.account.internal.global.domain.dto;

import com.msa.jewelry.account.internal.global.domain.entity.CommonOption;
import com.msa.jewelry.account.internal.global.domain.entity.GoldHarry;
import com.msa.jewelry.account.internal.global.domain.entity.OptionLevel;
import com.msa.jewelry.account.internal.global.domain.entity.OptionTradeType;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "공통 거래 옵션 DTO 묶음")
public class CommonOptionDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "공통 거래 옵션 정보 — 거래 유형/등급/금시세 정책 ID")
    public static class CommonOptionInfo {
        @Schema(description = "거래 유형 (BUY/SELL)", example = "SELL")
        private String tradeType;
        @Schema(description = "거래처 등급 (A/B/C 등)", example = "A")
        private String grade;
        @Schema(description = "금시세 정책 ID (GoldHarry FK)", example = "1")
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
