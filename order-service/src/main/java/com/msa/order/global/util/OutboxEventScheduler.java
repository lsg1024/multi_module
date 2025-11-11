package com.msa.order.global.util;

import com.msa.common.global.aop.NoTrace;
import com.msa.common.global.tenant.TenantContext;
import com.msa.order.local.outbox.redis.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@NoTrace
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {
    private final OutboxRelayService outboxRelayService;

    private final RedisTemplate<String, String> redisTemplate;
    private static final String OUTBOX_TENANT_WORK_QUEUE_KEY = "outbox:work:tenants";

    @Scheduled(fixedDelay = 1000)
    public void relayOutboxEvent() {

        String tenantId = redisTemplate.opsForSet().pop(OUTBOX_TENANT_WORK_QUEUE_KEY);

        while (StringUtils.hasText(tenantId)) {
            try {
                TenantContext.setTenant(tenantId);

                outboxRelayService.relayEventsForCurrentTenant();

            } catch (Exception e) {
                log.error("[{}] 테넌트 작업 실패 (다음 주기에 재시도될 수 있음): {}", tenantId, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }

            tenantId = redisTemplate.opsForSet().pop(OUTBOX_TENANT_WORK_QUEUE_KEY);
        }
    }
}