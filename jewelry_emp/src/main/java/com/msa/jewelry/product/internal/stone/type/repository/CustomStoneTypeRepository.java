package com.msa.jewelry.product.internal.stone.type.repository;

import com.msa.jewelry.product.internal.stone.type.dto.StoneTypeDto;

import java.util.List;

public interface CustomStoneTypeRepository {
    List<StoneTypeDto.ResponseSingle> findByStoneTypeAllOrderByAsc(String stoneTypeName);
}
