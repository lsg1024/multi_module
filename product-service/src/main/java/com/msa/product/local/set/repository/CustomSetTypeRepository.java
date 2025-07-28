package com.msa.product.local.set.repository;

import com.msa.product.local.set.dto.SetTypeDto;

import java.util.List;

public interface CustomSetTypeRepository {
    List<SetTypeDto.ResponseSingle> findAllOrderByAsc();
}
