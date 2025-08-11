package com.msa.order.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.tenant.TenantContext;
import com.msa.order.global.kafka.dto.OrderEnrichmentRequested;
import com.msa.order.local.domain.order.service.OrderEnrichmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private final OrderEnrichmentService enrichmentService;

    public KafkaConsumer(ObjectMapper objectMapper, OrderEnrichmentService enrichmentService) {
        this.objectMapper = objectMapper;
        this.enrichmentService = enrichmentService;
    }

    @KafkaListener(
            topics = "order.enrichment.requested",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEnrichmentRequested(String message) {
        try {
            OrderEnrichmentRequested evt = objectMapper.readValue(message, OrderEnrichmentRequested.class);

            TenantContext.setTenant(evt.getTenantId());
            // 서비스로 위임 (비즈니스 전담)
            enrichmentService.handle(evt);
        } catch (Exception e) {
            log.error("Consume failed. payload={}, err={}", message, e.getMessage(), e);
            throw new IllegalStateException("Kafka consume error", e);

        }
    }

}
