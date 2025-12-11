package com.msa.account.local.transaction_history.repository;

import com.msa.account.local.transaction_history.domain.dto.TransactionPage;
import com.msa.common.global.util.CustomPage;

public interface CustomTransactionHistoryRepository {
    CustomPage<TransactionPage> findTranscationHistory(String start, String end, String accountType, String accountName);
}
