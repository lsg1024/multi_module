package com.msa.jewelry.local.product.repository.image;

import com.msa.jewelry.local.product.dto.ProductImageDto;

import java.util.List;
import java.util.Map;

public interface CustomProductImageRepository {
    List<ProductImageDto.Response> findImagesByProductId(Long productId);
    Map<Long, ProductImageDto.ProductImageResponse> findMainImagesByProductIds(List<Long> productIds);
}
