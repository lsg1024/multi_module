package com.msa.jewelry.account.internal.transaction_history.domain.entity;

import com.msa.jewelry.account.internal.factory.domain.entity.Factory;
import com.msa.jewelry.account.internal.store.domain.entity.Store;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "거래 이력 엔티티 — 거래처/제조사 단위 매 거래(판매/결제/반품 등)의 발생 이력")
public class TransactionHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_ID")
    @Schema(description = "거래 이력 PK", example = "1001")
    private Long transactionId;
    @Column(name = "TRANSACTION_DATE", nullable = false)
    @Schema(description = "거래 발생 일시", example = "2026-05-16T14:30:00")
    private LocalDateTime transactionDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_TYPE")
    @Schema(description = "거래 유형 (SALE/PAYMENT/RETURN 등)", example = "SALE")
    private SaleStatus transactionType;
    @Column(name = "MATERIAL")
    @Schema(description = "재질 (14K, 18K, 24K 등)", example = "18K")
    private String material;
    @Column(name = "GOLD_AMOUNT", precision = 10, scale = 3)
    @Schema(description = "거래 금 수량(돈)", example = "3.333")
    private BigDecimal goldAmount;
    @Column(name = "MONEY_AMOUNT")
    @Schema(description = "거래 현금 금액(원)", example = "500000")
    private Long moneyAmount;
    @Column(name = "TRANSACTION_DELETED", nullable = false)
    @Schema(description = "소프트 삭제 플래그", example = "false")
    private boolean transactionDeleted = false;

    @Column(name = "EVENT_ID", nullable = false, unique = true)
    @Schema(description = "멱등성 키(UUID) — 판매 시 동시 등록되는 연결 값", example = "550e8400-e29b-41d4-a716-446655440000")
    private String eventId; // 멱등성 id 이자 판매 시 동시 등록되는 연결되는 값

    @Column(name = "ACCOUNT_SALE_CODE")
    @Schema(description = "판매 세션 코드 (TSID, 판매 주문장 ID)", example = "445823472384938240")
    private Long accountSaleCode; // 판매 주문장 id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STORE_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "거래처 (FK, 거래처 거래 시)")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FACTORY_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "제조사 (FK, 제조사 거래 시)")
    private Factory factory;

    @Column(name = "TRANSACTION_HISTORY_NOTE")
    @Schema(description = "거래 이력 비고", example = "결제 누락 후 입금 처리")
    private String transactionHistoryNote;

    @PrePersist
    void onCreate() {
        transactionDate = LocalDateTime.now();
    }

    @Builder
    public TransactionHistory(SaleStatus transactionType, String material, BigDecimal goldAmount, Long moneyAmount, String eventId, Long accountSaleCode, Store store, Factory factory, String transactionHistoryNote) {
        this.transactionType = transactionType;
        this.material = material;
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
