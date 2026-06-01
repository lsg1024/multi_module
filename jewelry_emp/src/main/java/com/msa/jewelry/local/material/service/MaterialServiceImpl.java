package com.msa.jewelry.local.material.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.material.dto.MaterialDto;
import com.msa.jewelry.local.material.entity.Material;
import com.msa.jewelry.local.material.repository.MaterialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.msa.jewelry.global.exception.ExceptionMessage.*;

@Slf4j
@Service
@Transactional
public class MaterialServiceImpl implements MaterialService {

    private final JwtUtil jwtUtil;
    private final JobLauncher jobLauncher;
    private final Job updateMaterialUpdateJob;
    private final MaterialRepository materialRepository;

    public MaterialServiceImpl(JwtUtil jwtUtil,
                               JobLauncher jobLauncher,
                               @Qualifier("updateMaterialUpdateJob") Job updateMaterialUpdateJob,
                               MaterialRepository materialRepository) {
        this.jwtUtil = jwtUtil;
        this.jobLauncher = jobLauncher;
        this.updateMaterialUpdateJob = updateMaterialUpdateJob;
        this.materialRepository = materialRepository;
    }

    @Override
    public void saveMaterial(MaterialDto materialDto) {
        if (materialRepository.existsByMaterialName(materialDto.getName())) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        Material material = Material.builder()
                .materialName(materialDto.getName())
                .materialGoldPurityPercent(parsePurityPercent(materialDto.getGoldPurityPercent()))
                .build();
        materialRepository.save(material);
    }

    private static BigDecimal parsePurityPercent(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("금 함량 퍼센트 는 필수입니다.");
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("금 함량 퍼센트가 올바른 숫자가 아닙니다: " + raw);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialDto.ResponseSingle getMaterial(Long materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        return MaterialDto.ResponseSingle.builder()
                .materialId(String.valueOf(materialId))
                .materialName(material.getMaterialName())
                .materialGoldPurityPercent(material.getMaterialGoldPurityPercent().toString())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialDto.ResponseSingle> getMaterials(String name) {
        List<MaterialDto.ResponseSingle> all = materialRepository.findAllOrderByAsc();
        if (name == null || name.isBlank()) return all;
        final String q = name.trim().toLowerCase();
        return all.stream()
                .filter(m -> m.getMaterialName() != null
                        && m.getMaterialName().toLowerCase().contains(q))
                .toList();
    }

    @Override
    public void updateMaterial(Long id, MaterialDto materialDto) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (!material.getMaterialName().equals(materialDto.getName())
                && materialRepository.existsByMaterialName(materialDto.getName())) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        material.updateMaterial(materialDto);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 배치 런칭은 트랜잭션 밖에서 (JobRepository "Existing transaction" 방지)
    public void deleteMaterial(String accessToken, Long id) {
        String role = jwtUtil.getRole(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        if (material.isDeletable()) {
            throw new IllegalArgumentException(CANNOT_DELETE_DEFAULT);
        }
        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", tenantId)
                    .addLong("materialId", id)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(updateMaterialUpdateJob, jobParameters);
        } catch (Exception e) {
            log.error("updateMaterialUpdateJob 실행 실패: materialId={}", id, e);
            throw new IllegalStateException(BATCH_FAIL, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getMaterialName(Long materialId) {
        return materialRepository.findById(materialId)
                .map(Material::getMaterialName)
                .orElseThrow(() -> new NotFoundException("재질 미존재: materialId=" + materialId));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getMaterialIdByName(String name) {
        return materialRepository.findByMaterialNameIgnoreCase(name)
                .map(Material::getMaterialId)
                .orElse(null);
    }
}
