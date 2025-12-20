package com.msa.product.local.goldPrice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "GOLD")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gold {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GOLD_ID")
    private Long goldId;

    @Column(name = "GOLD_PRICE")
    private Integer goldPrice;

    @Builder
    public Gold(Long goldId, Integer goldPrice) {
        this.goldId = goldId;
        this.goldPrice = goldPrice;
    }
}
