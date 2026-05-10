package com.msa.jewelry.account.internal.transaction_history.repository;

import com.msa.jewelry.account.internal.transaction_history.domain.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long>, CustomTransactionHistoryRepository {
    boolean existsByEventIdAndStore_StoreId(String eventId, Long storeId);
    boolean existsByEventIdAndFactory_FactoryId(String eventId, Long storeId);
}
