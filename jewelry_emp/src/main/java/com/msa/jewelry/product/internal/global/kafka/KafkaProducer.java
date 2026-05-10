package com.msa.jewelry.product.internal.global.kafka;

import com.msa.jewelry.shared.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Product 모듈 KafkaProducer stub (in-process).
 *
 * <p>기존 product-service KafkaProducer 의 메서드 시그니처를 그대로 보존.
 * 호출 측 코드 (ClassificationService, MaterialService, ColorService, SetTypeService 등)
 * 무수정 동작.
 *
 * <p>발행되는 이벤트는 product-service 내부에서 자체 소비 (배치 잡 트리거)였으므로
 * 모놀리스에서는 직접 batch job 호출 또는 {@code @ApplicationModuleListener} 가
 * 더 깔끔하다. 점진적 전환을 위해 stub 유지.
 */
@Slf4j
@Component("productkafkaProducer")
@RequiredArgsConstructor
public class KafkaProducer {

    private final EventPublisher eventPublisher;

    public void sendClassificationUpdate(String tenantId, Long classificationId) {
        eventPublisher.publish("classification.update", tenantId,
                new SimplePayload(tenantId, "classification", classificationId, false));
    }

    public void sendClassificationDelete(String tenantId, Long classificationId) {
        eventPublisher.publish("classification.delete", tenantId,
                new SimplePayload(tenantId, "classification", classificationId, true));
    }

    public void sendMaterialUpdate(String tenantId, Long materialId) {
        eventPublisher.publish("material.update", tenantId,
                new SimplePayload(tenantId, "material", materialId, false));
    }

    public void sendMaterialDelete(String tenantId, Long materialId) {
        eventPublisher.publish("material.delete", tenantId,
                new SimplePayload(tenantId, "material", materialId, true));
    }

    public void sendSetTypeUpdate(String tenantId, Long setTypeId) {
        eventPublisher.publish("set-type.update", tenantId,
                new SimplePayload(tenantId, "set-type", setTypeId, false));
    }

    public void sendSetTypeDelete(String tenantId, Long setTypeId) {
        eventPublisher.publish("set-type.delete", tenantId,
                new SimplePayload(tenantId, "set-type", setTypeId, true));
    }

    public void sendColorUpdate(String tenantId, Long colorId) {
        eventPublisher.publish("color.update", tenantId,
                new SimplePayload(tenantId, "color", colorId, false));
    }

    public void sendColorDelete(String tenantId, Long colorId) {
        eventPublisher.publish("color.delete", tenantId,
                new SimplePayload(tenantId, "color", colorId, true));
    }

    public record SimplePayload(String tenantId, String entityType, Long entityId, boolean deleted) {
    }
}
