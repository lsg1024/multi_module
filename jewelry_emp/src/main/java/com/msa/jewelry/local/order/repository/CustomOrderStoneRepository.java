package com.msa.jewelry.local.order.repository;

import com.msa.jewelry.local.sale.dto.SaleDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomOrderStoneRepository {
    List<SaleDto.StoneCountDto> findStoneCountsByStockIds(List<Long> flowCodes);
}
