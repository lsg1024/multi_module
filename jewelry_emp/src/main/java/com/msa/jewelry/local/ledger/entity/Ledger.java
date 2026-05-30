package com.msa.jewelry.local.ledger.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "LEDGER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "장부 엔티티 — 일자별 자산(금/현금) 입출 내역")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LEDGER_ID")
    @Schema(description = "장부 PK", example = "1001")
    private Long ledgerId;

    @Column(name = "LEDGER_DATE", nullable = false)
    @Schema(description = "장부 기록일", example = "2026-05-16")
    private LocalDate ledgerDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ASSET_TYPE", nullable = false, length = 10)
    @Schema(description = "자산 유형 (GOLD/MONEY)", example = "GOLD")
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_TYPE", nullable = false, length = 10)
    @Schema(description = "거래 유형 (입금/출금 등)", example = "DEPOSIT")
    private TransactionType transactionType;

    @Column(name = "GOLD_AMOUNT", precision = 10, scale = 3)
    @Schema(description = "금 금액(g)", example = "12.345")
    private BigDecimal goldAmount;

    @Column(name = "MONEY_AMOUNT")
    @Schema(description = "현금 금액(원)", example = "1500000")
    private Long moneyAmount;

    @Column(name = "DESCRIPTION", length = 255)
    @Schema(description = "장부 설명/적요", example = "금 시세 차익 입고")
    private String description;

    @Column(name = "CREATED_BY", updatable = false)
    @Schema(description = "기록 생성자", example = "admin")
    private String createdBy;

    @Builder
    public Ledger(LocalDate ledgerDate, AssetType assetType, TransactionType transactionType,
                  BigDecimal goldAmount, Long moneyAmount, String description, String createdBy) {
        this.ledgerDate = ledgerDate;
        this.assetType = assetType;
        this.transactionType = transactionType;
        this.goldAmount = goldAmount;
        this.moneyAmount = moneyAmount;
        this.description = description;
        this.createdBy = createdBy;
    }

    public void update(LocalDate ledgerDate, TransactionType transactionType,
                       BigDecimal goldAmount, Long moneyAmount, String description) {
        this.ledgerDate = ledgerDate;
        this.transactionType = transactionType;
        this.goldAmount = goldAmount;
        this.moneyAmount = moneyAmount;
        this.description = description;
    }
}
