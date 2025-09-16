package com.msa.order.global.kafka.config;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${kafka.uri}")
    private String KAFKA_BROKER_URL;
    @Value("${kafka.order-group}")
    private String KAFKA_ORDER_GROUP_ID;
    @Value("${kafka.stock-group}")
    private String KAFKA_STOCK_GROUP_ID;

    @Bean
    public ConsumerFactory<String, Object> orderConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER_URL);
        configs.put(GROUP_ID_CONFIG, KAFKA_ORDER_GROUP_ID);
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderKafkaListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderConsumerFactory());
        factory.getContainerProperties().setGroupId(KAFKA_ORDER_GROUP_ID);
        factory.setConcurrency(3);
        factory.getContainerProperties().setMissingTopicsFatal(false);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, Object> stockConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER_URL);
        configs.put(GROUP_ID_CONFIG, KAFKA_STOCK_GROUP_ID);
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stockConsumerFactory());
        factory.getContainerProperties().setGroupId(KAFKA_STOCK_GROUP_ID);
        factory.setConcurrency(3);
        factory.getContainerProperties().setMissingTopicsFatal(false);

        return factory;
    }
}
