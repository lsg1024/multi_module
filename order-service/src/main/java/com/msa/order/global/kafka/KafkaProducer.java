package com.msa.order.global.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.order.global.kafka.dto.KafkaStockRequest;
import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.global.kafka.dto.OrderUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void orderSave(OrderAsyncRequested evt) {
        String key = String.valueOf(evt.getFlowCode());

        try {
            String payload = objectMapper.writeValueAsString(evt);
            kafkaTemplate.send("order.async.requested", key, payload)
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

    public void orderUpdate(OrderUpdateRequest orderUpdateRequest) {
        String key = String.valueOf(orderUpdateRequest.getFlowCode());

        try {
            String payload = objectMapper.writeValueAsString(orderUpdateRequest);
            kafkaTemplate.send("order.update.requested", key, payload)
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

    public void stockSave(KafkaStockRequest ksq) {
        String key = String.valueOf(ksq.getFlowCode());

        try {
            String payload = objectMapper.writeValueAsString(ksq);
            kafkaTemplate.send("stock.async.requested", key, payload)
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

    public void send(String topic, String key, String payload)
            throws ExecutionException, InterruptedException {

        log.info("Kafka 동기 전송 시도. topic={}, key={}", topic, key);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, payload);

        SendResult<String, Object> res = future.get();

        log.info("Kafka 동기 전송 성공. topic={}, key={}, partition={}, offset={}",
                res.getRecordMetadata().topic(),
                res.getProducerRecord().key(),
                res.getRecordMetadata().partition(),
                res.getRecordMetadata().offset());
    }


}
