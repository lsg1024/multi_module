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

/**
 * Transactional Outbox 패턴 릴레이 서비스.
 *
 * *DB의 {@code OUT_BOX_EVENT} 테이블에서 {@code PENDING} 상태의 이벤트를 조회하여
 * Kafka로 발행하고, 성공 시 {@code SENT}, 실패 시 재시도 횟수를 증가시킨다.
 *
 * *이벤트 유형별 처리 전략:
 *
 *   - {@code PAYMENT_SETTLED}: 정산 이벤트 — 순서 보장을 위해 순차 처리
 *   - 그 외 이벤트: 배치 처리 — 개별 실패를 허용하며 나머지 이벤트는 계속 진행
 * 
 * 
 *
 * *주요 의존성:
 *
 *   - {@link KafkaProducer} — Kafka 메시지 전송
 *   - {@link OutboxEventRepository} — PENDING 이벤트 조회 및 상태 갱신
 * 
 * 
 */
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
     * PENDING 이벤트를 조회하여 Kafka로 릴레이한다.
     *
     * *처리 흐름:
     *
     *   - DB에서 재시도 가능한 PENDING 이벤트를 최대 100건 조회 (커넥션 1회만 사용)
     *   - 이벤트를 {@code eventType}별로 분류
     *   - {@code PAYMENT_SETTLED} 이벤트: 순서 보장을 위해 순차 전송
     *   - 나머지 이벤트: 배치 전송 — 개별 실패 시 {@link OutboxEvent#incrementRetryCount} 호출,
     *       나머지 이벤트는 계속 처리
     *   - 전송 성공 시 {@link OutboxEvent#markAsSent()} 호출
     * 
     * 
     *
     * @return 처리할 이벤트가 하나라도 있었으면 {@code true}, 없었으면 {@code false}
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
