package com.msa.jewelry.local.transaction_history.entity;

import com.msa.common.global.domain.BaseEntity;
import com.msa.jewelry.local.factory.entity.Factory;
import com.msa.jewelry.local.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "BALANCE_HISTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "미수 잔액 변경 불변 이력 — before/after 스냅샷 + 델타 + 사유 + 멱등키")
public class BalanceHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BALANCE_HISTORY_ID")
    @Schema(description = "이력 PK", example = "1001")
    private Long balanceHistoryId;

    @Column(name = "OWNER_TYPE", nullable = false, length = 20)
    @Schema(description = "잔액 소유 주체 (STORE/FACTORY)", example = "FACTORY")
    private String ownerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STORE_ID")
    @Schema(description = "거래처 (STORE 잔액 변경 시)")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FACTORY_ID")
    @Schema(description = "제조사 (FACTORY 잔액 변경 시)")
    private Factory factory;

    @Column(name = "BEFORE_GOLD_BALANCE", nullable = false, precision = 10, scale = 3)
    @Schema(description = "변경 전 금 잔액(g)", example = "12.345")
    private BigDecimal beforeGoldBalance = BigDecimal.ZERO;

    @Column(name = "AFTER_GOLD_BALANCE", nullable = false, precision = 10, scale = 3)
    @Schema(description = "변경 후 금 잔액(g)", example = "15.678")
    private BigDecimal afterGoldBalance = BigDecimal.ZERO;

    @Column(name = "BEFORE_MONEY_BALANCE", nullable = false)
    @Schema(description = "변경 전 현금 잔액(원)", example = "1000000")
    private Long beforeMoneyBalance = 0L;

    @Column(name = "AFTER_MONEY_BALANCE", nullable = false)
    @Schema(description = "변경 후 현금 잔액(원)", example = "1500000")
    private Long afterMoneyBalance = 0L;

    @Column(name = "DELTA_GOLD", nullable = false, precision = 10, scale = 3)
    @Schema(description = "금 변동량(g)", example = "3.333")
    private BigDecimal deltaGold = BigDecimal.ZERO;

    @Column(name = "DELTA_MONEY", nullable = false)
    @Schema(description = "현금 변동량(원)", example = "500000")
    private Long deltaMoney = 0L;

    @Column(name = "REASON", nullable = false, length = 30)
    @Schema(description = "변경 사유 (SALE/PAYMENT/RETURN/... + OPENING/REVERSAL)", example = "SALE")
    private String reason;

    @Column(name = "EVENT_ID", nullable = false, unique = true, length = 100)
    @Schema(description = "멱등키 — 동일 이벤트 중복 적용 차단", example = "550e8400-e29b-41d4-a716-446655440000")
    private String eventId;

    @Column(name = "ACCOUNT_SALE_CODE")
    @Schema(description = "판매 주문장 참조 (TSID, 있을 때)", example = "445823472384938240")
    private Long accountSaleCode;

    @Column(name = "NOTE")
    @Schema(description = "비고", example = "판매 등록")
    private String note;

    @Builder
    public BalanceHistory(String ownerType, Store store, Factory factory,
                          BigDecimal beforeGoldBalance, BigDecimal afterGoldBalance,
                          Long beforeMoneyBalance, Long afterMoneyBalance,
                          BigDecimal deltaGold, Long deltaMoney,
                          String reason, String eventId, Long accountSaleCode,
                          String note) {
        this.ownerType = ownerType;
        this.store = store;
        this.factory = factory;
        this.beforeGoldBalance = beforeGoldBalance != null ? beforeGoldBalance : BigDecimal.ZERO;
        this.afterGoldBalance = afterGoldBalance != null ? afterGoldBalance : BigDecimal.ZERO;
        this.beforeMoneyBalance = beforeMoneyBalance != null ? beforeMoneyBalance : 0L;
        this.afterMoneyBalance = afterMoneyBalance != null ? afterMoneyBalance : 0L;
        this.deltaGold = deltaGold != null ? deltaGold : BigDecimal.ZERO;
        this.deltaMoney = deltaMoney != null ? deltaMoney : 0L;
        this.reason = reason;
        this.eventId = eventId;
        this.accountSaleCode = accountSaleCode;
        this.note = note;
    }
}
