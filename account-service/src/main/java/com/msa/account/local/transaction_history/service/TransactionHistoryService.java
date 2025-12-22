package com.msa.account.local.transaction_history.service;

import com.msa.account.local.factory.repository.FactoryRepository;
import com.msa.account.local.store.repository.StoreRepository;
import com.msa.account.local.transaction_history.domain.dto.TransactionDto;
import com.msa.account.local.transaction_history.domain.dto.TransactionPage;
import com.msa.account.local.transaction_history.domain.entity.TransactionHistory;
import com.msa.account.local.transaction_history.dto.PurchaseDto;
import com.msa.account.local.transaction_history.repository.TransactionHistoryRepository;
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

        if (purchaseDto.getTransactionDate() == null) {
            LocalDateTime dateTime = purchaseDto.getTransactionDate().toLocalDateTime();
            transactionHistory.updateTransactionDate(dateTime);
        }

        transactionHistoryRepository.save(transactionHistory);
    }
}
