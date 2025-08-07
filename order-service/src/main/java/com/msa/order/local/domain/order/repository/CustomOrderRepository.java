package com.msa.order.local.domain.order.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.order.dto.OrderDto;
import org.springframework.data.domain.Pageable;

public interface CustomOrderRepository {
    // 주문 상세 조회 - productStone 포함
    OrderDto.ResponseDetail findByOrderId(Long orderId);

    // 검색 = 제품 이름, 거래처, 공장, 접수일
    CustomPage<OrderDto.Response> findByOrders(OrderDto.Condition condition, Pageable pageable);
}
