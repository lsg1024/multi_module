package com.msa.order.local.outbox.redis;

import com.msa.common.global.redis.enum_type.EventStatus;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.local.outbox.domain.entity.OutboxEvent;
import com.msa.order.local.outbox.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class OutboxRelayService {

    private final KafkaProducer kafkaProducer;
    private final OutboxEventRepository outboxEventRepository;

    public OutboxRelayService(KafkaProducer kafkaProducer, OutboxEventRepository outboxEventRepository) {
        this.kafkaProducer = kafkaProducer;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public void relayEventsForCurrentTenant() {

        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatus(EventStatus.PENDING);

        if (!events.isEmpty()) {
            log.info("발견된 Outbox 이벤트: {} 건", events.size());
        }

        for (OutboxEvent event : events) {
            try {
                kafkaProducer.send(event.getTopic(), event.getMessageKey(), event.getPayload());

                event.markAsSent();

            } catch (Exception e) {
                log.error("Kafka 동기 전송 실패. (트랜잭션 롤백으로 다음 주기 재시도) EventID: {}, Error: {}",
                        event.getId(), e.getMessage());
            }
        }
    }
}