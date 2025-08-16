package com.msa.order.local.domain.stock.service;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.stock.dto.StockDto;
import com.msa.order.local.domain.stock.repository.CustomStockRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockService {

    private final CustomStockRepository customStockRepository;

    public StockService(CustomStockRepository customStockRepository) {
        this.customStockRepository = customStockRepository;
    }

    // 재고 관리 get 전체 재고 + 주문, 수리, 대여 관련
    @Transactional(readOnly = true)
    public CustomPage<StockDto.Response> getStocks(String inputSearch, StockDto.StockCondition condition, Pageable pageable) {
        return customStockRepository.findByStockProducts(inputSearch, condition, pageable);
    }

    // 재고 등록

    // 재고 -> 주문

    // 재고 -> 판매

    // 재고 -> 대여

    // 재고 -> 삭제
}
