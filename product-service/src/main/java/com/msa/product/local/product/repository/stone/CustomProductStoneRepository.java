package com.msa.product.local.product.repository.stone;

import com.msa.product.local.product.dto.ProductStoneDto;

import java.util.List;

public interface CustomProductStoneRepository {
    List<ProductStoneDto.Response> findProductStones(Long productId);
}
