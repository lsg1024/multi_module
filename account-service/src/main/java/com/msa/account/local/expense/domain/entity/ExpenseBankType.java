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
@Table(name = "expense_bank_type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE expense_bank_type SET deleted = TRUE WHERE expense_bank_type_id = ?")
@SQLRestriction("deleted = FALSE")
public class ExpenseBankType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_bank_type_id")
    private Long expenseBankTypeId;

    @Column(name = "bank_type_name", nullable = false, length = 100)
    private String bankTypeName;

    @Column(name = "bank_type_note", length = 255)
    private String bankTypeNote;

    @Column(name = "deleted", nullable = false)
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
