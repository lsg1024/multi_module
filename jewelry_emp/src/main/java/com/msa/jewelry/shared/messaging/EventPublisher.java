package com.msa.jewelry.shared.messaging;

/**
 * 모듈 간 이벤트 발행 추상화.
 *
 * <p>기존 Kafka 의존을 제거하면서, 호출 측 코드(예: KafkaProducer.send,
 * sendGoldHarryLossUpdated 등)는 변경하지 않고 내부 구현만 바꿀 수 있도록
 * 설계된 인터페이스다.
 *
 * <h3>구현체 전환 경로</h3>
 * <ol>
 *   <li><b>현재(Phase 1)</b>: {@code SpringApplicationEventPublisher}
 *       — 같은 JVM 내 {@link org.springframework.context.ApplicationEventPublisher} 사용.
 *       대부분의 시나리오는 모놀리스 통합으로 직접 메서드 호출이 가능해져
 *       이벤트 발행 자체가 불필요해진다.</li>
 *   <li><b>Phase 2 (Redis 도입)</b>: {@code RedisStreamEventPublisher}
 *       — Spring Data Redis Streams 로 전환. 멀티 인스턴스 환경 대비.</li>
 *   <li><b>Phase 3 (Kafka 복귀, 만약 필요시)</b>: {@code KafkaEventPublisher}
 *       — 동일 인터페이스로 Kafka 다시 도입 가능.</li>
 * </ol>
 *
 * <p>모놀리스 + 단일 인스턴스 환경에서는 대부분의 호출이 같은 트랜잭션 내
 * 직접 호출로 대체되므로, 이 인터페이스 사용처는 점차 줄어드는 것이 정상이다.
 */
public interface EventPublisher {

    /**
     * 이벤트를 비동기로 발행한다.
     *
     * @param topic   토픽/스트림/채널 이름 (구현체별 명명 규칙)
     * @param key     파티션 키 (Kafka 시절 잔재; 단일 인스턴스에서는 무시)
     * @param payload 직렬화된 페이로드 (JSON 문자열 권장)
     */
    void publish(String topic, String key, Object payload);
}
