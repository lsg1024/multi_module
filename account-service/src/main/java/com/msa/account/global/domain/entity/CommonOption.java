package com.msa.account.global.domain.entity;

import com.msa.account.global.domain.dto.CommonOptionDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

/**
 * Store(매장) 및 Factory(공장)의 거래 옵션 엔티티.
 *
 * *각 계정(Store/Factory)은 하나의 {@code CommonOption}을 가지며, 다음 정보를 포함한다:
 *
 *   - {@link GoldHarry} 연결 — 실시간 손모율 참조용 외래키
 *   - {@code goldHarryLoss} 문자열 사본 — 조인 없이 빠른 조회가 필요한 경우 사용
 *   - {@link OptionTradeType} — 거래 유형 (예: 매입/매출)
 *   - {@link OptionLevel} — 거래 등급
 * 
 *
 * *{@code goldHarryLoss} 필드는 {@link GoldHarry#getGoldHarryLoss()}의 사본으로,
 * 해리 손모율 변경 시 {@code UpdateGoldHarryLossBatchJob}에 의해 일괄 동기화된다.
 *
 * *소프트 삭제 방식을 사용한다 ({@code deleted = true}).
 */
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
    /**
     * {@link GoldHarry#getGoldHarryLoss()} 값의 문자열 사본.
     * 조인 없이 손모율을 빠르게 조회하기 위해 비정규화하여 보관한다.
     * 해리 손모율 변경 시 배치 잡에 의해 자동 동기화된다.
     */
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
