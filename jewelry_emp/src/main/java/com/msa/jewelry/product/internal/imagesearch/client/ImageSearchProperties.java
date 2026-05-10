package com.msa.jewelry.product.internal.imagesearch.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * application.yml의 imagesearch.* 프로퍼티 매핑.
 *
 * <pre>
 * imagesearch:
 *   base-url: http://image-search-service:8081
 *   api-key: ${IMAGE_SEARCH_API_KEY}
 *   connect-timeout: 2s
 *   read-timeout: 5s
 *   enabled: true
 *   default-top-k: 20
 *   candidate-top-k: 200
 *   scoring:
 *     similarity-weight: 0.7
 *     material-weight: 0.15
 *     color-weight: 0.15
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "imagesearch")
public class ImageSearchProperties {

    private String baseUrl;
    private String apiKey;
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    private boolean enabled = true;
    private int defaultTopK = 20;
    private int candidateTopK = 200;
    private Scoring scoring = new Scoring();

    @Getter
    @Setter
    public static class Scoring {
        private double similarityWeight = 0.7;
        private double materialWeight = 0.15;
        private double colorWeight = 0.15;
    }
}
