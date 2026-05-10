package com.msa.jewelry.order.internal.global.kafka;

import com.msa.jewelry.shared.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Order 모듈 KafkaProducer stub (in-process).
 *
 * <p>order-service 의 KafkaProducer 는 OutboxRelayService 가 호출하던 일반화된
 * {@code send(topic, key, payload)} 형태였다. 이를 in-process EventPublisher 로 대체.
 *
 * <p>모놀리스 통합 후 권장 경로:
 * <ul>
 *   <li>OutboxEvent 자체를 제거하고 {@code @ApplicationModuleListener} 사용</li>
 *   <li>또는 같은 트랜잭션 내 직접 호출로 대체</li>
 * </ul>
 */
@Slf4j
@Component("orderkafkaProducer")
@RequiredArgsConstructor
public class KafkaProducer {

    private final EventPublisher eventPublisher;

    /**
     * 토픽 / 키 / 페이로드 일반 전송.
     * 기존 OutboxRelayService 가 사용하던 시그니처 보존.
     */
    public void send(String topic, String key, String payload) {
        log.debug("[Order.Kafka] send topic={}, key={}", topic, key);
        eventPublisher.publish(topic, key, payload);
    }
}
