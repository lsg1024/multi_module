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