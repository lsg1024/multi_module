package com.msa.account.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.kafka.dto.GoldHarryLossUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendGoldHarryLossUpdated(String tenantId, Long commonOptionId, String updatedValue) {
        try {
            GoldHarryLossUpdatedEvent event = new GoldHarryLossUpdatedEvent(tenantId, commonOptionId, updatedValue);
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("goldHarryLoss.update", message);
        } catch (Exception e) {
            log.error("Failed to produce GoldHarryLossUpdatedEvent", e);
        }
    }

}
