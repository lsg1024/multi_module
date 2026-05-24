package com.msa.jewelry.local.classification.repository;

import com.msa.jewelry.local.classification.dto.ClassificationDto;

import java.util.List;

public interface CustomClassificationRepository {
    List<ClassificationDto.ResponseSingle> findAllOrderByAsc(String classificationName);
}
