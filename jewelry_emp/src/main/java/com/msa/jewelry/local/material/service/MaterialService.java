package com.msa.jewelry.local.material.service;

import com.msa.jewelry.local.material.dto.MaterialDto;

import java.util.List;

public interface MaterialService {

    void saveMaterial(MaterialDto materialDto);

    MaterialDto.ResponseSingle getMaterial(Long materialId);

    List<MaterialDto.ResponseSingle> getMaterials(String name);

    void updateMaterial(Long id, MaterialDto materialDto);

    void deleteMaterial(String accessToken, Long id);

    String getMaterialName(Long materialId);

    Long getMaterialIdByName(String name);
}
