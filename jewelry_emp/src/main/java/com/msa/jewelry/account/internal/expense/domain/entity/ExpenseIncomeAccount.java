package com.msa.jewelry.account.internal.expense.domain.entity;

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
@Table(name = "expense_income_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE expense_income_account SET deleted = TRUE WHERE expense_income_account_id = ?")
@SQLRestriction("deleted = FALSE")
@Schema(description = "수입 계정 엔티티 — 지출/수입 기록 시 수입을 분류하는 계정 마스터")
public class ExpenseIncomeAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_income_account_id")
    @Schema(description = "수입 계정 PK", example = "1")
    private Long expenseIncomeAccountId;

    @Column(name = "income_account_name", nullable = false, length = 100)
    @Schema(description = "수입 계정명", example = "판매수입")
    private String incomeAccountName;

    @Column(name = "income_account_note", length = 255)
    @Schema(description = "수입 계정 비고", example = "일반 판매로 인한 수입")
    private String incomeAccountNote;

    @Column(name = "deleted", nullable = false)
    @Schema(description = "소프트 삭제 플래그", example = "false")
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
