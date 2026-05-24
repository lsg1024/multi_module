package com.msa.jewelry.local.expense.repository;

import com.msa.jewelry.local.expense.entity.ExpenseExpenseAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseExpenseAccountRepository extends JpaRepository<ExpenseExpenseAccount, Long> {
    Optional<ExpenseExpenseAccount> findByExpenseAccountName(String expenseAccountName);

    List<ExpenseExpenseAccount> findAllByDeletedFalse();
}
