package com.msa.account.local.transaction_history.repository;

import com.msa.account.local.transaction_history.domain.dto.TransactionPage;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

public interface CustomTransactionHistoryRepository {
    CustomPage<TransactionPage> findTransactionHistory(String start, String end, String accountType, String accountName, Pageable pageable);
    CustomPage<TransactionPage> findTransactionHistoryFactory(String start, String end, String accountType, String accountName, Pageable pageable);
}
