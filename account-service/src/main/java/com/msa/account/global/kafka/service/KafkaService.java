package com.msa.account.global.kafka.service;

import com.msa.account.global.kafka.dto.KafkaEventDto;
import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.factory.repository.FactoryRepository;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.store.repository.StoreRepository;
import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import com.msa.account.local.transaction_history.domain.entity.TransactionHistory;
import com.msa.account.local.transaction_history.repository.SaleLogRepository;
import com.msa.account.local.transaction_history.repository.TransactionHistoryRepository;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class KafkaService {

    private final StoreRepository storeRepository;
    private final FactoryRepository factoryRepository;
    private final SaleLogRepository saleLogRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public KafkaService(StoreRepository storeRepository, FactoryRepository factoryRepository, SaleLogRepository saleLogRepository, TransactionHistoryRepository transactionHistoryRepository) {
        this.storeRepository = storeRepository;
        this.factoryRepository = factoryRepository;
        this.saleLogRepository = saleLogRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }


    // 상점 or 공장 잔액 업데이트
    public SaleLog updateCurrentBalance(KafkaEventDto.updateCurrentBalance dto) {

        String type = dto.getType();
        Long saleCode = Long.parseLong(dto.getSaleCode());
        Long entityId = dto.getId();
        String eventId = dto.getEventId();
        String saleType = dto.getSaleType();
        String material = dto.getMaterial();
        BigDecimal pureGoldAmount = new BigDecimal(dto.getPureGoldBalance());
        Long moneyAmount = Long.valueOf(dto.getMoneyBalance());
        LocalDateTime transactionDate;

        log.info("updateCurrentBalance = {}", dto.toString());

        if (SaleStatus.RETURN.name().equals(saleType)) {
            Optional<SaleLog> originalLog = Optional.empty();

            if ("STORE".equals(type)) {
                originalLog = saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateDesc(saleCode, entityId);
            } else if ("FACTORY".equals(type)) {
                originalLog = saleLogRepository.findTopByAccountSaleCodeAndFactory_FactoryIdOrderBySaleDateDesc(saleCode, entityId);
            }

            transactionDate = originalLog.map(saleLog -> saleLog.getSaleDate().plus(1, ChronoUnit.MICROS)).orElseGet(() -> dto.getSaleDate() != null ? dto.getSaleDate() : LocalDateTime.now());
        } else {
            transactionDate = dto.getSaleDate() != null ? dto.getSaleDate() : LocalDateTime.now();
        }

        if (SaleStatus.WG.name().equals(saleType)) {
            moneyAmount = 0L;
        }

        transactionDate = transactionDate.truncatedTo(ChronoUnit.MICROS);

        Store store = null;
        Factory factory = null;

        SaleLog lastLog = null;
        BigDecimal prevGold = BigDecimal.ZERO;
        Long prevMoney = 0L;

        if ("STORE".equals(type)) {

            if (transactionHistoryRepository.existsByEventIdAndStore_StoreId(eventId, entityId)) {
                log.info("이미 처리된 상점 잔액 업데이트입니다. eventId={}, storeId={}", eventId, entityId);
                throw new IllegalArgumentException("이미 처리된 상점 잔액 업데이트입니다.");
            }

            store = storeRepository.findByIdWithLock(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("TENANT ID: " + dto.getTenantId() + " STORE: NOT FOUND"));

            lastLog = saleLogRepository.findTopByStoreAndOwnerTypeOrderBySaleDateDesc(store, "STORE").orElse(null);

            if (lastLog == null) {
                prevGold = store.getCurrentGoldBalance() != null ? store.getCurrentGoldBalance() : BigDecimal.ZERO;
                prevMoney = store.getCurrentMoneyBalance() != null ? store.getCurrentMoneyBalance() : 0L;
            }

        } else if ("FACTORY".equals(type)) {

            if (transactionHistoryRepository.existsByEventIdAndFactory_FactoryId(eventId, entityId)) {
                log.info("이미 처리된 공장 잔액 업데이트입니다. eventId={}, factoryId={}", eventId, entityId);
                throw new IllegalArgumentException("이미 처리된 공장 잔액 업데이트입니다");
            }

            factory = factoryRepository.findByIdWithLock(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("TENANT ID: " + dto.getTenantId() + " FACTORY: NOT FOUND"));

            lastLog = saleLogRepository.findTopByFactoryAndOwnerTypeOrderBySaleDateDesc(factory, "FACTORY").orElse(null);

            if (lastLog == null) {
                prevGold = factory.getCurrentGoldBalance() != null ? factory.getCurrentGoldBalance() : BigDecimal.ZERO;
                prevMoney = factory.getCurrentMoneyBalance() != null ? factory.getCurrentMoneyBalance() : 0L;
            }

        } else {
            throw new IllegalArgumentException("Unknown balance type: " + type);
        }

        if (lastLog != null) {
            prevGold = lastLog.getAfterGoldBalance();
            prevMoney = lastLog.getAfterMoneyBalance();
        }

        BigDecimal afterGold = prevGold.add(pureGoldAmount);
        Long afterMoney = prevMoney + moneyAmount;

        SaleLog newLog = SaleLog.builder()
                .accountSaleCode(saleCode)
                .ownerType(type)
                .previousGoldBalance(prevGold)
                .previousMoneyBalance(prevMoney)
                .afterGoldBalance(afterGold)
                .afterMoneyBalance(afterMoney)
                .saleDate(transactionDate)
                .store(store)
                .factory(factory)
                .build();

        SaleLog saleLog = saleLogRepository.save(newLog);

        if (store != null) {
            store.updateBalance(pureGoldAmount, moneyAmount);
        } else {
            factory.updateBalance(pureGoldAmount, moneyAmount);
        }

        TransactionHistory history = TransactionHistory.builder()
                .eventId(eventId)
                .accountSaleCode(saleCode)
                .transactionType(SaleStatus.valueOf(saleType))
                .material(material)
                .goldAmount(pureGoldAmount)
                .moneyAmount(moneyAmount)
                .store(store)
                .factory(factory)
                .transactionHistoryNote("")
                .build();

        transactionHistoryRepository.save(history);

        return saleLog;
    }
}
