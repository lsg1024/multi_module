package com.msa.jewelry.local.set.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.set.dto.SetTypeDto;
import com.msa.jewelry.local.set.entity.SetType;
import com.msa.jewelry.local.set.repository.SetTypeRepository;
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
public class SetTypeServiceImpl implements SetTypeService {

    private final JwtUtil jwtUtil;
    private final JobLauncher jobLauncher;
    private final Job updateSetTypeUpdateJob;
    private final SetTypeRepository setTypeRepository;

    public SetTypeServiceImpl(JwtUtil jwtUtil,
                              JobLauncher jobLauncher,
                              @Qualifier("updateSetTypeUpdateJob") Job updateSetTypeUpdateJob,
                              SetTypeRepository setTypeRepository) {
        this.jwtUtil = jwtUtil;
        this.jobLauncher = jobLauncher;
        this.updateSetTypeUpdateJob = updateSetTypeUpdateJob;
        this.setTypeRepository = setTypeRepository;
    }

    @Override
    public void saveSetType(SetTypeDto setTypeDto) {
        if (setTypeRepository.existsBySetTypeName(setTypeDto.getName())) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        SetType setType = SetType.builder()
                .setTypeName(setTypeDto.getName())
                .setTypeNote(setTypeDto.getNote())
                .build();
        setTypeRepository.save(setType);
    }

    @Override
    @Transactional(readOnly = true)
    public SetTypeDto.ResponseSingle getSetType(Long setTypeId) {
        SetType setType = setTypeRepository.findById(setTypeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        return SetTypeDto.ResponseSingle.builder()
                .setTypeId(String.valueOf(setTypeId))
                .setTypeName(setType.getSetTypeName())
                .setTypeNote(setType.getSetTypeNote())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SetTypeDto.ResponseSingle> getSetTypes(String setName) {
        return setTypeRepository.findAllOrderByAsc(setName);
    }

    @Override
    public void updateSetType(Long setTypeId, SetTypeDto setTypeDto) {
        SetType setType = setTypeRepository.findById(setTypeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (!setType.getSetTypeName().equals(setTypeDto.getName())
                && setTypeRepository.existsBySetTypeName(setTypeDto.getName())) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        setType.updateSetType(setTypeDto);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 배치 런칭은 트랜잭션 밖에서 (JobRepository "Existing transaction" 방지)
    public void deletedSetType(String accessToken, Long setTypeId) {
        String role = jwtUtil.getRole(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);

        SetType setType = setTypeRepository.findById(setTypeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (!setType.isDeletable()) {
            throw new IllegalArgumentException(CANNOT_DELETE_DEFAULT);
        }
        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", tenantId)
                    .addLong("setTypeId", setTypeId)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(updateSetTypeUpdateJob, jobParameters);
        } catch (Exception e) {
            log.error("updateSetTypeUpdateJob 실행 실패: setTypeId={}", setTypeId, e);
            throw new IllegalStateException(BATCH_FAIL, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getSetTypeName(Long setTypeId) {
        return setTypeRepository.findById(setTypeId)
                .map(SetType::getSetTypeName)
                .orElseThrow(() -> new NotFoundException("세트타입 미존재: setTypeId=" + setTypeId));
    }
}
