package com.msa.jewelry.account.internal.api_impl;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.jewelry.account.api.StoreBalanceUpdater;
import com.msa.jewelry.account.internal.store.domain.entity.Store;
import com.msa.jewelry.account.internal.store.repository.StoreRepository;
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
 * 거래처 미수금 잔고를 같은 트랜잭션 안에서 즉시 반영하는 구현체.
 *
 * <p>2026-05 P1 단계 신설. 기존에는 SaleService 가 {@code OutboxEvent} 를 발행하고
 * {@code OutboxRelayService → Kafka(stub) → ApplicationEvent} 경로로 잔고를 갱신했으나,
 * 모놀로식에서는 외부 비동기가 필요 없으므로 **본 빈을 호출자 트랜잭션 안에서 직접 호출**한다.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>{@link TransactionHistoryRepository#existsByEventIdAndStore_StoreId}
 *       로 멱등성 검사 — 같은 eventId 가 이미 처리됐으면 no-op</li>
 *   <li>{@link Store#updateBalance(BigDecimal, Long)} 로 잔고 가감</li>
 *   <li>{@link TransactionHistory} 행을 기록 (감사 추적 + 멱등성 키 저장)</li>
 * </ol>
 *
 * <p>{@code Propagation.REQUIRED} (기본) 로 호출자 트랜잭션에 합류하므로
 * 판매 등록과 잔고 갱신이 함께 commit / rollback 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED)
public class StoreBalanceUpdaterImpl implements StoreBalanceUpdater {

    private final StoreRepository storeRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    @Override
    public void applyDelta(
            Long storeId,
            BigDecimal goldDelta,
            Long moneyDelta,
            String eventId,
            String transactionType,
            String material,
            Long accountSaleCode,
            String note) {

        // 1. 멱등성 검사 — 같은 eventId + storeId 가 이미 처리됐으면 skip
        if (transactionHistoryRepository.existsByEventIdAndStore_StoreId(eventId, storeId)) {
            log.debug("StoreBalanceUpdater: eventId={} storeId={} already processed (idempotent skip)",
                    eventId, storeId);
            return;
        }

        // 2. Store 잔고 갱신 (PESSIMISTIC_WRITE 락으로 동시 갱신 충돌 방지)
        Store store = storeRepository.findByIdWithLock(storeId)
                .orElseThrow(() -> new NotFoundException("Store not found: storeId=" + storeId));

        BigDecimal gold = goldDelta != null ? goldDelta : BigDecimal.ZERO;
        Long money = moneyDelta != null ? moneyDelta : 0L;
        store.updateBalance(gold, money);

        // 3. TransactionHistory 기록 (감사 추적 + 멱등성 키)
        TransactionHistory history = TransactionHistory.builder()
                .transactionType(parseSaleStatus(transactionType))
                .material(material)
                .goldAmount(gold)
                .moneyAmount(money)
                .eventId(eventId)
                .accountSaleCode(accountSaleCode)
                .store(store)
                .transactionHistoryNote(note)
                .build();
        transactionHistoryRepository.save(history);

        log.info("StoreBalanceUpdater applied: storeId={} goldDelta={} moneyDelta={} eventId={} type={}",
                storeId, gold, money, eventId, transactionType);
    }

    private SaleStatus parseSaleStatus(String transactionType) {
        if (transactionType == null || transactionType.isBlank()) {
            return null;
        }
        try {
            return SaleStatus.valueOf(transactionType);
        } catch (IllegalArgumentException e) {
            log.warn("StoreBalanceUpdater: unknown transactionType '{}' — stored as null", transactionType);
            return null;
        }
    }
}
