package com.msa.jewelry.local.stone.repository;

import com.msa.jewelry.global.excel.dto.StoneExcelDto;
import com.msa.jewelry.local.stone.dto.StoneDto;
import com.msa.jewelry.local.stone.entity.Stone;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CustomStoneRepository {
    Optional<Stone> findFetchJoinById(Long stoneId);
    CustomPage<StoneDto.PageDto> findAllStones(String search, String searchField, String searchMin, String searchMax, String sortField, String sortOrder, Pageable pageable);
    List<StoneExcelDto> findStonesForExcel(String stoneName, String stoneShape, String stoneType);
}
