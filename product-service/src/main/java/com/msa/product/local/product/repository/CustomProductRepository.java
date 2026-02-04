package com.msa.product.local.product.repository;

import com.msa.product.local.grade.WorkGrade;
import com.msa.product.local.product.dto.ProductDetailDto;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

public interface CustomProductRepository {
    ProductDto.Detail findByProductId(Long productId);
    CustomPage<ProductDto.Page> findByAllProductName(String search, String searchField, String searchMin, String searchMax, String grade, String sortField, String sortOrder, Pageable pageable);

    ProductDetailDto findProductDetail(Long productId, WorkGrade grade);

    /**
     * 관련번호로 관련 상품 목록 조회 (본인 제외)
     */
    java.util.List<ProductDto.RelatedProduct> findRelatedProducts(Long productId, String relatedNumber);
}
