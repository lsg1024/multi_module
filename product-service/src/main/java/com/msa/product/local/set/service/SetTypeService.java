package com.msa.product.local.set.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.product.global.kafka.KafkaProducer;
import com.msa.product.local.set.dto.SetTypeDto;
import com.msa.product.local.set.entity.SetType;
import com.msa.product.local.set.repository.SetTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class SetTypeService {

    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final SetTypeRepository setTypeRepository;

    public SetTypeService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, SetTypeRepository setTypeRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.setTypeRepository = setTypeRepository;
    }

    //생성
    public void saveSetType(SetTypeDto setTypeDto) {
        boolean existsBySetTypeName = setTypeRepository.existsBySetTypeName(setTypeDto.getName());
        if (existsBySetTypeName) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        SetType setType = SetType.builder()
                .setTypeName(setTypeDto.getName())
                .setTypeNote(setTypeDto.getNote())
                .build();
        setTypeRepository.save(setType);
    }

    //조회 단일
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

    //조회 복수
    @Transactional(readOnly = true)
    public List<SetTypeDto.ResponseSingle> getSetTypes(String setName) {
        return setTypeRepository.findAllOrderByAsc(setName);
    }

    //수정
    public void updateSetType(Long setTypeId, SetTypeDto setTypeDto) {
        SetType setType = setTypeRepository.findById(setTypeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean existsBySetTypeName = setTypeRepository.existsBySetTypeName(setTypeDto.getName());
        if (existsBySetTypeName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        setType.updateSetType(setTypeDto);
    }

    //삭제
    public void deletedSetType(String accessToken, Long setTypeId) {
        String role = jwtUtil.getRole(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);

        SetType setType = setTypeRepository.findById(setTypeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean deletable = setType.isDeletable();
        if (deletable) {
            throw new IllegalArgumentException(CANNOT_DELETE_DEFAULT);
        }

        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        kafkaProducer.sendSetTypeUpdate(tenantId, setTypeId);
    }

    public String getSetTypeName(Long id) {
        return setTypeRepository.findByMaterialName(id);
    }
}
