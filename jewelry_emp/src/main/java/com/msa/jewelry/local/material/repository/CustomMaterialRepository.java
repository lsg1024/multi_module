package com.msa.jewelry.local.material.repository;

import com.msa.jewelry.local.material.dto.MaterialDto;

import java.util.List;

public interface CustomMaterialRepository {
    List<MaterialDto.ResponseSingle> findAllOrderByAsc();
}
