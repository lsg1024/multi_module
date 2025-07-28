package com.msa.product.local.stone.type.repository;

import com.msa.product.local.stone.type.dto.StoneTypeDto;

import java.util.List;

public interface CustomStoneTypeRepository {
    List<StoneTypeDto.ResponseSingle> findByStoneTypeAllOrderByAsc(String stoneTypeName);
}
