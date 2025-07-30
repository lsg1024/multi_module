package com.msa.product.local.product.repository;

import com.msa.product.local.product.dto.ProductDto;

public interface CustomProductRepository {
    ProductDto.Detail findByProductId(Long productId);
}
