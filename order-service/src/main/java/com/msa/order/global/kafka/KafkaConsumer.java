package com.msa.order.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.tenant.TenantContext;
import com.msa.order.global.exception.KafkaProcessingException;
import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.local.domain.order.service.OrderAsyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private final OrderAsyncService orderAsyncService;

    public KafkaConsumer(ObjectMapper objectMapper, OrderAsyncService orderAsyncService) {
        this.objectMapper = objectMapper;
        this.orderAsyncService = orderAsyncService;
    }

    @KafkaListener(topics = "order.async.requested", containerFactory = "kafkaListenerContainerFactory")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void orderInfoAsyncRequested(String message) {
        try {
            OrderAsyncRequested evt = objectMapper.readValue(message, OrderAsyncRequested.class);

            TenantContext.setTenant(evt.getTenantId());

            orderAsyncService.handle(evt);
        } catch (Exception e) {
            log.error("Consume failed. payload={}, err={}", message, e.getMessage(), e);
            throw new IllegalStateException("Kafka consume error", e);

        }
    }

}
