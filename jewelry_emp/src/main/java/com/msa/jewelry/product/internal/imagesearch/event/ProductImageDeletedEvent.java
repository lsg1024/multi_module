package com.msa.jewelry.product.internal.imagesearch.event;

import java.util.List;

/**
 * 이미지 삭제 직후 발행되는 이벤트. 임베딩 정리에 사용.
 */
public record ProductImageDeletedEvent(
        String tenantId,
        List<Long> productImageIds
) {}
