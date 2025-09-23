package com.msa.order.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.tenant.TenantContext;
import com.msa.order.global.exception.KafkaProcessingException;
import com.msa.order.global.kafka.dto.KafkaStockRequest;
import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.global.kafka.dto.OrderUpdateRequest;
import com.msa.order.local.order.service.KafkaOrderService;
import com.msa.order.local.stock.service.KafkaStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaOrderService kafkaOrderService;
    private final KafkaStockService kafkaStockService;

    public KafkaConsumer(ObjectMapper objectMapper, KafkaOrderService kafkaOrderService, KafkaStockService kafkaStockService) {
        this.objectMapper = objectMapper;
        this.kafkaOrderService = kafkaOrderService;
        this.kafkaStockService = kafkaStockService;
    }

    @KafkaListener(topics = "order.async.requested", groupId = "kafka.order-group", concurrency = "3")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void orderInfoAsyncRequested(String message) {
        try {
            OrderAsyncRequested evt = objectMapper.readValue(message, OrderAsyncRequested.class);

            TenantContext.setTenant(evt.getTenantId());

            kafkaOrderService.createHandle(evt);
        } catch (Exception e) {
            log.error("Consume failed. payload={}, err={}", message, e.getMessage(), e);
            throw new IllegalStateException("Kafka consume error", e);

        }
    }

    @KafkaListener(topics = "order.update.requested", groupId = "kafka.order-group", concurrency = "3")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void orderUpdateRequested(String message) {
        try {
            OrderUpdateRequest orderUpdateRequest = objectMapper.readValue(message, OrderUpdateRequest.class);

            TenantContext.setTenant(orderUpdateRequest.getTenantId());

            kafkaOrderService.updateHandle(orderUpdateRequest);
        } catch (Exception e) {
            log.error("Consume failed. payload={}, err={}", message, e.getMessage(), e);
            throw new IllegalStateException("Kafka consume error", e);

        }
    }

    @KafkaListener(topics = "stock.async.requested", groupId = "kafka.stock-group", containerFactory = "kafkaListenerContainerFactory")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void stockInfoAsyncRequested(String message) {
        try {
            KafkaStockRequest ksq = objectMapper.readValue(message, KafkaStockRequest.class);

            TenantContext.setTenant(ksq.getTenantId());

            kafkaStockService.saveStockDetail(ksq);
        } catch (Exception e) {
            log.error("Consume failed. payload={}, err={}", message, e.getMessage(), e);
            throw new IllegalStateException("Kafka consume error", e);

        }
    }

}
