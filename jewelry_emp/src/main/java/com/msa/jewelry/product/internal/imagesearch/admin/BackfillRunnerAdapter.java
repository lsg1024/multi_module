package com.msa.jewelry.product.internal.imagesearch.admin;

import com.msa.jewelry.product.internal.imagesearch.admin.ImageEmbeddingBackfillService.BackfillRunner;
import com.msa.jewelry.product.internal.imagesearch.dto.ImageSearchDtos.EmbedItem;
import com.msa.jewelry.product.internal.product.entity.ProductImage;
import com.msa.jewelry.product.internal.product.repository.image.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 백필 페이지 로더 — 실제 ProductImage 일괄 조회.
 *
 * 멀티테넌시는 Hibernate Schema-per-tenant 라우팅이 자동 처리한다.
 * (TenantContext.setTenant(...)이 호출자에서 미리 설정되어 있어야 함 — BackfillService.execute 참조)
 *
 * 모든 ProductImage 행을 imageId 오름차순으로 페이징 조회해서 EmbedItem으로 매핑.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackfillRunnerAdapter implements BackfillRunner {

    private final ProductImageRepository productImageRepository;

    /**
     * 현재 tenant schema의 ProductImage 총 수.
     */
    @Override
    @Transactional(readOnly = true)
    public int countAll(String tenantId) {
        long count = productImageRepository.count();
        // Integer 범위 초과는 단일 tenant 9000장 가정에서 발생 불가
        return (int) count;
    }

    /**
     * 0-based page 단위로 EmbedItem 묶음 반환.
     * 정렬: imageId asc (안정적 페이징)
     */
    @Override
    @Transactional(readOnly = true)
    public List<EmbedItem> loadPage(String tenantId, int page, int size) {
        Page<ProductImage> result = productImageRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "imageId"))
        );
        return result.getContent().stream()
                .map(this::toEmbedItem)
                .toList();
    }

    private EmbedItem toEmbedItem(ProductImage img) {
        // ProductImage.imagePath = "/products/{productId}/{file}" (tenant prefix 없음)
        // image-search-service Python이 nas_base_path + tenantId + imagePath로 절대경로 구성
        return new EmbedItem(
                img.getProduct().getProductId(),
                img.getImageId(),
                img.getImagePath()
        );
    }
}
