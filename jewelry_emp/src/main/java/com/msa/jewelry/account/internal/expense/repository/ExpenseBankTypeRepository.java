package com.msa.jewelry.account.internal.expense.repository;

import com.msa.jewelry.account.internal.expense.domain.entity.ExpenseBankType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseBankTypeRepository extends JpaRepository<ExpenseBankType, Long> {
    Optional<ExpenseBankType> findByBankTypeName(String bankTypeName);

    List<ExpenseBankType> findAllByDeletedFalse();
}
