package com.msa.account.local.transaction_history.domain.entity;

import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
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
    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_TYPE")
    private SaleStatus transactionType;
    @Column(name = "GOLD_AMOUNT", precision = 10, scale = 3)
    private BigDecimal goldAmount;
    @Column(name = "MONEY_AMOUNT")
    private Long moneyAmount;
    @Column(name = "TRANSACTION_DELETED", nullable = false)
    private boolean transactionDeleted = false;

    @Column(name = "EVENT_ID", nullable = false, unique = true)
    private String eventId; // 멱등성 id 이자 판매 시 동시 등록되는 연결되는 값

    @Column(name = "ACCOUNT_SALE_CODE")
    private Long accountSaleCode; // 판매 주문장 id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STORE_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FACTORY_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Factory factory;

    @Column(name = "TRANSACTION_HISTORY_NOTE")
    private String transactionHistoryNote;


    @PrePersist
    void onCreate() {
        transactionDate = LocalDateTime.now();
    }

    @Builder
    public TransactionHistory(SaleStatus transactionType, BigDecimal goldAmount, Long moneyAmount, String eventId, Long accountSaleCode, Store store, Factory factory, String transactionHistoryNote) {
        this.transactionType = transactionType;
        this.goldAmount = goldAmount;
        this.moneyAmount = moneyAmount;
        this.eventId = eventId;
        this.accountSaleCode = accountSaleCode;
        this.store = store;
        this.factory = factory;
        this.transactionHistoryNote = transactionHistoryNote;
    }

    public void updateTransactionDate(LocalDateTime newTransactionDate) {
        this.transactionDate = newTransactionDate;
    }
}
