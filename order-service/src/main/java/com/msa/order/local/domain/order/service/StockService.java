package com.msa.order.local.domain.order.service;

import com.msa.order.local.domain.order.repository.CustomOrderRepository;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockService {

    private final OrdersRepository ordersRepository;
    private final CustomOrderRepository customOrderRepository;

    public StockService(OrdersRepository ordersRepository, CustomOrderRepository customOrderRepository) {
        this.ordersRepository = ordersRepository;
        this.customOrderRepository = customOrderRepository;
    }

    // 재고 관리 get 전체 재고 + 주문, 수리, 대여 관련
    @Transactional(readOnly = true)
    public void getStocks() {

    }

    // 재고 등록

    // 재고 -> 주문

    // 재고 -> 판매

    // 재고 -> 대여

    // 재고 -> 삭제
}
