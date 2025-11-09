package com.msa.account.global.kafka.config;

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
    @Value("${kafka.harryGroup}")
    private String KAFKA_HARRY_GROUP_ID;
    @Value("${kafka.currentBalanceGroup}")
    private String KAFKA_BALANCE_GROUP_ID;

    @Bean
    public ConsumerFactory<String, Object> goldHarryConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER_URL);
        configs.put(GROUP_ID_CONFIG, KAFKA_HARRY_GROUP_ID);
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> goldHarryKafkaListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(goldHarryConsumerFactory());
        factory.getContainerProperties().setGroupId(KAFKA_HARRY_GROUP_ID);
        factory.setConcurrency(3);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, Object> balacneConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER_URL);
        configs.put(GROUP_ID_CONFIG, KAFKA_BALANCE_GROUP_ID);
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> balanceKafkaListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(balacneConsumerFactory());
        factory.getContainerProperties().setGroupId(KAFKA_BALANCE_GROUP_ID);
        factory.setConcurrency(3);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }
}
