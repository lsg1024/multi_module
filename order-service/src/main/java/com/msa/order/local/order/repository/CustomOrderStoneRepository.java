package com.msa.order.local.order.repository;

import com.msa.order.local.sale.entity.dto.SaleDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomOrderStoneRepository {
    List<SaleDto.StoneCountDto> findStoneCountsByStockIds(List<Long> flowCodes);
}
