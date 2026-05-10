package com.msa.jewelry.shared.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@link EventPublisher} 의 같은 JVM 내 구현체.
 *
 * <p>발행되는 이벤트를 Spring 의 {@link ApplicationEventPublisher} 로 위임.
 * 구독 측에서는 {@code @TransactionalEventListener(phase=AFTER_COMMIT)} 또는
 * {@code @ApplicationModuleListener} 로 받아 처리.
 *
 * <p>Kafka 시절 대비 차이:
 * <ul>
 *   <li>네트워크 호출 없음 → 마이크로초 단위 발행</li>
 *   <li>같은 JVM 트랜잭션 안에서 publish → AFTER_COMMIT 시점에 listener 실행</li>
 *   <li>실패 시 listener 측에서 재시도 (Spring Modulith JPA Outbox 사용 권장)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringApplicationEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(String topic, String key, Object payload) {
        log.debug("[EventPublisher] topic={}, key={}, payloadType={}",
                topic, key, payload == null ? "null" : payload.getClass().getSimpleName());
        publisher.publishEvent(new GenericTopicEvent(topic, key, payload));
    }

    /**
     * 토픽/키/페이로드를 묶은 일반 이벤트.
     * 구독자는 {@code topic} 으로 분기하거나, 별도 strongly-typed 이벤트 클래스를 사용 권장.
     */
    public record GenericTopicEvent(String topic, String key, Object payload) {
    }

    /**
     * 토픽 이름 기반 라우팅 예시.
     * 실제 구독은 도메인별 listener 에서 강한 타입 이벤트로 받는 편이 안전.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGenericEvent(GenericTopicEvent event) {
        log.debug("[EventPublisher] AFTER_COMMIT consumed: topic={}, key={}",
                event.topic(), event.key());
        // 도메인별 처리는 각 모듈에서 구체 이벤트 클래스를 별도 listen 하도록 구현.
    }
}
