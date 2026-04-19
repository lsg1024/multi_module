package com.msa.account.local.transaction_history.domain.entity;

import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.store.domain.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 잔액 변동 추적 엔티티 (거래 원장 로그).
 *
 * *매 거래마다 거래 직전({@code previous}) 및 직후({@code after})의
 * 금(gold)·돈(money) 잔액 스냅샷을 저장한다.
 * {@code saleDate} 기준 시간순 정렬로 running balance를 유지하며,
 * 과거 거래 삽입 시 {@code SaleLogRebalanceJob}이 이후 레코드의 잔액을 일괄 재계산한다.
 *
 * *소유자 구분: {@code ownerType} 필드가 {@code "STORE"} 또는 {@code "FACTORY"} 값을 가지며,
 * 각각 {@link Store} 또는 {@link Factory}와 연관된다.
 *
 * *인덱스: {@code ACCOUNT_SALE_CODE}, {@code STORE_ID}, {@code FACTORY_ID}에
 * 각각 인덱스가 설정되어 있어 이력 조회 성능을 보장한다.
 */
@Getter
@Entity
@Table(
        name = "SALE_LOG",
        indexes = {
                @Index(name = "idx_sale_log_code", columnList = "ACCOUNT_SALE_CODE"),
                @Index(name = "idx_sale_log_store", columnList = "STORE_ID"),
                @Index(name = "idx_sale_log_factory", columnList = "FACTORY_ID")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_LOG_ID")
    private Long id;

    @Column(name = "ACCOUNT_SALE_CODE")
    private Long accountSaleCode;

    @Column(name = "PREVIOUS_GOLD_BALANCE", precision = 10, scale = 3)
    private BigDecimal previousGoldBalance = BigDecimal.ZERO;

    @Column(name = "PREVIOUS_MONEY_BALANCE")
    private Long previousMoneyBalance = 0L;

    @Column(name = "AFTER_GOLD_BALANCE", precision = 10, scale = 3)
    private BigDecimal afterGoldBalance = BigDecimal.ZERO;

    @Column(name = "AFTER_MONEY_BALANCE")
    private Long afterMoneyBalance = 0L;

    @Column(name = "SALE_DATE")
    private LocalDateTime saleDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STORE_ID")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FACTORY_ID")
    private Factory factory;

    /** 잔액 소유자 구분. "STORE" 또는 "FACTORY" 값을 가진다. */
    @Column(name = "OWNER_TYPE")
    private String ownerType;

    @Builder
    public SaleLog(Long accountSaleCode, BigDecimal previousGoldBalance, Long previousMoneyBalance,
                   BigDecimal afterGoldBalance, Long afterMoneyBalance, LocalDateTime saleDate,
                   Store store, Factory factory, String ownerType) {

        this.accountSaleCode = accountSaleCode;
        this.previousGoldBalance = previousGoldBalance;
        this.previousMoneyBalance = previousMoneyBalance;
        this.afterGoldBalance = afterGoldBalance;
        this.afterMoneyBalance = afterMoneyBalance;
        this.saleDate = saleDate;
        this.store = store;
        this.factory = factory;
        this.ownerType = ownerType;
    }

    public void updateBalance(BigDecimal prevGold, Long prevMoney, BigDecimal afterGold, Long afterMoney) {
        this.previousGoldBalance = prevGold;
        this.previousMoneyBalance = prevMoney;
        this.afterGoldBalance = afterGold;
        this.afterMoneyBalance = afterMoney;
    }
}