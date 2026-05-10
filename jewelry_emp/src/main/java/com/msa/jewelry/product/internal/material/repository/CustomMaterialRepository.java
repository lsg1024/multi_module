package com.msa.jewelry.product.internal.material.repository;

import com.msa.jewelry.product.internal.material.dto.MaterialDto;

import java.util.List;

public interface CustomMaterialRepository {
    List<MaterialDto.ResponseSingle> findAllOrderByAsc();
}
