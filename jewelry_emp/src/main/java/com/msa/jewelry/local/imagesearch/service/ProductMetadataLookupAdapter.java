package com.msa.jewelry.local.imagesearch.service;

import com.msa.jewelry.local.imagesearch.service.ProductImageSearchService.ProductMetadataLookup;
import com.msa.jewelry.local.product.entity.Product;
import com.msa.jewelry.local.product.entity.ProductImage;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ProductMetadataLookup 실제 구현체.
 *
 * - Product → material, classification fetch join 으로 N+1 방지
 * - 메인 이미지 path는 ProductImage.imageMain=true 한 건 조회
 * - color 도메인은 현재 시스템에 존재하지 않음 → null 반환
 *   (추후 ProductStone 또는 별도 Color 엔티티 도입 시 이 어댑터만 수정)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductMetadataLookupAdapter implements ProductMetadataLookup {

    // Spring Data JPA가 SharedEntityManagerCreator를 통해 자동 주입
    // (@PersistenceContext + final은 호환되지 않으므로 생성자 주입 사용)
    private final EntityManager em;

    // ============================================================
    // 상품 메타 일괄 조회 (Material + Classification fetch join)
    // ============================================================
    @Override
    public Map<Long, ProductMeta> findByIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        // 1) Product fetch join (material, classification)
        List<Product> products = em.createQuery(
                        "SELECT DISTINCT p FROM Product p " +
                                "LEFT JOIN FETCH p.material " +
                                "LEFT JOIN FETCH p.classification " +
                                "WHERE p.productId IN :ids",
                        Product.class)
                .setParameter("ids", productIds)
                .getResultList();

        // 2) 메인 이미지 path 일괄 조회 (imageMain=true)
        Map<Long, String> mainImagePathByProductId = em.createQuery(
                        "SELECT pi FROM ProductImage pi " +
                                "WHERE pi.imageMain = true " +
                                "AND pi.product.productId IN :ids",
                        ProductImage.class)
                .setParameter("ids", productIds)
                .getResultStream()
                .collect(Collectors.toMap(
                        pi -> pi.getProduct().getProductId(),
                        ProductImage::getImagePath,
                        (a, b) -> a   // 중복 방어
                ));

        // 3) ProductMeta 매핑
        Map<Long, ProductMeta> result = new HashMap<>();
        for (Product p : products) {
            Long materialId       = p.getMaterial() != null ? p.getMaterial().getMaterialId() : null;
            String materialName   = p.getMaterial() != null ? p.getMaterial().getMaterialName() : null;
            String classification = p.getClassification() != null
                    ? p.getClassification().getClassificationName()
                    : null;

            result.put(p.getProductId(), new ProductMeta(
                    p.getProductId(),
                    p.getProductName(),
                    mainImagePathByProductId.get(p.getProductId()),
                    materialId,
                    materialName,
                    null,    // colorId  — 시스템에 존재하지 않음
                    null,    // colorName — 동일
                    classification
            ));
        }
        return result;
    }

    // ============================================================
    // imageId → imagePath 일괄 조회
    // ============================================================
    @Override
    public Map<Long, String> findImagePaths(Collection<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return Map.of();
        }
        return em.createQuery(
                        "SELECT pi FROM ProductImage pi WHERE pi.imageId IN :ids",
                        ProductImage.class)
                .setParameter("ids", imageIds)
                .getResultStream()
                .collect(Collectors.toMap(
                        ProductImage::getImageId,
                        ProductImage::getImagePath,
                        (a, b) -> a
                ));
    }
}
