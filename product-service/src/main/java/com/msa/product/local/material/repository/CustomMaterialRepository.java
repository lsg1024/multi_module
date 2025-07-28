package com.msa.product.local.material.repository;

import com.msa.product.local.material.dto.MaterialDto;

import java.util.List;

public interface CustomMaterialRepository {
    List<MaterialDto.ResponseSingle> findAllOrderByAsc();
}
