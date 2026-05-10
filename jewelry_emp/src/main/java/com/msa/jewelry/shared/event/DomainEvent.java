package com.msa.jewelry.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 모듈 간 도메인 이벤트의 공통 base.
 *
 * <p>기존 Kafka 토픽으로 발행되던 이벤트들이 모놀리스에서는
 * Spring {@link org.springframework.context.ApplicationEventPublisher} 또는
 * Spring Modulith 의 {@code @ApplicationModuleListener} 를 통해
 * 같은 JVM 내에서 처리된다.
 *
 * <h3>Kafka topic ↔ Domain event 매핑</h3>
 * <table>
 *   <tr><th>Kafka topic (구)</th><th>Domain event (신)</th><th>처리 방식</th></tr>
 *   <tr><td>order.create.requested</td><td>OrderCreateRequested</td><td>같은 TX 내 직접 호출</td></tr>
 *   <tr><td>stock.async.requested</td><td>StockAsyncRequested</td><td>같은 TX 내 직접 호출</td></tr>
 *   <tr><td>current-balance-update</td><td>SaleRegisteredEvent</td><td>같은 TX 내 직접 호출 (정합성 핵심)</td></tr>
 *   <tr><td>goldHarryLoss.update</td><td>GoldHarryLossUpdated</td><td>@ApplicationModuleListener (배치 트리거)</td></tr>
 *   <tr><td>classification.update</td><td>ClassificationUpdated</td><td>@ApplicationModuleListener</td></tr>
 *   <tr><td>material.update / delete</td><td>MaterialChanged</td><td>@ApplicationModuleListener</td></tr>
 * </table>
 */
public interface DomainEvent {

    /** 이벤트 고유 ID — 멱등성 키로 사용 가능. */
    UUID eventId();

    /** 이벤트 발생 시각. */
    Instant occurredAt();

    /** 멀티테넌트 컨텍스트 — 비동기 처리 시 TenantContext 복원에 사용. */
    String tenantId();
}
