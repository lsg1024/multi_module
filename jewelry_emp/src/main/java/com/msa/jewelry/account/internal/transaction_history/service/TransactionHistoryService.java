package com.msa.jewelry.account.internal.transaction_history.service;

import com.msa.jewelry.account.internal.factory.repository.FactoryRepository;
import com.msa.jewelry.account.internal.store.repository.StoreRepository;
import com.msa.jewelry.account.internal.transaction_history.domain.dto.TransactionDto;
import com.msa.jewelry.account.internal.transaction_history.domain.dto.TransactionPage;
import com.msa.jewelry.account.internal.transaction_history.domain.entity.TransactionHistory;
import com.msa.jewelry.account.internal.transaction_history.dto.PurchaseDto;
import com.msa.jewelry.account.internal.transaction_history.repository.TransactionHistoryRepository;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class TransactionHistoryService {

    private final StoreRepository storeRepository;
    private final FactoryRepository factoryRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public TransactionHistoryService(StoreRepository storeRepository, FactoryRepository factoryRepository, TransactionHistoryRepository transactionHistoryRepository) {
        this.storeRepository = storeRepository;
        this.factoryRepository = factoryRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    @Transactional(readOnly = true)
    public TransactionDto getCurrentBalance(String type, String id, String name) {
        Long targetId = Long.valueOf(id);
        TransactionDto currentBalance;
        if (type.equals("store")) {
            currentBalance = storeRepository.findByStoreIdAndStoreName(targetId, name);
        } else {
            currentBalance = factoryRepository.findByFactoryIdAndFactoryName(targetId, name);
        }
        return currentBalance;
    }

    @Transactional(readOnly = true)
    public CustomPage<TransactionPage> findAccountPurchase(String start, String end, String accountType, String accountName, Pageable pageable) {
        return transactionHistoryRepository.findTransactionHistory(start, end, accountType, accountName, pageable);
    }

    @Transactional(readOnly = true)
    public CustomPage<TransactionPage> findFactoryPurchase(String start, String end, String accountType, String accountName, Pageable pageable) {
        return transactionHistoryRepository.findTransactionHistoryFactory(start, end, accountType, accountName, pageable);
    }

    public void savePurchase(PurchaseDto purchaseDto) {

        TransactionHistory transactionHistory = TransactionHistory.builder()
                .transactionType(SaleStatus.valueOf(purchaseDto.getTransactionType()))
                .goldAmount(purchaseDto.getGoldAmount())
                .moneyAmount(purchaseDto.getMoneyAmount())
                .accountSaleCode(Long.parseLong(purchaseDto.getSaleCode()))
                .transactionHistoryNote(purchaseDto.getTransactionNote())
                .build();

        // NOTE: 원본 코드의 if 조건이 `== null` 이어서 들어가는 즉시 NPE 가 나는 구조였음.
        //       OffsetDateTime → LocalDateTime 전환과 별개로 원래 의도 재확인 필요 (TODO).
        //       PurchaseDto.transactionDate 가 LocalDateTime 으로 변경됐으므로 toLocalDateTime() 호출은 제거.
        if (purchaseDto.getTransactionDate() != null) {
            LocalDateTime dateTime = purchaseDto.getTransactionDate();
            transactionHistory.updateTransactionDate(dateTime);
        }

        transactionHistoryRepository.save(transactionHistory);
    }
}
