package com.msa.account.global.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

@Entity
@Table(name = "GOLD_HARRY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE GOLD_HARRY SET DELETED = TRUE WHERE HARRY_ID = ?")
public class GoldHarry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GOLD_HARRY_ID")
    private Long goldHarryId;
    @Column(name = "GOLD_HARRY_LOSS")
    private BigDecimal goldHarryLoss;
    private boolean deleted = false;

    @Builder
    public GoldHarry(BigDecimal goldHarryLoss) {
        this.goldHarryLoss = goldHarryLoss;
    }

    public BigDecimal getGoldHarryLoss() {
        return goldHarryLoss;
    }
}
