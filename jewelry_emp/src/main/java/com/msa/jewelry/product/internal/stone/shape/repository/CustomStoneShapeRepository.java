package com.msa.jewelry.product.internal.stone.shape.repository;

import com.msa.jewelry.product.internal.stone.shape.dto.StoneShapeDto;

import java.util.List;

public interface CustomStoneShapeRepository {
    List<StoneShapeDto.ResponseSingle> findByStoneShapeAllOrderByAsc(String stoneShapeName);
}
