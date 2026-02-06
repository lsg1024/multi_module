package com.msa.order.local.outbox.redis;

import com.msa.common.global.redis.enum_type.EventStatus;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.local.outbox.domain.entity.OutboxEvent;
import com.msa.order.local.outbox.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OutboxRelayService {

    private final KafkaProducer kafkaProducer;
    private final OutboxEventRepository outboxEventRepository;

    public OutboxRelayService(KafkaProducer kafkaProducer,
                              OutboxEventRepository outboxEventRepository) {
        this.kafkaProducer = kafkaProducer;
        this.outboxEventRepository = outboxEventRepository;
    }

    /**
     * 한 번의 DB 조회로 모든 PENDING 이벤트를 가져와 타입별 처리
     * - DB 조회 1회 = 커넥션 1개만 사용
     * - 정산: 순차 처리 (순서 보장)
     * - 재고: 일괄 처리 (개별 실패 허용)
     *
     * @return 처리한 이벤트가 있으면 true
     */
    @Transactional
    public boolean relayAllEventsForTenant() {
        List<OutboxEvent> events = outboxEventRepository
                .findRetryableEvents(EventStatus.PENDING, PageRequest.of(0, 100));

        if (events.isEmpty()) return false;

        Map<String, List<OutboxEvent>> eventsByType = events.stream()
                .collect(Collectors.groupingBy(OutboxEvent::getEventType));

        // 정산 이벤트: 순차 처리 (순서 보장)
        List<OutboxEvent> paymentEvents = eventsByType.getOrDefault("PAYMENT_SETTLED", List.of());
        for (OutboxEvent event : paymentEvents) {
            try {
                kafkaProducer.send(event.getTopic(), event.getMessageKey(), event.getPayload());
                event.markAsSent();
            } catch (Exception e) {
                log.error("정산 이벤트 전송 실패. EventID: {}, Error: {}", event.getId(), e.getMessage());
                event.incrementRetryCount(e.getMessage());
            }
        }

        // 나머지 이벤트: 일괄 처리 (개별 실패 허용)
        eventsByType.forEach((eventType, eventList) -> {
            if (!"PAYMENT_SETTLED".equals(eventType)) {
                for (OutboxEvent event : eventList) {
                    try {
                        kafkaProducer.send(event.getTopic(), event.getMessageKey(), event.getPayload());
                        event.markAsSent();
                    } catch (Exception e) {
                        log.warn("이벤트 전송 실패 (재시도 예정). EventID: {}, Error: {}",
                                event.getId(), e.getMessage());
                        event.incrementRetryCount(e.getMessage());
                    }
                }
            }
        });

        log.info("Outbox 이벤트 처리 완료: {} 건", events.size());
        return true;
    }
}
