package com.msa.account.local.ledger.domain.entity;

import com.msa.common.global.domain.BaseTimeEntity;
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
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LEDGER_ID")
    private Long ledgerId;

    @Column(name = "LEDGER_DATE", nullable = false)
    private LocalDate ledgerDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ASSET_TYPE", nullable = false, length = 10)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_TYPE", nullable = false, length = 10)
    private TransactionType transactionType;

    @Column(name = "GOLD_AMOUNT", precision = 10, scale = 3)
    private BigDecimal goldAmount;

    @Column(name = "MONEY_AMOUNT")
    private Long moneyAmount;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @Column(name = "CREATED_BY", updatable = false)
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
