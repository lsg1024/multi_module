package com.msa.product.local.set.service;

import com.msa.product.local.set.dto.SetTypeDto;
import com.msa.product.local.set.entity.SetType;
import com.msa.product.local.set.repository.SetTypeRepository;
import com.msacommon.global.jwt.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class SetTypeService {

    private final JwtUtil jwtUtil;
    private final SetTypeRepository setTypeRepository;

    public SetTypeService(JwtUtil jwtUtil, SetTypeRepository setTypeRepository) {
        this.jwtUtil = jwtUtil;
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
    public List<SetTypeDto.ResponseSingle> getSetTypes() {
        return setTypeRepository.findAllOrderByAsc();
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
        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        SetType setType = setTypeRepository.findById(setTypeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        setTypeRepository.delete(setType);

        //kafka로 기본 값으로 product setType 전부 변경
    }
}
