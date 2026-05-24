package com.msa.jewelry.product.internal.api_impl;

import com.msa.jewelry.product.api.ClassificationFinder;
import com.msa.jewelry.product.internal.classification.entity.Classification;
import com.msa.jewelry.product.internal.classification.repository.ClassificationRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassificationFinderImpl implements ClassificationFinder {

    private final ClassificationRepository classificationRepository;

    @Override
    public String getClassificationName(Long classificationId) {
        return classificationRepository.findById(classificationId)
                .map(Classification::getClassificationName)
                .orElseThrow(() -> new NotFoundException("분류 미존재: classificationId=" + classificationId));
    }
}
