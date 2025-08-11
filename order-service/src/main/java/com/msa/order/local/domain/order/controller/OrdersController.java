package com.msa.order.local.domain.order.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.order.dto.FactoryDto;
import com.msa.order.local.domain.order.dto.OrderDto;
import com.msa.order.local.domain.order.dto.StoreDto;
import com.msa.order.local.domain.order.service.OrdersService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<String>> createOrders(
            @AccessToken String accessToken,
            @Valid @RequestBody OrderDto.Request orderDto) {

        ordersService.saveOrder(accessToken, orderDto);

        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    //단일 값 -> 상세 조회
    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDto.ResponseDetail>> getOrder(
            @PathVariable Long id) {

        OrderDto.ResponseDetail orderDetail = ordersService.getOrder(id);
        return ResponseEntity.ok(ApiResponse.success(orderDetail));
    }

    //복수 값
    @GetMapping("/orders") // URL 경로는 고정
    public ResponseEntity<ApiResponse<CustomPage<OrderDto.Response>>> getOrders(
            @RequestParam(required = false) String input,
            @RequestParam(required = false) String startAt,
            @RequestParam(required = false) String endAt,
            @PageableDefault(size = 16) Pageable pageable) {

        OrderDto.Condition condition = new OrderDto.Condition(input, startAt, endAt);
        CustomPage<OrderDto.Response> orderProducts = ordersService.getOrderProducts(condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(orderProducts));
    }

    // 주문 상태 호출
    @GetMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<List<String>>> getStatus (
            @PathVariable Long id) {
        List<String> orderStatusInfo = ordersService.getOrderStatusInfo(id);
        return ResponseEntity.ok(ApiResponse.success(orderStatusInfo));
    }

    // 주문 상태 변경
    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateOrderStatus (
            @PathVariable Long id,
            @RequestParam(required = false) String orderStatus) {
        ordersService.updateOrderStatus(id, orderStatus);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 주문 상점 변경
    @PatchMapping("/order/{id}/store")
    public ResponseEntity<ApiResponse<String>> updateOrderStore (
            @AccessToken String accessToken,
            @PathVariable Long id,
            @RequestBody StoreDto.Request updateStoreDto) {
        ordersService.updateOrderStore(accessToken, id, updateStoreDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @PatchMapping("/order/{id}/factory")
    public ResponseEntity<ApiResponse<String>> updateOrderStore (
            @AccessToken String accessToken,
            @PathVariable Long id,
            @RequestBody FactoryDto.Request updateFactoryDto) {
        ordersService.updateOrderFactory(accessToken, id, updateFactoryDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }



}
