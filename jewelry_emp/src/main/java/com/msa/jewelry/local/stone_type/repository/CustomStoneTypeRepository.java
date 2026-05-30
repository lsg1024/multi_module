package com.msa.jewelry.local.stone_type.repository;

import com.msa.jewelry.local.stone_type.dto.StoneTypeDto;

import java.util.List;

public interface CustomStoneTypeRepository {
    List<StoneTypeDto.ResponseSingle> findByStoneTypeAllOrderByAsc(String stoneTypeName);
}
