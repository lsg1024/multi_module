package com.msa.account.local.transaction_history.domain.entity;

import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.store.domain.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "TRANSACTION_HISTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE TransactionHistory SET TRANSACTION_DELETED = TRUE WHERE TRANSACTION_ID = ?")
public class TransactionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_ID")
    private Long transactionId;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDateTime transactionDate;
    @Column(name = "TRANSACTION_TYPE")
    private String transactionType;
    @Column(name = "GOLD_AMOUNT", precision = 10, scale = 3)
    private BigDecimal goldAmount;
    @Column(name = "MONEY_AMOUNT")
    private Long moneyAmount;
    @Column(name = "TRANSACTION_DELETED", nullable = false)
    private boolean transactionDeleted = false;

    @Column(name = "EVENT_ID", nullable = false, unique = true)
    private String eventId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STORE_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FACTORY_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Factory factory;

    @PrePersist
    void onCreate() {
        transactionDate = LocalDateTime.now();
    }

    @Builder
    public TransactionHistory(String transactionType, BigDecimal goldAmount, Long moneyAmount, String eventId, Store store, Factory factory) {
        this.transactionType = transactionType;
        this.goldAmount = goldAmount;
        this.moneyAmount = moneyAmount;
        this.eventId = eventId;
        this.store = store;
        this.factory = factory;
    }
}
