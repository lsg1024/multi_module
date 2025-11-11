package com.msa.order.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration("orderRestTemplateConfig")
public class RestTemplateConfig {


    @Bean(name = "clientRestTemplate")
    public RestTemplate clientRestTemplate(RestTemplateBuilder builder) {

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

        return builder
                .requestFactory(() -> factory)
                .readTimeout(Duration.ofMillis(3000))
                .connectTimeout(Duration.ofMillis(3000))
                .build();

    }

}
