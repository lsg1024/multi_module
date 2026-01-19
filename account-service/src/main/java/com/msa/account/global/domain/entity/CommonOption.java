package com.msa.account.global.domain.entity;

import com.msa.account.global.domain.dto.CommonOptionDto;
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
public class CommonOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMMON_OPTION_ID")
    private Long commonOptionId;
    @Column(name = "OPTION_TRADE_TYPE")
    @Enumerated(EnumType.STRING)
    private OptionTradeType optionTradeType;
    @Column(name = "OPTION_LEVEL")
    @Enumerated(EnumType.STRING)
    private OptionLevel optionLevel;
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GOLD_HARRY_ID", nullable = false)
    private GoldHarry goldHarry;
    @Column(name = "GOLD_HARRY_LOSS")
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
