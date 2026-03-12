package com.msa.order.global.kafka;

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

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, String key, String payload)
            throws ExecutionException, InterruptedException {

        log.info("Kafka 전송 시도. topic={}, key={}", topic, key);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, payload);

        try {
            SendResult<String, Object> res = future.get(10, TimeUnit.SECONDS);

            log.info("Kafka 전송 성공. topic={}, key={}, partition={}, offset={}",
                    res.getRecordMetadata().topic(),
                    res.getProducerRecord().key(),
                    res.getRecordMetadata().partition(),
                    res.getRecordMetadata().offset());
        } catch (TimeoutException e) {
            throw new RuntimeException("Kafka 전송 타임아웃 (10초 초과)", e);
        }
    }


}
