package com.msa.jewelry.product.internal.api_impl;

import com.msa.jewelry.product.api.ProductDetailView;
import com.msa.jewelry.product.api.ProductFinder;
import com.msa.jewelry.product.api.ProductImageView;
import com.msa.jewelry.product.api.ProductStoneView;
import com.msa.jewelry.product.api.ProductView;
import com.msa.jewelry.product.internal.product.dto.ProductDetailDto;
import com.msa.jewelry.product.internal.product.dto.ProductImageDto;
import com.msa.jewelry.product.internal.product.entity.Product;
import com.msa.jewelry.product.internal.product.repository.ProductRepository;
import com.msa.jewelry.product.internal.product.service.ProductImageService;
import com.msa.jewelry.product.internal.product.service.ProductService;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link ProductFinder} 의 같은 JVM 동기 구현체.
 *
 * <p>2026-05 P2-1b 단계에서 ProductClient(Feign wrapper) 의 7개 메서드를 모두 흡수.
 * 내부적으로는 {@link ProductService} / {@link ProductImageService} 에 위임하고
 * 반환 객체를 product/api 의 view record 로 변환한다.
 *
 * <p>내부 DTO ({@link ProductDetailDto}, {@link ProductImageDto.ProductImageResponse}) 가
 * 다른 모듈로 직접 노출되면 모듈 경계가 깨지므로 반드시 view 로 어댑팅한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFinderImpl implements ProductFinder {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ProductImageService productImageService;

    /* ============================================================
     * Simple view (마스터 데이터)
     * ============================================================ */

    @Override
    public ProductView getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> Boolean.FALSE.equals(p.getProductDeleted()))
                .orElseThrow(() -> new NotFoundException("상품 미존재: productId=" + productId));
        return toView(product);
    }

    @Override
    public ProductView findProductByName(String productName) {
        Product product = productRepository.findByProductName(productName)
                .orElseThrow(() -> new NotFoundException("상품 미존재: name=" + productName));
        return toView(product);
    }

    private static ProductView toView(Product entity) {
        return new ProductView(
                entity.getProductId(),
                entity.getProductName(),
                entity.getMaterial() != null ? entity.getMaterial().getMaterialName() : null,
                entity.getClassification() != null
                        ? entity.getClassification().getClassificationName() : null,
                entity.getSetType() != null ? entity.getSetType().getSetTypeName() : null,
                entity.getFactoryId(),
                entity.getFactoryName(),
                entity.getStandardWeight()
        );
    }

    /* ============================================================
     * 상세 view (가격 + 스톤 포함)
     * ============================================================ */

    @Override
    public ProductDetailView getProductDetail(Long productId, String grade) {
        ProductDetailDto dto = productService.getProductInfo(productId, grade);
        if (dto == null) {
            throw new NotFoundException("상품 상세 미존재: productId=" + productId + " grade=" + grade);
        }
        return toDetailView(dto);
    }

    @Override
    public ProductDetailView findProductDetailByName(String productName) {
        ProductDetailDto dto = productService.getProductInfoByName(productName);
        return dto != null ? toDetailView(dto) : null;
    }

    @Override
    public List<ProductStoneView> getProductStonesByName(String productName) {
        List<ProductDetailDto.StoneInfo> stones = productService.getProductStonesByName(productName);
        return stones.stream().map(ProductFinderImpl::toStoneView).toList();
    }

    private static ProductDetailView toDetailView(ProductDetailDto dto) {
        return new ProductDetailView(
                dto.getProductId(),
                dto.getProductName(),
                dto.getProductFactoryName(),
                dto.getClassificationId(),
                dto.getClassificationName(),
                dto.getSetTypeId(),
                dto.getSetTypeName(),
                dto.getPurchaseCost(),
                dto.getLaborCost()
        );
    }

    private static ProductStoneView toStoneView(ProductDetailDto.StoneInfo s) {
        return new ProductStoneView(
                s.getStoneId(),
                s.getStoneName(),
                s.getStoneWeight(),
                s.getPurchaseCost(),
                s.getLaborCost(),
                s.getQuantity(),
                s.isMainStone(),
                s.isIncludeStone(),
                s.isIncludeQuantity(),
                s.isIncludePrice(),
                s.getStoneNote()
        );
    }

    /* ============================================================
     * 이미지
     * ============================================================ */

    @Override
    public Map<Long, ProductImageView> getProductImages(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ProductImageDto.ProductImageResponse> imagesByProductIds =
                productImageService.getImagesByProductIds(productIds);
        return imagesByProductIds.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ProductImageView(
                                e.getValue().getProductId(),
                                e.getValue().getImagePath()
                        )
                ));
    }

    /* ============================================================
     * 갱신 (read-only 클래스이지만 본 메서드는 쓰기 트랜잭션 필요)
     * ============================================================ */

    @Override
    @Transactional
    public void updateProductFactoryName(Long productId, String productFactoryName) {
        productService.updateProductFactoryName(productId, productFactoryName);
    }
}
