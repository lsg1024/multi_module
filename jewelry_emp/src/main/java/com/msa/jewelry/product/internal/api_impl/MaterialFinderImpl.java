package com.msa.jewelry.product.internal.api_impl;

import com.msa.jewelry.product.api.MaterialFinder;
import com.msa.jewelry.product.internal.material.entity.Material;
import com.msa.jewelry.product.internal.material.repository.MaterialRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialFinderImpl implements MaterialFinder {

    private final MaterialRepository materialRepository;

    @Override
    public String getMaterialName(Long materialId) {
        return materialRepository.findById(materialId)
                .map(Material::getMaterialName)
                .orElseThrow(() -> new NotFoundException("재질 미존재: materialId=" + materialId));
    }

    @Override
    public Long findMaterialIdByName(String materialName) {
        return materialRepository.findByMaterialNameIgnoreCase(materialName)
                .map(Material::getMaterialId)
                .orElse(null);
    }
}
