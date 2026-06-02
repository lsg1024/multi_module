package com.msa.jewelry.local.stone_type.service;

import com.msa.jewelry.local.stone_type.dto.StoneTypeDto;
import com.msa.jewelry.local.stone_type.entity.StoneType;
import com.msa.jewelry.local.stone_type.repository.StoneTypeRepository;
import com.msa.common.global.jwt.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.jewelry.global.exception.ExceptionMessage.*;

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
        String name = stoneTypeDto.getName() == null ? null : stoneTypeDto.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("스톤 타입명은 필수입니다.");
        }
        if (stoneTypeRepository.existsByStoneTypeName(name)) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        StoneType stoneType = StoneType.builder()
                .stoneTypeName(name)
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
        if (!stoneType.getStoneTypeName().equals(stoneTypeDto.getName()) && existsByTypeName) {
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

        if (stoneType.isStoneTypeDefault()) {
            throw new IllegalArgumentException("기본 스톤 타입은 삭제할 수 없습니다.");
        }

        stoneTypeRepository.delete(stoneType);
    }
}
