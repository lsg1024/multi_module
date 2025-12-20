package com.msa.product.local.stone.shape.service;

import com.msa.product.local.stone.shape.dto.StoneShapeDto;
import com.msa.product.local.stone.shape.entity.StoneShape;
import com.msa.product.local.stone.shape.repository.StoneShapeRepository;
import com.msa.common.global.jwt.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class StoneShapeService {

    private final JwtUtil jwtUtil;
    private final StoneShapeRepository stoneShapeRepository;

    public StoneShapeService(JwtUtil jwtUtil, StoneShapeRepository stoneShapeRepository) {
        this.jwtUtil = jwtUtil;
        this.stoneShapeRepository = stoneShapeRepository;
    }

    // 생성
    public void saveStoneShape(StoneShapeDto stoneShapeDto) {
        boolean existsByShapeName = stoneShapeRepository.existsByStoneShapeName(stoneShapeDto.getStoneShapeName());
        if (existsByShapeName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        StoneShape stoneShape = StoneShape.builder()
                .stoneShapeName(stoneShapeDto.getStoneShapeName())
                .stoneShapeNote(stoneShapeDto.getStoneShapeNote())
                .build();

        stoneShapeRepository.save(stoneShape);
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public StoneShapeDto.ResponseSingle getStoneShape(Long stoneShapeId) {
        StoneShape stoneShape = stoneShapeRepository.findById(stoneShapeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        return StoneShapeDto.ResponseSingle.builder()
                .stoneShapeId(String.valueOf(stoneShapeId))
                .stoneShapeName(stoneShape.getStoneShapeName())
                .stoneShapeNote(stoneShape.getStoneShapeNote())
                .build();
    }

    // 복수 조회
    @Transactional(readOnly = true)
    public List<StoneShapeDto.ResponseSingle> getStoneShapes(String stoneShapeName) {
        return stoneShapeRepository.findByStoneShapeAllOrderByAsc(stoneShapeName);
    }

    // 수정
    public void updateStoneShape(Long stoneShapeId, StoneShapeDto stoneShapeDto) {
        StoneShape stoneShape = stoneShapeRepository.findById(stoneShapeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean existsByShapeName = stoneShapeRepository.existsByStoneShapeName(stoneShapeDto.getStoneShapeName());
        if (stoneShape.getStoneShapeName().equals(stoneShapeDto.getStoneShapeName()) && existsByShapeName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        stoneShape.updateStoneShape(stoneShapeDto);
    }

    // 삭제
    public void deleteStoneShape(String accessToken, Long stoneShapeId) {
        String role = jwtUtil.getRole(accessToken);

        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        StoneShape stoneShape = stoneShapeRepository.findById(stoneShapeId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        stoneShapeRepository.delete(stoneShape);
    }
}
