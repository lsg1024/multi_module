package com.msa.order.local.domain.sale.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.order.local.domain.order.repository.CustomOrderRepository;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import com.msa.order.local.domain.order.repository.StatusHistoryRepository;
import com.msa.order.local.domain.stock.repository.CustomStockRepository;
import com.msa.order.local.domain.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SaleService {

    private final JwtUtil jwtUtil;
    private final OrdersRepository ordersRepository;
    private final StockRepository stockRepository;
    private final CustomOrderRepository customOrderRepository;
    private final CustomStockRepository customStockRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public SaleService(JwtUtil jwtUtil, OrdersRepository ordersRepository, StockRepository stockRepository, CustomOrderRepository customOrderRepository, CustomStockRepository customStockRepository, StatusHistoryRepository statusHistoryRepository) {
        this.jwtUtil = jwtUtil;
        this.ordersRepository = ordersRepository;
        this.stockRepository = stockRepository;
        this.customOrderRepository = customOrderRepository;
        this.customStockRepository = customStockRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    // 판매 등록 ->

}
