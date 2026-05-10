package com.msa.jewelry.product.internal.imagesearch.config;

import com.msa.jewelry.product.internal.imagesearch.client.ImageSearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * 이미지 검색 비동기 / RestClient / 프로퍼티 등록 설정.
 *
 * - @Async("imageEmbeddingExecutor") 형태로 명시 사용 권장 (기본 풀과 분리)
 * - RestClient는 별도 빈으로 분리하여 ImageSearchClient에서 주입
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(ImageSearchProperties.class)
public class ImageSearchAsyncConfig {

    /**
     * 이미지 임베딩 인덱싱 전용 비동기 executor.
     * - corePoolSize 2, maxPoolSize 4
     * - 큐 용량 200 (이상 시 caller-runs로 fallback)
     */
    @Bean(name = "imageEmbeddingExecutor")
    public Executor imageEmbeddingExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(4);
        exec.setQueueCapacity(200);
        exec.setThreadNamePrefix("img-embed-");
        exec.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        exec.initialize();
        return exec;
    }

    @Bean
    public RestClient imageSearchRestClient(ImageSearchProperties props) {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("X-Internal-Api-Key", props.getApiKey())
                .requestFactory(buildRequestFactory(props.getConnectTimeout(), props.getReadTimeout()))
                .build();
    }

    private static org.springframework.http.client.SimpleClientHttpRequestFactory buildRequestFactory(
            Duration connectTimeout, Duration readTimeout) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        return factory;
    }
}
