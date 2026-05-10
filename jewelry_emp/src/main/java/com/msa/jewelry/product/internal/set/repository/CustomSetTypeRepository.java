package com.msa.jewelry.product.internal.set.repository;

import com.msa.jewelry.product.internal.set.dto.SetTypeDto;

import java.util.List;

public interface CustomSetTypeRepository {
    List<SetTypeDto.ResponseSingle> findAllOrderByAsc(String setName);
}
