package com.msa.jewelry.global.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    /** 이벤트 고유 ID — 멱등성 키로 사용 가능. */
    UUID eventId();

    /** 이벤트 발생 시각. */
    Instant occurredAt();

    /** 멀티테넌트 컨텍스트 — 비동기 처리 시 TenantContext 복원에 사용. */
    String tenantId();
}
