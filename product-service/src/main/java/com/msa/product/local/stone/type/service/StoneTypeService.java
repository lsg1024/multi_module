package com.msa.product.local.stone.type.service;

import com.msa.product.local.stone.type.dto.StoneTypeDto;
import com.msa.product.local.stone.type.entity.StoneType;
import com.msa.product.local.stone.type.repository.StoneTypeRepository;
import com.msa.common.global.jwt.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class StoneTypeService {

    private final JwtUtil jwtUtil;
    private final StoneTypeRepository stoneTypeRepository;

    public StoneTypeService(JwtUtil jwtUtil, StoneTypeRepository stoneTypeRepository) {
        this.jwtUtil = jwtUtil;
        this.stoneTypeRepository = stoneTypeRepository;
    }

    // 생성
    public void saveStoneType(StoneTypeDto stoneTypeDto) {
        boolean existsByTypeName = stoneTypeRepository.existsByStoneTypeName(stoneTypeDto.getName());
        if (existsByTypeName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        StoneType stoneType = StoneType.builder()
                .stoneTypeName(stoneTypeDto.getName())
                .stoneTypeNote(stoneTypeDto.getNote())
                .build();

        stoneTypeRepository.save(stoneType);
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public StoneTypeDto.ResponseSingle getStoneType(Long stoneTypeId) {
        StoneType stoneType = stoneTypeRepository.findById(stoneTypeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        return StoneTypeDto.ResponseSingle.builder()
                .stoneTypeId(String.valueOf(stoneTypeId))
                .stoneTypeName(stoneType.getStoneTypeName())
                .stoneTypeNote(stoneType.getStoneTypeNote())
                .build();
    }

    // 복수 조회
    @Transactional(readOnly = true)
    public List<StoneTypeDto.ResponseSingle> getStoneTypes(String stoneTypeName) {
        return stoneTypeRepository.findByStoneTypeAllOrderByAsc(stoneTypeName);
    }

    // 수정
    public void updateStoneType(Long stoneTypeId, StoneTypeDto stoneTypeDto) {
        StoneType stoneType = stoneTypeRepository.findById(stoneTypeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean existsByTypeName = stoneTypeRepository.existsByStoneTypeName(stoneTypeDto.getName());
        if (existsByTypeName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        stoneType.updateStoneType(stoneTypeDto);
    }

    // 삭제
    public void deleteStoneType(String accessToken, Long stoneTypeId) {
        String role = jwtUtil.getRole(accessToken);

        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        StoneType stoneType = stoneTypeRepository.findById(stoneTypeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        stoneTypeRepository.delete(stoneType);
    }
}
