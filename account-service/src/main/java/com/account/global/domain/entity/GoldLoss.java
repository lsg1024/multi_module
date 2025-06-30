package com.account.global.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

@Entity
@Table(name = "GOLD_LOSS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE GOLD_LOSS SET DELETED = TRUE WHERE GOLD_ID = ?")
public class GoldLoss {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GOLD_ID")
    private Long goldId;
    @Column(name = "LOSS")
    private BigDecimal loss;

    @Builder
    public GoldLoss(BigDecimal loss) {
        this.loss = loss;
    }
}
