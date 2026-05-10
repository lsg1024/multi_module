package com.msa.jewelry.product.internal.classification.repository;

import com.msa.jewelry.product.internal.classification.dto.ClassificationDto;

import java.util.List;

public interface CustomClassificationRepository {
    List<ClassificationDto.ResponseSingle> findAllOrderByAsc(String classificationName);
}
