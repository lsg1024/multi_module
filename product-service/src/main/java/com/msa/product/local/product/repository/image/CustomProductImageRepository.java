package com.msa.product.local.product.repository.image;

import com.msa.product.local.product.dto.ProductImageDto;

import java.util.List;

public interface CustomProductImageRepository {
    List<ProductImageDto.Response> findImagesByProductId(Long productId);
}
