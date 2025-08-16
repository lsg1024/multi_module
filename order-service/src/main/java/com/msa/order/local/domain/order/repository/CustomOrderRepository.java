package com.msa.order.local.domain.order.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.order.dto.OrderDto;
import org.springframework.data.domain.Pageable;

public interface CustomOrderRepository {
    // 주문 상세 조회 - productStone 포함
    OrderDto.ResponseDetail findByOrderId(Long orderId);

    // 주문 검색 = 제품 이름, 거래처, 공장, 접수일
    CustomPage<OrderDto.Response> findByOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable);

    // 주문 출고 검색 = 제품 이름, 거래처, 공장, 접수일 - 출고 예정
    CustomPage<OrderDto.Response> findByExpectOrders(OrderDto.InputCondition inputCondition, OrderDto.ExpectCondition orderCondition, Pageable pageable);
    // 주문 삭제 검색 = 제품 이름, 거래처, 공장, 접수일 - 삭제 예정
    CustomPage<OrderDto.Response> findByDeletedOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable);

}
