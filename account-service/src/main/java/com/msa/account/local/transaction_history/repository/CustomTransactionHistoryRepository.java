package com.msa.account.local.transaction_history.repository;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.local.transaction_history.domain.dto.TransactionPage;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomTransactionHistoryRepository {
    CustomPage<TransactionPage> findTransactionHistory(String start, String end, String accountType, String accountName, Pageable pageable);
    CustomPage<TransactionPage> findTransactionHistoryFactory(String start, String end, String accountType, String accountName, Pageable pageable);

    /**
     * Task 4-3 — 특정 거래처(Store) 의 최근 SALE 트랜잭션을 최신순으로 조회.
     * @param storeId 거래처 ID
     * @param limit   반환 최대 건수
     */
    List<AccountDto.TransactionItem> findRecentSalesByStore(Long storeId, int limit);

    /**
     * Task 4-3 — 특정 제조사(Factory) 의 최근 SALE(=주문/거래) 트랜잭션 최신순 조회.
     */
    List<AccountDto.TransactionItem> findRecentSalesByFactory(Long factoryId, int limit);

    /**
     * Task 4-4 — 특정 거래처(Store) 의 PAYMENT 집계(총 순금 중량, 총 결제 금액, 건수, 최근 결제일).
     */
    AccountDto.PaymentSummary findPaymentSummaryByStore(Long storeId);

    /**
     * Task 4-4 — 특정 제조사(Factory) 의 PAYMENT 집계.
     */
    AccountDto.PaymentSummary findPaymentSummaryByFactory(Long factoryId);
}
