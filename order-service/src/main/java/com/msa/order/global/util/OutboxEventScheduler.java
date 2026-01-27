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

@NoTrace
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {
    private final OutboxRelayService outboxRelayService;

    private final RedisTemplate<String, String> redisTemplate;
    private static final String OUTBOX_TENANT_WORK_QUEUE_KEY = "outbox:work:tenants";

    /**
     * 실시간성이 중요한 재고 이벤트 - 1초마다
     */
    @Scheduled(fixedDelay = 1000)
    public void relayStockEvents() {
        Set<String> tenants = redisTemplate.opsForSet().members(OUTBOX_TENANT_WORK_QUEUE_KEY);

        if (tenants == null || tenants.isEmpty()) {
            return;
        }

        for (String tenantId : tenants) {
            try {
                TenantContext.setTenant(tenantId);
                AuditorHolder.setAuditor(tenantId);

                outboxRelayService.relayStockEventsIndependently();

                redisTemplate.opsForSet().remove(OUTBOX_TENANT_WORK_QUEUE_KEY, tenantId);

            } catch (Exception e) {
                log.error("재고 Outbox 처리 실패. TenantID: {}", tenantId, e);
            } finally {
                TenantContext.clear();
                AuditorHolder.clear();
            }
        }
    }

    /**
     * 정산 이벤트 - 1초마다
     */
    @Scheduled(fixedDelay = 1000)
    public void relayPaymentEvents() {
        try {
            outboxRelayService.relayPaymentEventsSequentially();
        } catch (Exception e) {
            log.error("정산 Outbox 처리 실패", e);
        }
    }

    /**
     * 실패한 이벤트 재시도 - 1분마다
     */
    @Scheduled(fixedDelay = 60000)
    public void retryFailedEvents() {
        try {
            outboxRelayService.relayAllPendingEvents();
        } catch (Exception e) {
            log.error("Outbox 재시도 처리 실패", e);
        }
    }
}