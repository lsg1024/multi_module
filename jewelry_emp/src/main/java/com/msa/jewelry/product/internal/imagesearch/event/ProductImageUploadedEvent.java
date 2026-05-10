package com.msa.jewelry.product.internal.imagesearch.event;

/**
 * 이미지 업로드/저장 직후 발행되는 이벤트.
 *
 * ProductImageService.save(...) 마지막에
 * eventPublisher.publishEvent(new ProductImageUploadedEvent(...)) 형태로 발행한다.
 *
 * @param tenantId        TenantContext에서 가져온 테넌트 식별자
 * @param productId       소속 상품 ID
 * @param productImageId  방금 저장된 ProductImage의 PK
 * @param imagePath       NAS 베이스 경로 기준 상대경로
 *                        (예: "kkhan/products/123/uuid-aaa.jpg")
 */
public record ProductImageUploadedEvent(
        String tenantId,
        long productId,
        long productImageId,
        String imagePath
) {}
