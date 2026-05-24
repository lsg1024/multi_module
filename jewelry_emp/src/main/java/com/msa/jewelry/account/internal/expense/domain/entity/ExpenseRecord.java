package com.msa.jewelry.account.internal.expense.domain.entity;

import com.msa.common.global.common_enum.expense_enum.ExpenseType;
import com.msa.common.global.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "expense_record")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE expense_record SET deleted = TRUE WHERE expense_record_id = ?")
@SQLRestriction("deleted = FALSE")
@Schema(description = "지출/수입 기록 엔티티 — 일자별 거래처 단위 지출/수입 내역")
public class ExpenseRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_record_id")
    @Schema(description = "지출 기록 PK", example = "1001")
    private Long expenseRecordId;

    @Column(name = "record_date", nullable = false)
    @Schema(description = "지출/수입 기록 일시", example = "2026-05-16T10:30:00")
    private LocalDateTime recordDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 20)
    @Schema(description = "지출 유형 (수입/지출 구분)", example = "EXPENSE")
    private ExpenseType expenseType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_bank_type_id")
    @Schema(description = "지출 은행 유형 (FK)")
    private ExpenseBankType bankType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_income_account_id")
    @Schema(description = "수입 계정 (FK)")
    private ExpenseIncomeAccount incomeAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_expense_account_id")
    @Schema(description = "지출 계정 (FK)")
    private ExpenseExpenseAccount expenseAccount;

    @Column(name = "counterparty", length = 100)
    @Schema(description = "거래 상대방 (거래처/공급처 이름)", example = "ABC상사")
    private String counterparty;

    @Column(name = "description", length = 500)
    @Schema(description = "지출/수입 적요/설명", example = "월세 정산")
    private String description;

    @Column(name = "material", length = 50)
    @Schema(description = "재질 (14K, 18K, 24K 등)", example = "18K")
    private String material;

    @Column(name = "weight", precision = 10, scale = 3)
    @Schema(description = "중량(g)", example = "12.345")
    private BigDecimal weight;

    @Column(name = "quantity")
    @Schema(description = "수량", example = "1")
    private Integer quantity = 1;

    @Column(name = "unit_price")
    @Schema(description = "단가", example = "100000")
    private Long unitPrice = 0L;

    @Column(name = "supply_amount")
    @Schema(description = "공급가액", example = "1000000")
    private Long supplyAmount = 0L;

    @Column(name = "tax_amount")
    @Schema(description = "부가세액", example = "100000")
    private Long taxAmount = 0L;

    @Column(name = "deleted", nullable = false)
    @Schema(description = "소프트 삭제 플래그", example = "false")
    private Boolean deleted = false;

    @Builder
    public ExpenseRecord(LocalDateTime recordDate,
                        ExpenseType expenseType,
                        ExpenseBankType bankType,
                        ExpenseIncomeAccount incomeAccount,
                        ExpenseExpenseAccount expenseAccount,
                        String counterparty,
                        String description,
                        String material,
                        BigDecimal weight,
                        Integer quantity,
                        Long unitPrice,
                        Long supplyAmount,
                        Long taxAmount) {
        this.recordDate = recordDate;
        this.expenseType = expenseType;
        this.bankType = bankType;
        this.incomeAccount = incomeAccount;
        this.expenseAccount = expenseAccount;
        this.counterparty = counterparty;
        this.description = description;
        this.material = material;
        this.weight = weight;
        this.quantity = quantity != null ? quantity : 1;
        this.unitPrice = unitPrice != null ? unitPrice : 0L;
        this.supplyAmount = supplyAmount != null ? supplyAmount : 0L;
        this.taxAmount = taxAmount != null ? taxAmount : 0L;
        this.deleted = false;
    }

    public void update(LocalDateTime recordDate,
                      ExpenseType expenseType,
                      ExpenseBankType bankType,
                      ExpenseIncomeAccount incomeAccount,
                      ExpenseExpenseAccount expenseAccount,
                      String counterparty,
                      String description,
                      String material,
                      BigDecimal weight,
                      Integer quantity,
                      Long unitPrice,
                      Long supplyAmount,
                      Long taxAmount) {
        this.recordDate = recordDate;
        this.expenseType = expenseType;
        this.bankType = bankType;
        this.incomeAccount = incomeAccount;
        this.expenseAccount = expenseAccount;
        this.counterparty = counterparty;
        this.description = description;
        this.material = material;
        this.weight = weight;
        this.quantity = quantity != null ? quantity : 1;
        this.unitPrice = unitPrice != null ? unitPrice : 0L;
        this.supplyAmount = supplyAmount != null ? supplyAmount : 0L;
        this.taxAmount = taxAmount != null ? taxAmount : 0L;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
