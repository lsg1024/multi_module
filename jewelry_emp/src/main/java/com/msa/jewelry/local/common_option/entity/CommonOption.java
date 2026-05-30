package com.msa.jewelry.local.common_option.entity;

import com.msa.jewelry.local.common_option.dto.CommonOptionDto;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Getter
@Entity
@Table(name = "COMMON_OPTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE COMMON_OPTION SET DELETED = TRUE WHERE COMMON_OPTION_ID = ?")
@Schema(description = "공통 거래 옵션 엔티티 — Store/Factory 가 1:1 로 보유하는 거래 유형/등급/금시세 정책")
public class CommonOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMMON_OPTION_ID")
    @Schema(description = "공통 옵션 PK", example = "1")
    private Long commonOptionId;
    @Column(name = "OPTION_TRADE_TYPE")
    @Enumerated(EnumType.STRING)
    @Schema(description = "거래 유형 (BUY/SELL)", example = "SELL")
    private OptionTradeType optionTradeType;
    @Column(name = "OPTION_LEVEL")
    @Enumerated(EnumType.STRING)
    @Schema(description = "거래처 등급 (A/B/C 등)", example = "A")
    private OptionLevel optionLevel;
    @Schema(description = "소프트 삭제 플래그", example = "false")
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GOLD_HARRY_ID", nullable = false)
    @Schema(description = "금시세 정책 (FK) — 실시간 손모율 참조용")
    private GoldHarry goldHarry;
    @Column(name = "GOLD_HARRY_LOSS")
    @Schema(description = "금 손모율(가공 시 손실 비율) 문자열 사본 — 배치로 GoldHarry 와 동기화", example = "1.5")
    private String goldHarryLoss;

    @Builder
    public CommonOption(OptionTradeType optionTradeType, OptionLevel optionLevel, GoldHarry goldHarry, String goldHarryLoss) {
        this.optionTradeType = optionTradeType;
        this.optionLevel = optionLevel;
        this.goldHarry = goldHarry;
        this.goldHarryLoss = goldHarryLoss;
    }

    public CommonOption(Long commonOptionId, GoldHarry goldHarry, String goldHarryLoss) {
        this.commonOptionId = commonOptionId;
        this.goldHarry = goldHarry;
        this.goldHarryLoss = goldHarryLoss;
    }

    public void setGoldHarry(GoldHarry goldHarry) {
        this.goldHarry = goldHarry;
    }

    public void updateTradeTypeAndOptionLevel(CommonOptionDto.CommonOptionInfo commonOptionInfo) {
        this.optionTradeType = OptionTradeType.valueOf(commonOptionInfo.getTradeType());
        this.optionLevel = OptionLevel.valueOf(commonOptionInfo.getGrade());
    }

    public void updateOptionLevel(String level) {
        this.optionLevel = OptionLevel.valueOf(level);
    }

    public void updateGoldHarry(GoldHarry newGoldHarry) {
        this.goldHarry = newGoldHarry;
        this.goldHarryLoss = newGoldHarry.getGoldHarryLoss().toString();
    }

    public void updateGoldHarryLoss(String updatedGoldHarryLoss) {
        this.goldHarryLoss = updatedGoldHarryLoss;
    }
}
