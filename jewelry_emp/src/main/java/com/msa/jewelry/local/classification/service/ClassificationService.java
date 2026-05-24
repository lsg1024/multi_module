package com.msa.jewelry.local.classification.service;

import com.msa.jewelry.local.classification.dto.ClassificationDto;

import java.util.List;

public interface ClassificationService {

    void saveClassification(ClassificationDto classificationDto);

    ClassificationDto.ResponseSingle getClassification(Long classificationId);

    List<ClassificationDto.ResponseSingle> getClassifications(String classificationName);

    void updateClassification(Long classificationId, ClassificationDto classificationDto);

    void deletedClassification(String accessToken, Long classificationId);

    String getClassificationName(Long classificationId);
}
