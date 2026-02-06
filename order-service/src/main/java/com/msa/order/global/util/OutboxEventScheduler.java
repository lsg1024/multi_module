package com.msa.order.global.util;

import com.msa.common.global.aop.NoTrace;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.AuditorHolder;
import com.msa.order.local.outbox.redis.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Outbox 이벤트 Fallback 스케줄러
 * - 메인 처리: OutboxNotificationListener (PostgreSQL LISTEN/NOTIFY)
 * - 이 스케줄러는 LISTEN 누락분 안전망 역할 (30초마다)
 */
@NoTrace
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxRelayService outboxRelayService;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String OUTBOX_TENANT_WORK_QUEUE_KEY = "outbox:work:tenants";

    /**
     * Fallback: LISTEN 누락분 처리 - 30초마다
     * (메인 처리는 OutboxNotificationListener가 즉시 수행)
     */
    @Scheduled(fixedDelay = 30000)
    public void fallbackRelay() {
        Set<String> tenants = redisTemplate.opsForSet().members(OUTBOX_TENANT_WORK_QUEUE_KEY);

        if (tenants == null || tenants.isEmpty()) {
            return;
        }

        for (String tenantId : tenants) {
            try {
                TenantContext.setTenant(tenantId);
                AuditorHolder.setAuditor(tenantId);

                boolean hasEvents = outboxRelayService.relayAllEventsForTenant();

                if (!hasEvents) {
                    redisTemplate.opsForSet().remove(OUTBOX_TENANT_WORK_QUEUE_KEY, tenantId);
                }

            } catch (Exception e) {
                log.error("Outbox fallback 처리 실패. TenantID: {}", tenantId, e);
            } finally {
                TenantContext.clear();
                AuditorHolder.clear();
            }
        }
    }
}
