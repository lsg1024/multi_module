package com.msa.account.local.transaction_history.repository;

import com.msa.account.local.transaction_history.domain.dto.TransactionPage;
import com.msa.common.global.util.CustomPage;
import com.querydsl.jpa.impl.JPAQueryFactory;

public class TransactionHistoryRepositoryImpl extends CustomTransactionHistoryRepository{

    private final JPAQueryFactory

    @Override
    public CustomPage<TransactionPage> findTranscationHistory(String start, String end, String accountType, String accountName) {
        return null;
    }
}
