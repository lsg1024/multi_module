package com.msa.account.global.kafka.service;

import com.msa.account.global.kafka.dto.KafkaEventDto;
import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.factory.repository.FactoryRepository;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.store.repository.StoreRepository;
import com.msa.account.local.transaction_history.domain.entity.TransactionHistory;
import com.msa.account.local.transaction_history.repository.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.msa.account.global.exception.ExceptionMessage.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class KafkaService {

    private final StoreRepository storeRepository;
    private final FactoryRepository factoryRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public KafkaService(StoreRepository storeRepository, FactoryRepository factoryRepository, TransactionHistoryRepository transactionHistoryRepository) {
        this.storeRepository = storeRepository;
        this.factoryRepository = factoryRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }


    // 상점 or 공장 잔액 업데이트
    public void updateCurrentBalance(KafkaEventDto.updateCurrentBalance dto) {
        String type = dto.getType();
        Long entityId = dto.getId();
        String eventId = dto.getEventId();
        String saleType = dto.getSaleType();
        BigDecimal goldAmount = new BigDecimal(dto.getGoldBalance());
        Long moneyAmount = Long.valueOf(dto.getMoneyBalance());

        Store store = null;
        Factory factory = null;
        BigDecimal goldAmountDelta;
        if ("STORE".equals(type)) {

            store = storeRepository.findById(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("TENANT ID: " + dto.getTenantId() + " STORE: " + NOT_FOUND));

            BigDecimal currentGoldBalance = store.getCurrentGoldBalance();
            goldAmountDelta = currentGoldBalance.add(goldAmount);
            store.updateBalance(goldAmountDelta, moneyAmount);

        } else if ("FACTORY".equals(type)) {
            factory = factoryRepository.findById(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("TENANT ID: " + dto.getTenantId() + " FACTORY: " + NOT_FOUND));

            BigDecimal currentGoldBalance = factory.getCurrentGoldBalance();
            goldAmountDelta = currentGoldBalance.add(goldAmount);

            factory.updateBalance(goldAmountDelta, moneyAmount);

        } else {
            throw new IllegalArgumentException("Unknown balance type: " + type);
        }

        if (!"SALE".equals(saleType)) {

            TransactionHistory history = TransactionHistory.builder()
                    .eventId(eventId)
                    .transactionType(saleType)
                    .goldAmount(goldAmount)
                    .moneyAmount(moneyAmount)
                    .store(store)
                    .factory(factory)
                    .build();

            transactionHistoryRepository.save(history);
        }
    }

}
