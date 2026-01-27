package com.msa.order.local.outbox.redis;

import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.local.outbox.domain.entity.OutboxEvent;
import com.msa.order.local.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final KafkaProducer kafkaProducer;
    private final OutboxEventRepository outboxEventRepository;

    /**
     * 각 이벤트를 독립적인 트랜잭션에서 처리
     * 실패해도 다른 이벤트에 영향을 주지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEventIndependently(OutboxEvent event) {
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

    /**
     * 순차 처리용 - 실패 시 예외를 던져 롤백
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEventSequentially(OutboxEvent event) {
        try {
            kafkaProducer.send(event.getTopic(), event.getMessageKey(), event.getPayload());
            event.markAsSent();
            outboxEventRepository.saveAndFlush(event);
        } catch (Exception e) {
            log.error("순차 이벤트 처리 실패. EventID: {}, Error: {}",
                    event.getId(), e.getMessage(), e);
            throw new RuntimeException("이벤트 처리 실패", e);
        }
    }
}
