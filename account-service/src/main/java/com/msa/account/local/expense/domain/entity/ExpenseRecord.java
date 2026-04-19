package com.msa.account.local.expense.domain.entity;

import com.msa.common.global.common_enum.expense_enum.ExpenseType;
import com.msa.common.global.domain.BaseEntity;
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
public class ExpenseRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_record_id")
    private Long expenseRecordId;

    @Column(name = "record_date", nullable = false)
    private LocalDateTime recordDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 20)
    private ExpenseType expenseType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_bank_type_id")
    private ExpenseBankType bankType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_income_account_id")
    private ExpenseIncomeAccount incomeAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_expense_account_id")
    private ExpenseExpenseAccount expenseAccount;

    @Column(name = "counterparty", length = 100)
    private String counterparty;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "material", length = 50)
    private String material;

    @Column(name = "weight", precision = 10, scale = 3)
    private BigDecimal weight;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "unit_price")
    private Long unitPrice = 0L;

    @Column(name = "supply_amount")
    private Long supplyAmount = 0L;

    @Column(name = "tax_amount")
    private Long taxAmount = 0L;

    @Column(name = "deleted", nullable = false)
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
