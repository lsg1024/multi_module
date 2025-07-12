package com.msa.account.global.domain.entity;

import com.msa.account.global.domain.dto.CommonOptionDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

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
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
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

    public void update(CommonOptionDto.CommonOptionInfo commonOptionInfo) {
        this.optionTradeType = OptionTradeType.valueOf(commonOptionInfo.getTradeType());
        this.optionLevel = OptionLevel.valueOf(commonOptionInfo.getLevel());
    }

    public void updateGoldHarry(GoldHarry goldHarry) {
        this.goldHarry = goldHarry;
        this.goldHarryLoss = goldHarry.getGoldHarryLoss().toString();
    }
}
