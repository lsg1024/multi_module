package com.msa.jewelry.local.set.repository;

import com.msa.jewelry.local.set.dto.SetTypeDto;

import java.util.List;

public interface CustomSetTypeRepository {
    List<SetTypeDto.ResponseSingle> findAllOrderByAsc(String setName);
}
