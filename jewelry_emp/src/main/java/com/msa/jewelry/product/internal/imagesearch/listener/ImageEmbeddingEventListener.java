package com.msa.jewelry.product.internal.imagesearch.listener;

import com.msa.jewelry.product.internal.imagesearch.client.ImageSearchClient;
import com.msa.jewelry.product.internal.imagesearch.client.ImageSearchProperties;
import com.msa.jewelry.product.internal.imagesearch.dto.ImageSearchDtos.*;
import com.msa.jewelry.product.internal.imagesearch.event.ProductImageDeletedEvent;
import com.msa.jewelry.product.internal.imagesearch.event.ProductImageUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * ProductImage 이벤트를 비동기로 image-search-service에 전파.
 *
 * - 트랜잭션 외부에서 동작 (이미지 업로드 트랜잭션 영향 없음)
 * - 실패 시 Spring Retry로 3회까지 지수 백오프
 * - 모든 retry 실패 시 ERROR 로그 — 별도 모니터/알림에서 캡처
 *
 * @Async("imageEmbeddingExecutor") — ImageSearchAsyncConfig 빈 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageEmbeddingEventListener {

    private final ImageSearchClient client;
    private final ImageSearchProperties properties;

    @Async("imageEmbeddingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Retryable(
            retryFor = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 5_000)
    )
    public void onUploaded(ProductImageUploadedEvent event) {
        if (!properties.isEnabled()) {
            log.debug("imagesearch disabled — skip embedding for image {}", event.productImageId());
            return;
        }

        EmbedRequest req = new EmbedRequest(
                event.tenantId(),
                List.of(new EmbedItem(event.productId(), event.productImageId(), event.imagePath()))
        );

        EmbedResponse resp = client.embed(req);

        if (resp.failed() != null && !resp.failed().isEmpty()) {
            EmbedFailure f = resp.failed().get(0);
            // 영속적 실패(파일 없음 등)는 예외 throw → 재시도해도 동일하므로
            // 운영 정책상 throw하지 않고 ERROR 로깅만 (재시도 무의미)
            log.error("Embedding failed permanently — productImageId={}, reason={}",
                    f.productImageId(), f.reason());
            return;
        }

        log.debug("Embedding indexed productImageId={}, modelVersion={}",
                event.productImageId(), resp.modelVersion());
    }

    @Async("imageEmbeddingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDeleted(ProductImageDeletedEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            client.embedDelete(new EmbedDeleteRequest(event.tenantId(), event.productImageIds()));
        } catch (RuntimeException e) {
            log.error("Embedding deletion failed — tenant={}, ids={}",
                    event.tenantId(), event.productImageIds(), e);
        }
    }
}
