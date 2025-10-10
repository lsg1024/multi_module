package com.msa.order.local.stock.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.stock.dto.StockDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomStockRepository {
    // 재고
    CustomPage<StockDto.Response> findByStockProducts(OrderDto.InputCondition inputCondition, StockDto.StockCondition condition, Pageable pageable);
    List<String> findByFilterFactories(StockDto.StockCondition condition);
    List<String> findByFilterStores(StockDto.StockCondition condition);
    List<String> findByFilterSetType(StockDto.StockCondition condition);
    List<String> findByFilterColor(StockDto.StockCondition condition);
}
