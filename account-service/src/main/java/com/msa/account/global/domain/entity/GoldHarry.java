package com.msa.account.global.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    @Column(name = "GOLD_HARRY_LOSS")
    private BigDecimal goldHarryLoss;
    @Column(name = "DEFAULT_OPTION")
    private boolean DefaultOption;
    private boolean deleted = false;

    @Builder
    public GoldHarry(Long goldHarryId, BigDecimal goldHarryLoss) {
        this.goldHarryId = goldHarryId;
        this.goldHarryLoss = goldHarryLoss;
    }

    @OneToMany(mappedBy = "goldHarry", cascade = CascadeType.ALL)
    private List<CommonOption> commonOptions = new ArrayList<>();

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
