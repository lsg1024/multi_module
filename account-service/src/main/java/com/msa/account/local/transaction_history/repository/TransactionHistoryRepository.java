package com.msa.account.local.transaction_history.repository;

import com.msa.account.local.transaction_history.domain.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long>, CustomTransactionHistoryRepository {
}
