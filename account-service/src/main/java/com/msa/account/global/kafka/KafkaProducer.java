package com.msa.account.global.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.kafka.dto.GoldHarryLossUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendGoldHarryLossUpdated(Long commonOptionId, String updatedValue) {
        try {
            GoldHarryLossUpdatedEvent event = new GoldHarryLossUpdatedEvent(commonOptionId, updatedValue);
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("goldHarryLoss.update", message);
        } catch (Exception e) {
            log.error("Failed to produce GoldHarryLossUpdatedEvent", e);
        }
    }

}
