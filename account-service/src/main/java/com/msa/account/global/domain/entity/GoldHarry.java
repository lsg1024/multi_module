package com.msa.account.global.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * 해리(손모율) 엔티티.
 *
 * *금 거래 시 녹이거나 정제하는 과정에서 발생하는 손실률을 나타낸다.
 * 예를 들어 {@code goldHarryLoss = 1.05}이면 5% 손모를 의미한다.
 *
 * *소프트 삭제({@code deleted = true}) 방식을 사용하며, 삭제된 해리를 참조하는
 * {@link CommonOption}은 배치 잡({@code DeleteGoldHarryBatchJob})에 의해
 * 기본 해리(ID=1)로 자동 대체된다.
 *
 * *{@code DefaultOption = true}인 레코드는 시스템 기본 해리로 취급된다.
 */
@Slf4j
@Getter
@Entity
@Table(name = "GOLD_HARRY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE GOLD_HARRY SET DELETED = TRUE WHERE HARRY_ID = ?")
public class GoldHarry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GOLD_HARRY_ID")
    private Long goldHarryId;
    @Column(name = "GOLD_HARRY_LOSS", nullable = false, precision = 10, scale = 2)
    private BigDecimal goldHarryLoss;
    /** 시스템 기본 해리 여부. 해리 삭제 시 대체 대상으로 사용되는 기준값(ID=1)에 해당하면 {@code true}. */
    @Column(name = "DEFAULT_OPTION", nullable = false)
    private boolean DefaultOption = false;
    private boolean deleted = false;

    @Builder
    public GoldHarry(Long goldHarryId, BigDecimal goldHarryLoss) {
        this.goldHarryId = goldHarryId;
        this.goldHarryLoss = goldHarryLoss;
    }

    public BigDecimal getGoldHarryLoss() {
        return goldHarryLoss;
    }
    public boolean getDefaultOption() {
        return this.DefaultOption;
    }

    public void updateLoss(String newLoss) {
        this.goldHarryLoss = new BigDecimal(newLoss);
    }
}
