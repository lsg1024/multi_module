package com.msa.product.local.classification.service;

import com.msa.product.global.kafka.KafkaProducer;
import com.msa.product.local.classification.dto.ClassificationDto;
import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.common.global.jwt.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class ClassificationService {
    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final ClassificationRepository classificationRepository;

    public ClassificationService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, ClassificationRepository classificationRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.classificationRepository = classificationRepository;
    }

    //생성
    public void saveClassification(ClassificationDto classificationDto) {
        boolean existsByClassificationName = classificationRepository.existsByClassificationName(classificationDto.getName());
        if (existsByClassificationName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        Classification classification = Classification.builder()
                .classificationName(classificationDto.getName())
                .classificationNote(classificationDto.getNote())
                .build();

        classificationRepository.save(classification);
    }

    //단건 조회
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

    //복수 조회
    @Transactional(readOnly = true)
    public List<ClassificationDto.ResponseSingle> getClassifications(String classificationName) {
        return classificationRepository.findAllOrderByAsc(classificationName);
    }

    //수정
    public void updateClassification(Long classificationId, ClassificationDto classificationDto) {
        Classification classification = classificationRepository.findById(classificationId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean existsByClassificationName = classificationRepository.existsByClassificationName(classificationDto.getName());
        if (existsByClassificationName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        classification.updateClassification(classificationDto);
    }

    //삭제
    public void deletedClassification(String accessToken, Long classificationId) {
        String role = jwtUtil.getRole(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);

        Classification classification = classificationRepository.findById(classificationId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean deletable = classification.isDeletable();
        if (deletable) {
            throw new IllegalArgumentException(CANNOT_DELETE_DEFAULT);
        }

        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        // 카프카 이용해 기존 분류 값들을 기본 "" 으로 변경
        kafkaProducer.sendClassificationUpdate(tenantId, classificationId);
    }

    public String getClassificationName(Long id) {
        return classificationRepository.findByClassificationName(id);
    }
}
