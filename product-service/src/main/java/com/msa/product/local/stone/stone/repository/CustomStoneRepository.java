package com.msa.product.local.stone.stone.repository;

import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomStoneRepository {
    Optional<Stone> findFetchJoinById(Long stoneId);
    CustomPage<StoneDto.PageDto> findByAllOrderByAsc(String stoneName, Pageable pageable);
}
