package com.msa.product.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.product.global.kafka.dto.ClassificationEvent;
import com.msa.product.global.kafka.dto.MaterialEvent;
import com.msa.product.global.kafka.dto.SetTypeEvent;
import org.apache.kafka.common.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendClassificationUpdate(String tenantId, Long classificationId) {
        try {
            ClassificationEvent event = new ClassificationEvent(tenantId, classificationId);
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("classification.update", message);
        } catch (Exception e) {
            throw new KafkaException(NOT_FOUND);
        }
    }

    public void sendClassificationDelete(String tenantId, Long classificationId) {
        try {
            MaterialEvent event = new MaterialEvent(tenantId, classificationId);
            kafkaTemplate.send("classification.delete", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new KafkaException(NOT_FOUND);
        }
    }

    public void sendMaterialUpdate(String tenantId, Long materialId) {
        try {
            MaterialEvent event = new MaterialEvent(tenantId, materialId);
            kafkaTemplate.send("material.update", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new KafkaException(NOT_FOUND);
        }
    }

    public void sendMaterialDelete(String tenantId, Long materialId) {
        try {
            MaterialEvent event = new MaterialEvent(tenantId, materialId);
            kafkaTemplate.send("material.delete", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new KafkaException(NOT_FOUND);
        }
    }

    public void sendSetTypeUpdate(String tenantId, Long setTypeId) {
        try {
            SetTypeEvent event = new SetTypeEvent(tenantId, setTypeId);
            kafkaTemplate.send("set-type.update", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new KafkaException(NOT_FOUND);
        }
    }

    public void sendSetTypeDelete(String tenantId, Long setTypeId) {
        try {
            SetTypeEvent event = new SetTypeEvent(tenantId, setTypeId);
            kafkaTemplate.send("set-type.delete", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new KafkaException(NOT_FOUND);
        }
    }

}
