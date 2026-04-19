package com.msa.account.local.expense.domain.entity;

import com.msa.common.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "expense_income_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE expense_income_account SET deleted = TRUE WHERE expense_income_account_id = ?")
@SQLRestriction("deleted = FALSE")
public class ExpenseIncomeAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_income_account_id")
    private Long expenseIncomeAccountId;

    @Column(name = "income_account_name", nullable = false, length = 100)
    private String incomeAccountName;

    @Column(name = "income_account_note", length = 255)
    private String incomeAccountNote;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Builder
    public ExpenseIncomeAccount(String incomeAccountName, String incomeAccountNote) {
        this.incomeAccountName = incomeAccountName;
        this.incomeAccountNote = incomeAccountNote;
        this.deleted = false;
    }

    public void update(String incomeAccountName, String incomeAccountNote) {
        this.incomeAccountName = incomeAccountName;
        this.incomeAccountNote = incomeAccountNote;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
