package com.msa.order.local.outbox.redis;

import com.msa.common.global.redis.enum_type.EventStatus;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.local.outbox.domain.entity.OutboxEvent;
import com.msa.order.local.outbox.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OutboxRelayService {

    private final KafkaProducer kafkaProducer;
    private final OutboxEventRepository outboxEventRepository;

    public OutboxRelayService(KafkaProducer kafkaProducer, OutboxEventRepository outboxEventRepository) {
        this.kafkaProducer = kafkaProducer;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public void relayPaymentEventsSequentially() throws ExecutionException, InterruptedException {

        List<String> types = List.of("PAYMENT_SETTLED");
        List<OutboxEvent> events = outboxEventRepository
                .findPendingEventsByType(EventStatus.PENDING, types, PageRequest.of(0, 100));

        if (events.isEmpty()) return;

        log.info("정산 이벤트 순차 처리: {} 건", events.size());

        for (OutboxEvent event : events) {
            try {
                kafkaProducer.send(event.getTopic(), event.getMessageKey(), event.getPayload());
                event.markAsSent();
            } catch (Exception e) {
                log.error("정산 이벤트 전송 실패. EventID: {}, Error: {}",
                        event.getId(), e.getMessage());
                throw e; // 트랜잭션 롤백
            }
        }
    }


    @Transactional
    public void relayStockEventsIndependently() {
        List<String> types = List.of("ORDER_CREATE", "ORDER_UPDATE", "STOCK_CREATED", "STOCK_UPDATE");
        List<OutboxEvent> events = outboxEventRepository
                .findPendingEventsByType(EventStatus.PENDING, types, PageRequest.of(0, 100));

        if (events.isEmpty()) return;

        log.info("재고 이벤트 독립 처리: {} 건", events.size());

        events.forEach(this::processEventInSeparateTransaction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 각각 독립 트랜잭션
    public void processEventInSeparateTransaction(OutboxEvent event) {
        try {
            kafkaProducer.send(event.getTopic(), event.getMessageKey(), event.getPayload());

            event.markAsSent();
            outboxEventRepository.saveAndFlush(event);

            log.info("재고 이벤트 전송 성공. EventID: {}, Key: {}",
                    event.getId(), event.getMessageKey());

        } catch (Exception e) {
            event.incrementRetryCount(e.getMessage());
            outboxEventRepository.save(event);

            log.warn("재고 이벤트 전송 실패 (재시도 예정). EventID: {}, Retry: {}, Error: {}",
                    event.getId(), event.getRetryCount(), e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.NEVER)
    public void relayAllPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findRetryableEvents(EventStatus.PENDING, PageRequest.of(0, 200));

        if (events.isEmpty()) return;

        log.info("전체 Outbox 이벤트 처리: {} 건", events.size());

        Map<String, List<OutboxEvent>> eventsByType = events.stream()
                .collect(Collectors.groupingBy(OutboxEvent::getEventType));

        // 정산은 순차, 재고는 병렬
        eventsByType.forEach((eventType, eventList) -> {
            if ("PAYMENT_SETTLED".equals(eventType)) {
                eventList.forEach(this::processEventSequentially);
            } else {
                eventList.forEach(this::processEventInSeparateTransaction);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEventSequentially(OutboxEvent event) {
        try {
            kafkaProducer.send(event.getTopic(), event.getMessageKey(), event.getPayload());
            event.markAsSent();
        } catch (Exception e) {
            log.error("순차 이벤트 처리 실패. EventID: {}, Error: {}",
                    event.getId(), e.getMessage(), e);
            throw new IllegalArgumentException("이벤트 처리 실패", e);
        }
    }
}