package com.msa.jewelry.product.internal.api_impl;

import com.msa.jewelry.product.api.SetTypeFinder;
import com.msa.jewelry.product.internal.set.entity.SetType;
import com.msa.jewelry.product.internal.set.repository.SetTypeRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SetTypeFinderImpl implements SetTypeFinder {

    private final SetTypeRepository setTypeRepository;

    @Override
    public String getSetTypeName(Long setTypeId) {
        return setTypeRepository.findById(setTypeId)
                .map(SetType::getSetTypeName)
                .orElseThrow(() -> new NotFoundException("세트타입 미존재: setTypeId=" + setTypeId));
    }
}
