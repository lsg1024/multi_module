package com.msa.account.global.domain.entity;

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
    @Column(name = "COMMON_OPTIONS_ID")
    private Long commonOptionId;
    @Column(name = "OPTION_TRADE_NOTE")
    private String optionTradeNote;
    @Enumerated(EnumType.STRING)
    private OptionTradeType optionTradeType;
    @Enumerated(EnumType.STRING)
    private OptionLevel optionLevel;
    private boolean deleted = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GOLD_ID", nullable = false)
    private GoldLoss goldLoss;

    @Builder
    public CommonOption(OptionTradeType optionTradeType, OptionLevel optionLevel) {
        this.optionTradeType = optionTradeType;
        this.optionLevel = optionLevel;
    }
}
