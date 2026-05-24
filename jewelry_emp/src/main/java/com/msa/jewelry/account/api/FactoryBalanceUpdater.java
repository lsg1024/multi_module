package com.msa.jewelry.account.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 매입/반품 시 제조사 미수금 잔고를 갱신하는 공개 API.
 *
 * <p>{@link StoreBalanceUpdater} 와 동일한 트랜잭션 합류 정책(REQUIRED).
 * 호출자는 반드시 {@code @Transactional} 컨텍스트 안이어야 한다.
 *
 * <p>2026-05 P3 단계 신설. 기존 SaleService 가 Outbox→Kafka stub 경유로 FACTORY 잔고
 * 변동을 발행하던 경로를 같은 트랜잭션 안에서 본 빈을 직접 호출하는 방식으로 단순화.
 */
@Schema(description = "제조사 미수금 잔고 갱신 공개 API — 매입/반품 트랜잭션 내 호출")
public interface FactoryBalanceUpdater {

    void applyDelta(
            Long factoryId,
            BigDecimal goldDelta,
            Long moneyDelta,
            String eventId,
            String transactionType,
            String material,
            Long accountSaleCode,
            String note
    );
}
