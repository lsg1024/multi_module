package com.msa.order.global.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.order.global.kafka.dto.OrderEnrichmentRequested;
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

    public void publishOrderEnrichmentRequested(OrderEnrichmentRequested evt) {
        String key = String.valueOf(evt.getOrderId());

        try {
            String payload = objectMapper.writeValueAsString(evt);
            kafkaTemplate.send("order.enrichment.requested", key, payload)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.error("kafka send failed. topic= {}, key= {}, err= {}",
                                    "order.enrichment.requested", key, ex.getMessage());
                        } else {
                            log.info("Kafka sent. topic={}, key={}, partition={}, offset={}",
                                    res.getRecordMetadata().topic(),
                                    res.getProducerRecord().key(),
                                    res.getRecordMetadata().partition(),
                                    res.getRecordMetadata().offset());
                        }
                    });

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이벤트 직렬화 실패", e);
        }
    }

}
