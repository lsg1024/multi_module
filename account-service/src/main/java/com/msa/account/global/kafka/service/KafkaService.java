package com.msa.account.global.kafka.service;

import com.msa.account.global.kafka.dto.KafkaEventDto;
import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.factory.repository.FactoryRepository;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.store.repository.StoreRepository;
import com.msa.account.local.transaction_history.domain.entity.TransactionHistory;
import com.msa.account.local.transaction_history.repository.TransactionHistoryRepository;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
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
        String saleCode = dto.getSaleCode();
        Long entityId = dto.getId();
        String eventId = dto.getEventId();
        String saleType = dto.getSaleType();
        String material = dto.getMaterial();
        BigDecimal pureGoldAmount = new BigDecimal(dto.getPureGoldBalance());
        Long moneyAmount = Long.valueOf(dto.getMoneyBalance());

        Store store;
        Factory factory;
        TransactionHistory history;
        if ("STORE".equals(type)) {

            store = storeRepository.findById(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("TENANT ID: " + dto.getTenantId() + " STORE: " + NOT_FOUND));

            store.updateBalance(pureGoldAmount, moneyAmount);

            history = TransactionHistory.builder()
                    .eventId(eventId)
                    .accountSaleCode(Long.parseLong(saleCode))
                    .transactionType(SaleStatus.valueOf(saleType))
                    .material(material)
                    .goldAmount(pureGoldAmount)
                    .moneyAmount(moneyAmount)
                    .store(store)
                    .transactionHistoryNote("")
                    .build();

        } else if ("FACTORY".equals(type)) {
            factory = factoryRepository.findById(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("TENANT ID: " + dto.getTenantId() + " FACTORY: " + NOT_FOUND));

            factory.updateBalance(pureGoldAmount, moneyAmount);

            history = TransactionHistory.builder()
                    .eventId(eventId)
                    .accountSaleCode(Long.parseLong(saleCode))
                    .transactionType(SaleStatus.valueOf(saleType))
                    .material(material)
                    .goldAmount(pureGoldAmount)
                    .moneyAmount(moneyAmount)
                    .factory(factory)
                    .transactionHistoryNote("")
                    .build();

        } else {
            throw new IllegalArgumentException("Unknown balance type: " + type);
        }

        transactionHistoryRepository.save(history);

    }

}
