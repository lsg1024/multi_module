package com.msa.order.local.order.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.global.excel.dto.OrderExcelQueryDto;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.dto.OrderQueryDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomOrderRepository {

    // 주문 검색 = 제품 이름, 거래처, 공장, 접수일
    CustomPage<OrderQueryDto> findByOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable);
    // 주문 수리 검색
    CustomPage<OrderQueryDto> findByFixOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable);

    // 주문 출고 검색 = 제품 이름, 거래처, 공장, 접수일 - 출고 예정
    CustomPage<OrderQueryDto> findByDeliveryOrders(OrderDto.InputCondition inputCondition, OrderDto.ExpectCondition orderCondition, Pageable pageable);
    // 주문 삭제 검색 = 제품 이름, 거래처, 공장, 접수일 - 삭제 예정
    CustomPage<OrderQueryDto> findByDeletedOrders(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable);
    // 주문 필더 목록
    List<String> findByFilterFactories(OrderDto.OrderCondition condition);
    List<String> findByFilterStores(OrderDto.OrderCondition condition);
    List<String> findByFilterSetType(OrderDto.OrderCondition condition);

    List<OrderExcelQueryDto> findByExcelData(OrderDto.OrderCondition condition);
}
