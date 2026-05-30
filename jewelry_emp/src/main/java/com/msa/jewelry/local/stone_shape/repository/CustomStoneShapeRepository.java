package com.msa.jewelry.local.stone_shape.repository;

import com.msa.jewelry.local.stone_shape.dto.StoneShapeDto;

import java.util.List;

public interface CustomStoneShapeRepository {
    List<StoneShapeDto.ResponseSingle> findByStoneShapeAllOrderByAsc(String stoneShapeName);
}
