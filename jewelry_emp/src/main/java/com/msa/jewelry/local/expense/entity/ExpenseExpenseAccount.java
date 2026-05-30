package com.msa.jewelry.local.expense.entity;

import com.msa.common.global.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "지출 계정 엔티티 — 지출 기록을 분류하는 계정 마스터")
public class ExpenseExpenseAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_expense_account_id")
    @Schema(description = "지출 계정 PK", example = "1")
    private Long expenseExpenseAccountId;

    @Column(name = "expense_account_name", nullable = false, length = 100)
    @Schema(description = "지출 계정명", example = "사무용품비")
    private String expenseAccountName;

    @Column(name = "expense_account_note", length = 255)
    @Schema(description = "지출 계정 비고", example = "운영 잡비 카테고리")
    private String expenseAccountNote;

    @Column(name = "deleted", nullable = false)
    @Schema(description = "소프트 삭제 플래그", example = "false")
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
