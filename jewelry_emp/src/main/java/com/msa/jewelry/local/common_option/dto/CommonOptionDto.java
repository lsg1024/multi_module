package com.msa.jewelry.local.common_option.dto;

import com.msa.jewelry.local.common_option.entity.CommonOption;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import com.msa.jewelry.local.common_option.entity.OptionLevel;
import com.msa.jewelry.local.common_option.entity.OptionTradeType;
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
            OptionTradeType tradeType = OptionTradeType.fromInput(this.tradeType);
            OptionLevel level = OptionLevel.fromInput(this.grade);
            if (tradeType == null) {
                throw new IllegalArgumentException("지원하지 않는 거래 유형: " + this.tradeType);
            }
            if (level == null) {
                throw new IllegalArgumentException("지원하지 않는 등급: " + this.grade);
            }
            return CommonOption.builder()
                    .optionTradeType(tradeType)
                    .optionLevel(level)
                    .goldHarry(goldHarry)
                    .goldHarryLoss(goldHarry.getGoldHarryLoss().toString())
                    .build();
        }
    }

}
