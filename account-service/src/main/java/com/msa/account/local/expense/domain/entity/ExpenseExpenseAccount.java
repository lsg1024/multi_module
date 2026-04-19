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
@Table(name = "expense_expense_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE expense_expense_account SET deleted = TRUE WHERE expense_expense_account_id = ?")
@SQLRestriction("deleted = FALSE")
public class ExpenseExpenseAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_expense_account_id")
    private Long expenseExpenseAccountId;

    @Column(name = "expense_account_name", nullable = false, length = 100)
    private String expenseAccountName;

    @Column(name = "expense_account_note", length = 255)
    private String expenseAccountNote;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Builder
    public ExpenseExpenseAccount(String expenseAccountName, String expenseAccountNote) {
        this.expenseAccountName = expenseAccountName;
        this.expenseAccountNote = expenseAccountNote;
        this.deleted = false;
    }

    public void update(String expenseAccountName, String expenseAccountNote) {
        this.expenseAccountName = expenseAccountName;
        this.expenseAccountNote = expenseAccountNote;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
