package com.msa.jewelry.account.internal.api_impl;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.jewelry.account.api.FactoryBalanceUpdater;
import com.msa.jewelry.account.internal.factory.domain.entity.Factory;
import com.msa.jewelry.account.internal.factory.repository.FactoryRepository;
import com.msa.jewelry.account.internal.transaction_history.domain.entity.TransactionHistory;
import com.msa.jewelry.account.internal.transaction_history.repository.TransactionHistoryRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 제조사 미수금 잔고를 같은 트랜잭션 안에서 즉시 반영하는 구현체.
 *
 * <p>{@link com.msa.jewelry.account.internal.api_impl.StoreBalanceUpdaterImpl} 과 동일한 패턴 —
 * 멱등성 검사(existsByEventIdAndFactory_FactoryId) + PESSIMISTIC_WRITE 락 + 잔고 가감 +
 * TransactionHistory 기록.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED)
public class FactoryBalanceUpdaterImpl implements FactoryBalanceUpdater {

    private final FactoryRepository factoryRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    @Override
    public void applyDelta(
            Long factoryId,
            BigDecimal goldDelta,
            Long moneyDelta,
            String eventId,
            String transactionType,
            String material,
            Long accountSaleCode,
            String note) {

        // 1. 멱등성 검사
        if (transactionHistoryRepository.existsByEventIdAndFactory_FactoryId(eventId, factoryId)) {
            log.debug("FactoryBalanceUpdater: eventId={} factoryId={} already processed (idempotent skip)",
                    eventId, factoryId);
            return;
        }

        // 2. Factory 잔고 갱신 (PESSIMISTIC_WRITE)
        Factory factory = factoryRepository.findByIdWithLock(factoryId)
                .orElseThrow(() -> new NotFoundException("Factory not found: factoryId=" + factoryId));

        BigDecimal gold = goldDelta != null ? goldDelta : BigDecimal.ZERO;
        Long money = moneyDelta != null ? moneyDelta : 0L;
        factory.updateBalance(gold, money);

        // 3. TransactionHistory 기록
        TransactionHistory history = TransactionHistory.builder()
                .transactionType(parseSaleStatus(transactionType))
                .material(material)
                .goldAmount(gold)
                .moneyAmount(money)
                .eventId(eventId)
                .accountSaleCode(accountSaleCode)
                .factory(factory)
                .transactionHistoryNote(note)
                .build();
        transactionHistoryRepository.save(history);

        log.info("FactoryBalanceUpdater applied: factoryId={} goldDelta={} moneyDelta={} eventId={} type={}",
                factoryId, gold, money, eventId, transactionType);
    }

    private SaleStatus parseSaleStatus(String transactionType) {
        if (transactionType == null || transactionType.isBlank()) {
            return null;
        }
        try {
            return SaleStatus.valueOf(transactionType);
        } catch (IllegalArgumentException e) {
            log.warn("FactoryBalanceUpdater: unknown transactionType '{}' — stored as null", transactionType);
            return null;
        }
    }
}
