package com.msa.product.local.material.service;

import com.msa.product.global.kafka.KafkaProducer;
import com.msa.product.local.material.dto.MaterialDto;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.common.global.jwt.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class MaterialService {

    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final MaterialRepository materialRepository;

    public MaterialService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, MaterialRepository materialRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.materialRepository = materialRepository;
    }

    // 생성
    public void saveMaterial(MaterialDto materialDto) {
        boolean existsByMaterialName = materialRepository.existsByMaterialName(materialDto.getName());
        if (existsByMaterialName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        Material material = Material.builder()
                .materialName(materialDto.getName())
                .materialGoldPurityPercent(new BigDecimal(materialDto.getGoldPurityPercent()))
                .build();

        materialRepository.save(material);
    }

    // 단건 조회
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

    // 복수 조회
    @Transactional(readOnly = true)
    public List<MaterialDto.ResponseSingle> getMaterials() {
        return materialRepository.findAllOrderByAsc();
    }

    // 수정
    public void updateMaterial(Long id, MaterialDto materialDto) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean existsByMaterialName = materialRepository.existsByMaterialName(materialDto.getName());
        if (!material.getMaterialName().equals(materialDto.getName()) && existsByMaterialName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        material.updateMaterial(materialDto);
    }

    // 삭제
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

        kafkaProducer.sendMaterialUpdate(tenantId, id);
    }

    public String getMaterialName(Long id) {
        return materialRepository.findByMaterialName(id);
    }
}

