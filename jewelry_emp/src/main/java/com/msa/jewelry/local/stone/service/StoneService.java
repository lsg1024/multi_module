package com.msa.jewelry.local.stone.service;

import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.local.stone.dto.StoneDto;
import org.springframework.data.domain.Pageable;

import java.io.IOException;

public interface StoneService {

    void saveStone(StoneDto stoneDto);

    StoneDto.ResponseSingle getStone(Long stoneId);

    CustomPage<StoneDto.PageDto> getStones(String search, String searchField, String searchMin, String searchMax, String sortField, String sortOrder, Pageable pageable);

    void updateStone(Long stoneId, StoneDto stoneDto);

    void deletedStone(String accessToken, Long stoneId);

    Boolean getExistStoneId(Long id);

    Boolean getExistStoneName(String stoneTypeName, String stoneShapeName, String stoneSize);

    byte[] getStonesExcel(String stoneName, String stoneShape, String stoneType) throws IOException;

    boolean existsStoneId(Long stoneId);
}
