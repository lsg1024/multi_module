package com.msa.product.local.product.repository;

import com.msa.product.local.product.dto.ProductDto;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

public interface CustomProductRepository {
    ProductDto.Detail findByProductId(Long productId);
    CustomPage<ProductDto.Page> findByAllProductName(String productName, Pageable pageable);
}
