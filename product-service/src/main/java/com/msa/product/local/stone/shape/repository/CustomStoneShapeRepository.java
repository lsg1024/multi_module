package com.msa.product.local.stone.shape.repository;

import com.msa.product.local.stone.shape.dto.StoneShapeDto;

import java.util.List;

public interface CustomStoneShapeRepository {
    List<StoneShapeDto.ResponseSingle> findByStoneShapeAllOrderByAsc(String stoneShapeName);
}
