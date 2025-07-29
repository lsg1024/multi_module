package com.msa.product.local.stone.stone.service;

import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.StoneWorkGradePolicy;
import com.msacommon.global.jwt.JwtUtil;
import com.msacommon.global.util.CustomPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Slf4j
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

        Stone stone = Stone.builder()
                .stoneName(stoneDto.getStoneName())
                .stoneNote(stoneDto.getStoneNote())
                .stoneWeight(weight)
                .stonePurchasePrice(stoneDto.getStonePurchasePrice())
                .gradePolicies(new ArrayList<>())
                .build();

        for (StoneWorkGradePolicyDto dto : stoneDto.getStoneWorkGradePolicyDto()) {
            StoneWorkGradePolicy stoneWorkGradePolicy = StoneWorkGradePolicy.builder()
                    .grade(dto.getGrade())
                    .laborCost(dto.getLaborCost())
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
}
