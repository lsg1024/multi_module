package com.msa.order.global.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendSync(String topic, String key, String payload) {
        try {
            kafkaTemplate.send(topic, key, payload)
                    .get(5, TimeUnit.SECONDS);

            log.debug("Kafka 동기 전송 성공. topic={}, key={}", topic, key);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka 전송 중단됨: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Kafka 전송 실패: " + e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("Kafka 전송 타임아웃 (5초 초과)", e);
        }
    }

    private void sendAsync(String topic, Object evt) {
        try {
            String payload = objectMapper.writeValueAsString(evt);
            String key = extractKey(evt);

            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 비동기 전송 실패. topic={}, key={}, err={}",
                                    topic, key, ex.getMessage());
                        } else {
                            log.info("Kafka 비동기 전송 성공. topic={}, key={}, partition={}, offset={}",
                                    result.getRecordMetadata().topic(),
                                    result.getProducerRecord().key(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이벤트 직렬화 실패: " + e.getMessage(), e);
        }
    }

    private String extractKey(Object evt) throws JsonProcessingException {
        return String.valueOf(evt.hashCode());
    }

    public void send(String topic, String key, String payload)
            throws ExecutionException, InterruptedException {

        log.info("Kafka 전송 시도. topic={}, key={}", topic, key);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, payload);

        SendResult<String, Object> res = future.get();

        log.info("Kafka 전송 성공. topic={}, key={}, partition={}, offset={}",
                res.getRecordMetadata().topic(),
                res.getProducerRecord().key(),
                res.getRecordMetadata().partition(),
                res.getRecordMetadata().offset());
    }


}
