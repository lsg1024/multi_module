package com.msa.account.local.transaction.repository;

import com.msa.account.local.transaction.domain.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
}
