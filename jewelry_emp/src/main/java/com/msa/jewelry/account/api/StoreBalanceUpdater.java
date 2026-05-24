package com.msa.jewelry.account.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 판매·결제·반품 시 거래처 미수금 잔고를 갱신하는 공개 API.
 *
 * <p>기존 MSA 에서는 order-service 가 Outbox → Kafka → account-service 컨슈머를
 * 거쳐 잔고를 업데이트했으나, 모놀리스에서는 같은 트랜잭션 내 직접 호출로 처리된다.
 *
 * <p>이 메서드를 호출하는 측은 반드시 {@code @Transactional} 컨텍스트 안이어야 한다
 * (호출자 트랜잭션에 합류 — REQUIRED).
 *
 * <h3>정합성 보장</h3>
 * 같은 트랜잭션 안에서 실행되므로:
 * <ul>
 *   <li>판매 등록 + 잔고 갱신이 함께 commit 또는 함께 rollback</li>
 *   <li>Kafka 컨슈머 lag 으로 인한 일시적 불일치 없음</li>
 *   <li>네트워크 실패 시나리오 자체가 사라짐</li>
 * </ul>
 */
@Schema(description = "거래처(매장) 미수금 잔고 갱신 공개 API — 판매/결제/반품 트랜잭션 내 호출")
public interface StoreBalanceUpdater {

    /**
     * 거래처 잔고를 변경량(delta)만큼 가감한다.
     *
     * @param storeId       거래처 ID
     * @param goldDelta     순금 중량 변화 (+: 미수 증가, -: 미수 감소)
     * @param moneyDelta    금액 변화 (+: 미수 증가, -: 미수 감소)
     * @param eventId       멱등성 키 (TransactionHistory.eventId UNIQUE 제약)
     * @param transactionType  트랜잭션 유형 ("SALE", "PAYMENT", "RETURN" 등)
     * @param material      재질 (트랜잭션 이력 기록용)
     * @param accountSaleCode 판매 세션 코드 (이력 추적용)
     * @param note          비고
     */
    void applyDelta(
            Long storeId,
            BigDecimal goldDelta,
            Long moneyDelta,
            String eventId,
            String transactionType,
            String material,
            Long accountSaleCode,
            String note
    );
}
