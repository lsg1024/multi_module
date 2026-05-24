package com.msa.jewelry.local.set.service;

import com.msa.jewelry.local.set.dto.SetTypeDto;

import java.util.List;

public interface SetTypeService {

    void saveSetType(SetTypeDto setTypeDto);

    SetTypeDto.ResponseSingle getSetType(Long setTypeId);

    List<SetTypeDto.ResponseSingle> getSetTypes(String setName);

    void updateSetType(Long setTypeId, SetTypeDto setTypeDto);

    void deletedSetType(String accessToken, Long setTypeId);

    String getSetTypeName(Long setTypeId);
}
