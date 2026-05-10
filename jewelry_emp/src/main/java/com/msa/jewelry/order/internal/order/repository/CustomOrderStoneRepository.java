package com.msa.jewelry.order.internal.order.repository;

import com.msa.jewelry.order.internal.sale.entity.dto.SaleDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomOrderStoneRepository {
    List<SaleDto.StoneCountDto> findStoneCountsByStockIds(List<Long> flowCodes);
}
