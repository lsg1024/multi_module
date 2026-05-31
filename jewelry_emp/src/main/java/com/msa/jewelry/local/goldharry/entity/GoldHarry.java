package com.msa.jewelry.local.goldharry.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Getter
@Entity
@Table(name = "GOLD_HARRY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "금시세 정책(해리/손모율) 엔티티 — 테넌트별 가공 시 금 손실 비율 마스터")
public class GoldHarry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GOLD_HARRY_ID")
    @Schema(description = "금시세 정책 PK", example = "1")
    private Long goldHarryId;
    @Column(name = "GOLD_HARRY_LOSS", nullable = false, precision = 10, scale = 2)
    @Schema(description = "금 손모율 — 가공 시 손실되는 금 비율 (예: 1.05 = 5% 손모)", example = "1.05")
    private BigDecimal goldHarryLoss;
    @Column(name = "DEFAULT_OPTION", nullable = false)
    @Schema(description = "시스템 기본 해리 여부 — 해리 삭제 시 대체 대상으로 사용", example = "true")
    private boolean DefaultOption = false;

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
