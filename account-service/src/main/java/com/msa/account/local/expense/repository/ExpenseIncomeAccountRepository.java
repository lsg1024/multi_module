package com.msa.account.local.expense.repository;

import com.msa.account.local.expense.domain.entity.ExpenseIncomeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseIncomeAccountRepository extends JpaRepository<ExpenseIncomeAccount, Long> {
    Optional<ExpenseIncomeAccount> findByIncomeAccountName(String incomeAccountName);

    List<ExpenseIncomeAccount> findAllByDeletedFalse();
}
