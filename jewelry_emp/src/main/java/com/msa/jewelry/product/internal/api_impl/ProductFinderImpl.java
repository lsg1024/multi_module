package com.msa.jewelry.product.internal.api_impl;

import com.msa.jewelry.product.api.ProductFinder;
import com.msa.jewelry.product.api.ProductView;
import com.msa.jewelry.product.internal.product.entity.Product;
import com.msa.jewelry.product.internal.product.repository.ProductRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ProductFinder} 의 같은 JVM 동기 구현체.
 *
 * <p>기존 ProductFeignClient.getProductInfo / getProductInfoByName 를 대체.
 *
 * <p>주의: Product 엔티티에는 color / goldWeight / productLaborCost 가 없다.
 * 색상은 별도 카탈로그 옵션이고, 무게·공임은 ProductWorkGradePolicy / ProductStone 에 분산.
 * view 는 마스터 데이터만 노출.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFinderImpl implements ProductFinder {

    private final ProductRepository productRepository;

    @Override
    public ProductView getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> Boolean.FALSE.equals(p.getProductDeleted()))
                .orElseThrow(() -> new NotFoundException("상품 미존재: productId=" + productId));
        return toView(product);
    }

    @Override
    public ProductView findProductByName(String productName) {
        // ProductRepository 에 findByProductName(String) 메서드가 추가되면 활성화.
        // 현재는 productId 기반 조회만 권장.
        throw new UnsupportedOperationException(
                "findProductByName 은 ProductRepository.findByProductNameIgnoreCase 메서드가" +
                        " 추가된 후 활성화. 현재는 getProduct(productId) 사용.");
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
}
