package com.msa.product.local.classification.repository;

import com.msa.product.local.classification.dto.ClassificationDto;

import java.util.List;

public interface CustomClassificationRepository {
    List<ClassificationDto.ResponseSingle> findAllOrderByAsc(String classificationName);
}
