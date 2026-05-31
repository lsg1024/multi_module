package com.msa.jewelry.local.stone.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.excel.dto.StoneExcelDto;
import com.msa.jewelry.global.excel.util.StoneExcelUtil;
import com.msa.jewelry.local.stone.dto.StoneDto;
import com.msa.jewelry.local.stone.dto.StoneWorkGradePolicyDto;
import com.msa.jewelry.local.stone.entity.Stone;
import com.msa.jewelry.local.stone.entity.StoneWorkGradePolicy;
import com.msa.jewelry.local.product.repository.stone.ProductStoneRepository;
import com.msa.jewelry.local.stone.repository.StoneRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.msa.jewelry.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class StoneServiceImpl implements StoneService {

    private final JwtUtil jwtUtil;
    private final StoneRepository stoneRepository;
    private final ProductStoneRepository productStoneRepository;

    public StoneServiceImpl(JwtUtil jwtUtil,
                            StoneRepository stoneRepository,
                            ProductStoneRepository productStoneRepository) {
        this.jwtUtil = jwtUtil;
        this.stoneRepository = stoneRepository;
        this.productStoneRepository = productStoneRepository;
    }

    @Override
    public void saveStone(StoneDto stoneDto) {
        if (stoneRepository.existsByStoneName(stoneDto.getStoneName())) {
            throw new IllegalArgumentException(IS_EXIST);
        }
        BigDecimal weight = Optional.ofNullable(stoneDto.getStoneWeight())
                .filter(s -> !s.isBlank())
                .map(BigDecimal::new)
                .orElse(BigDecimal.ZERO);
        String note = Optional.ofNullable(stoneDto.getStoneNote()).orElse("");
        Integer purchasePrice = Optional.ofNullable(stoneDto.getStonePurchasePrice()).orElse(0);

        Stone stone = Stone.builder()
                .stoneName(stoneDto.getStoneName())
                .stoneNote(note)
                .stoneWeight(weight)
                .stonePurchasePrice(purchasePrice)
                .gradePolicies(new ArrayList<>())
                .build();

        for (StoneWorkGradePolicyDto dto : stoneDto.getStoneWorkGradePolicyDto()) {
            Integer laborCost = Optional.ofNullable(dto.getLaborCost()).orElse(0);
            StoneWorkGradePolicy stoneWorkGradePolicy = StoneWorkGradePolicy.builder()
                    .grade(dto.getGrade())
                    .laborCost(laborCost)
                    .build();
            stone.addGradePolicy(stoneWorkGradePolicy);
        }
        stoneRepository.save(stone);
    }

    @Override
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

    @Override
    public CustomPage<StoneDto.PageDto> getStones(String search, String searchField, String searchMin, String searchMax, String sortField, String sortOrder, Pageable pageable) {
        return stoneRepository.findAllStones(search, searchField, searchMin, searchMax, sortField, sortOrder, pageable);
    }

    @Override
    public void updateStone(Long stoneId, StoneDto stoneDto) {
        Stone stone = getStoneEntity(stoneRepository.findById(stoneId));

        if (stoneRepository.existsByStoneNameAndStoneIdNot(stoneDto.getStoneName(), stoneId)) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        stone.updateStone(stoneDto);
        stone.getGradePolicies().clear();
        if (stoneDto.getStoneWorkGradePolicyDto() != null) {
            for (StoneWorkGradePolicyDto dto : stoneDto.getStoneWorkGradePolicyDto()) {
                StoneWorkGradePolicy policy = StoneWorkGradePolicy.builder()
                        .grade(dto.getGrade())
                        .laborCost(dto.getLaborCost())
                        .build();
                stone.addGradePolicy(policy);
            }
        }
    }

    @Override
    public void deletedStone(String accessToken, Long stoneId) {
        String role = jwtUtil.getRole(accessToken);
        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }
        Stone stone = getStoneEntity(stoneRepository.findById(stoneId));

        if (productStoneRepository.existsByStone_StoneId(stoneId)) {
            throw new IllegalArgumentException("이 스톤을 사용 중인 상품이 있어 삭제할 수 없습니다.");
        }
        stoneRepository.delete(stone);
    }

    private Stone getStoneEntity(Optional<Stone> opt) {
        return opt.orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
    }

    @Override
    public Boolean getExistStoneId(Long id) {
        return stoneRepository.existsByStoneId(id);
    }

    @Override
    public Boolean getExistStoneName(String stoneTypeName, String stoneShapeName, String stoneSize) {
        String stoneName = stoneTypeName + "/" + stoneShapeName + "/" + stoneSize;
        return stoneRepository.existsByStoneName(stoneName);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getStonesExcel(String stoneName, String stoneShape, String stoneType) throws IOException {
        List<StoneExcelDto> stoneExcelDtos = stoneRepository.findStonesForExcel(stoneName, stoneShape, stoneType);
        return StoneExcelUtil.createStoneWorkSheet(stoneExcelDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsStoneId(Long stoneId) {
        return stoneRepository.existsByStoneId(stoneId);
    }
}
