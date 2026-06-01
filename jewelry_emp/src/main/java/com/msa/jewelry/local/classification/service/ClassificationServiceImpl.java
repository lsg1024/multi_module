package com.msa.jewelry.local.classification.service;

import com.msa.jewelry.local.classification.dto.ClassificationDto;
import com.msa.jewelry.local.classification.entity.Classification;
import com.msa.jewelry.local.classification.repository.ClassificationRepository;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.common.global.jwt.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.jewelry.global.exception.ExceptionMessage.*;

@Slf4j
@Service
@Transactional
public class ClassificationServiceImpl implements ClassificationService {

    private final JwtUtil jwtUtil;
    private final JobLauncher jobLauncher;
    private final Job updateClassificationJob;
    private final ClassificationRepository classificationRepository;

    public ClassificationServiceImpl(JwtUtil jwtUtil,
                                     JobLauncher jobLauncher,
                                     @Qualifier("updateClassificationJob") Job updateClassificationJob,
                                     ClassificationRepository classificationRepository) {
        this.jwtUtil = jwtUtil;
        this.jobLauncher = jobLauncher;
        this.updateClassificationJob = updateClassificationJob;
        this.classificationRepository = classificationRepository;
    }

    @Override
    public void saveClassification(ClassificationDto classificationDto) {
        if (classificationRepository.existsByClassificationName(classificationDto.getName())) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        Classification classification = Classification.builder()
                .classificationName(classificationDto.getName())
                .classificationNote(classificationDto.getNote())
                .build();
        classificationRepository.save(classification);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassificationDto.ResponseSingle getClassification(Long classificationId) {
        Classification classification = classificationRepository.findById(classificationId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        return ClassificationDto.ResponseSingle.builder()
                .classificationId(String.valueOf(classificationId))
                .classificationName(classification.getClassificationName())
                .classificationNote(classification.getClassificationNote())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassificationDto.ResponseSingle> getClassifications(String classificationName) {
        return classificationRepository.findAllOrderByAsc(classificationName);
    }

    @Override
    public void updateClassification(Long classificationId, ClassificationDto classificationDto) {
        Classification classification = classificationRepository.findById(classificationId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (!classification.getClassificationName().equals(classificationDto.getName())
                && classificationRepository.existsByClassificationName(classificationDto.getName())) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        classification.updateClassification(classificationDto);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 배치 런칭은 트랜잭션 밖에서 (JobRepository "Existing transaction" 방지)
    public void deletedClassification(String accessToken, Long classificationId) {
        String role = jwtUtil.getRole(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);

        Classification classification = classificationRepository.findById(classificationId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (classification.isDeletable()) {
            throw new IllegalArgumentException(CANNOT_DELETE_DEFAULT);
        }
        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", tenantId)
                    .addLong("classificationId", classificationId)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(updateClassificationJob, jobParameters);
        } catch (Exception e) {
            log.error("updateClassificationJob 실행 실패: classificationId={}", classificationId, e);
            throw new IllegalStateException(BATCH_FAIL, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getClassificationName(Long classificationId) {
        return classificationRepository.findById(classificationId)
                .map(Classification::getClassificationName)
                .orElseThrow(() -> new NotFoundException("분류 미존재: classificationId=" + classificationId));
    }
}
