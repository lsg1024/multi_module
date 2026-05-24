package com.msa.jewelry.local.product.repository.stone;

import com.msa.jewelry.local.product.dto.ProductStoneDto;

import java.util.List;

public interface CustomProductStoneRepository {
    List<ProductStoneDto.Response> findProductStones(Long productId);
}
