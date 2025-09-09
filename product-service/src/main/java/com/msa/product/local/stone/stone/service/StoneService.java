package com.msa.product.local.stone.stone.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.stone.stone.entity.StoneWorkGradePolicy;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class StoneService {

    private final JwtUtil jwtUtil;
    private final StoneRepository stoneRepository;

    public StoneService(JwtUtil jwtUtil, StoneRepository stoneRepository) {
        this.jwtUtil = jwtUtil;
        this.stoneRepository = stoneRepository;
    }

    //생성
    public void saveStone(StoneDto stoneDto) {
        boolean existsByStoneName = stoneRepository.existsByStoneName(stoneDto.getStoneName());
        if (existsByStoneName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        BigDecimal weight = Optional.ofNullable(stoneDto.getStoneWeight())
                .filter(s -> !s.isBlank())
                .map(BigDecimal::new)
                .orElse(BigDecimal.ZERO);

        String note = Optional.ofNullable(stoneDto.getStoneNote())
                .orElse("");

        Integer purchasePrice = Optional.ofNullable(stoneDto.getStonePurchasePrice())
                .orElse(0);

        Stone stone = Stone.builder()
                .stoneName(stoneDto.getStoneName())
                .stoneNote(note)
                .stoneWeight(weight)
                .stonePurchasePrice(purchasePrice)
                .gradePolicies(new ArrayList<>())
                .build();

        for (StoneWorkGradePolicyDto dto : stoneDto.getStoneWorkGradePolicyDto()) {
            Integer laborCost = Optional.ofNullable(dto.getLaborCost())
                    .orElse(0);

            StoneWorkGradePolicy stoneWorkGradePolicy = StoneWorkGradePolicy.builder()
                    .grade(dto.getGrade())
                    .laborCost(laborCost)
                    .build();

            stone.addGradePolicy(stoneWorkGradePolicy);
        }

        stoneRepository.save(stone);
    }

    //단건조회
    public StoneDto.ResponseSingle getStone(Long stoneId) {
        Stone stone = getStoneEntity(stoneRepository.findFetchJoinById(stoneId));

        List<StoneWorkGradePolicyDto.Response> policyDtos = stone.getGradePolicies().stream()
                .map(StoneWorkGradePolicyDto.Response::fromEntity)
                .collect(Collectors.toList());

        return StoneDto.ResponseSingle.builder()
                .stoneId(String.valueOf(stoneId))
                .stoneName(stone.getStoneName())
                .stoneNote(stone.getStoneNote())
                .stoneWeight(stone.getStoneWeight().stripTrailingZeros().toPlainString())
                .stonePurchasePrice(stone.getStonePurchasePrice())
                .stoneWorkGradePolicyDto(policyDtos)
                .build();
    }

    //복수조회 + 검색 + page
    public CustomPage<StoneDto.PageDto> getStones(String stoneName, Pageable pageable) {
        return stoneRepository.findByAllOrderByAsc(stoneName, pageable);
    }

    //수정
    public void updateStone(Long stoneId, StoneDto stoneDto) {
        Stone stone = getStoneEntity(stoneRepository.findById(stoneId));

        boolean existsByStoneName = stoneRepository.existsByStoneName(stoneDto.getStoneName());
        if (existsByStoneName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        stone.updateStone(stoneDto);
        stone.getGradePolicies().clear();

        for (StoneWorkGradePolicyDto dto : stoneDto.getStoneWorkGradePolicyDto()) {
            StoneWorkGradePolicy policy = StoneWorkGradePolicy.builder()
                    .grade(dto.getGrade())
                    .laborCost(dto.getLaborCost())
                    .build();
            stone.addGradePolicy(policy);
        }
    }

    //삭제
    public void deletedStone(String accessToken, Long stoneId) {
        String role = jwtUtil.getRole(accessToken);

        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        Stone stone = getStoneEntity(stoneRepository.findById(stoneId));

        stoneRepository.delete(stone);
    }

    private Stone getStoneEntity(Optional<Stone> stoneRepository) {
        return stoneRepository
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
    }

    public Boolean getExistStoneId(Long id) {
        return stoneRepository.existsByStoneId(id);
    }

    public Boolean getExistStoneName(String stoneTypeName, String stoneShapeName, String stoneSize) {
        String stoneName = stoneTypeName + "/" + stoneShapeName + "/" + stoneSize;
        return stoneRepository.existsByStoneName(stoneName);
    }
}
