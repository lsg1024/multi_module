package com.msa.product.local.product.repository.image;

import com.msa.product.local.product.dto.ProductImageDto;

import java.util.List;
import java.util.Map;

public interface CustomProductImageRepository {
    List<ProductImageDto.Response> findImagesByProductId(Long productId);
    Map<Long, ProductImageDto.ApiResponse> findMainImagesByProductIds(List<Long> productIds);
}
