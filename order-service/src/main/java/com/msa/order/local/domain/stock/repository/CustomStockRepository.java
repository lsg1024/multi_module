package com.msa.order.local.domain.stock.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.stock.dto.StockDto;
import org.springframework.data.domain.Pageable;

public interface CustomStockRepository {
    // 재고
    CustomPage<StockDto.Response> findByStockProducts(String input, String orderType, StockDto.StockCondition condition, Pageable pageable);
}
