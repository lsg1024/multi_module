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
@Table(name = "expense_bank_type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE expense_bank_type SET deleted = TRUE WHERE expense_bank_type_id = ?")
@SQLRestriction("deleted = FALSE")
@Schema(description = "지출 은행 유형 엔티티 — 지출 기록 시 사용하는 은행/계좌 유형 마스터")
public class ExpenseBankType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_bank_type_id")
    @Schema(description = "지출 은행 유형 PK", example = "1")
    private Long expenseBankTypeId;

    @Column(name = "bank_type_name", nullable = false, length = 100)
    @Schema(description = "은행 유형명", example = "국민은행")
    private String bankTypeName;

    @Column(name = "bank_type_note", length = 255)
    @Schema(description = "은행 유형 비고", example = "법인 운영 계좌")
    private String bankTypeNote;

    @Column(name = "deleted", nullable = false)
    @Schema(description = "소프트 삭제 플래그", example = "false")
    private Boolean deleted = false;

    @Builder
    public ExpenseBankType(String bankTypeName, String bankTypeNote) {
        this.bankTypeName = bankTypeName;
        this.bankTypeNote = bankTypeNote;
        this.deleted = false;
    }

    public void update(String bankTypeName, String bankTypeNote) {
        this.bankTypeName = bankTypeName;
        this.bankTypeNote = bankTypeNote;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
