package com.msa.order.local.outbox.redis;

import com.msa.order.global.dto.OutboxCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventListener {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String OUTBOX_TENANT_WORK_QUEUE_KEY = "outbox:work:tenants";

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOutboxCreated(OutboxCreatedEvent event) {
        try {
            redisTemplate.opsForSet().add(OUTBOX_TENANT_WORK_QUEUE_KEY, event.tenantId());
        } catch (Exception e) {
            log.error("Redis 작업 알림 추가 실패: {}", event.tenantId(), e);
        }
    }
}