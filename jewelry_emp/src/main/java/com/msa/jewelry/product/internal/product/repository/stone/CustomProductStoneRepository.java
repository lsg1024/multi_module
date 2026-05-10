package com.msa.jewelry.product.internal.product.repository.stone;

import com.msa.jewelry.product.internal.product.dto.ProductStoneDto;

import java.util.List;

public interface CustomProductStoneRepository {
    List<ProductStoneDto.Response> findProductStones(Long productId);
}
