package com.msa.jewelry.account.internal.global.kafka;

import com.msa.jewelry.shared.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Account 모듈 KafkaProducer stub (in-process).
 *
 * <p>기존 Kafka 기반 발행을 {@link EventPublisher} 추상화로 위임. 호출 측 코드
 * (GoldHarryService 등)는 시그니처 변경 없이 그대로 동작한다.
 *
 * <p>모놀리스 통합 후 대부분의 시나리오는 같은 트랜잭션 내 직접 호출로 변경하는 것이
 * 더 단순하고 정합성도 강하지만, 점진적 전환을 위해 일단 stub 으로 호환성 유지.
 */
@Slf4j
@Component("accountkafkaProducer")
@RequiredArgsConstructor
public class KafkaProducer {

    private final EventPublisher eventPublisher;

    /**
     * 금 손모율 변경 이벤트.
     *
     * <p>기존 동작: account-service 내 KafkaConsumer 가 수신하여
     *  UpdateGoldHarryLossBatchJob 실행.
     * <p>모놀리스 동작 권장: 같은 트랜잭션 내 직접 batch job 호출 또는
     *  {@code @ApplicationModuleListener} 로 변경.
     */
    public void sendGoldHarryLossUpdated(String tenantId, Long commonOptionId, String updatedValue) {
        log.debug("[Account.Kafka] sendGoldHarryLossUpdated tenantId={}, commonOptionId={}",
                tenantId, commonOptionId);
        eventPublisher.publish("goldHarryLoss.update", tenantId,
                new GoldHarryLossUpdatedPayload(tenantId, commonOptionId, updatedValue));
    }

    public void sendGoldHarryDeleted(String tenantId, String goldHarryId) {
        log.debug("[Account.Kafka] sendGoldHarryDeleted tenantId={}, goldHarryId={}",
                tenantId, goldHarryId);
        eventPublisher.publish("goldHarry.delete", tenantId,
                new GoldHarryDeletedPayload(tenantId, goldHarryId));
    }

    public record GoldHarryLossUpdatedPayload(
            String tenantId, Long commonOptionId, String updatedValue) {
    }

    public record GoldHarryDeletedPayload(String tenantId, String goldHarryId) {
    }
}
