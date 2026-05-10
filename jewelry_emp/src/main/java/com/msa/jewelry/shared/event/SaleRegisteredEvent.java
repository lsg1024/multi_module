package com.msa.jewelry.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 판매 등록 완료 후 발행되는 도메인 이벤트.
 *
 * <p>대부분의 경우 같은 트랜잭션 내에서 거래처 잔고 갱신이 직접 호출로 처리되지만,
 * 추후 fan-out 이 필요한 작업(SMS 알림, 영수증 인쇄 큐, 외부 회계 시스템 연동 등)이
 * 추가될 가능성을 고려해 이벤트 형태도 제공한다.
 *
 * <p>구독 측 권장 패턴:
 * <pre>{@code
 * @ApplicationModuleListener   // = @Async + @TransactionalEventListener(AFTER_COMMIT) + JPA Outbox
 * void onSaleRegistered(SaleRegisteredEvent event) {
 *     smsService.sendReceipt(event.storeId(), event.saleCode());
 * }
 * }</pre>
 */
public record SaleRegisteredEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String saleCode,
        Long storeId,
        String storeName,
        String materialName,
        BigDecimal pureGoldDelta,
        Long moneyDelta
) implements DomainEvent {
}
