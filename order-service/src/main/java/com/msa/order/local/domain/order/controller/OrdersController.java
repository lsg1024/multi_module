package com.msa.order.local.domain.order.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.order.dto.DateDto;
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
            @RequestParam(name = "order_type") String orderType,
            @Valid @RequestBody OrderDto.Request orderDto) {

        ordersService.saveOrder(accessToken, orderType, orderDto);

        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    //단일 값 -> 상세 조회
    @GetMapping("/order")
    public ResponseEntity<ApiResponse<OrderDto.ResponseDetail>> getOrder(
            @RequestParam Long id) {

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

        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OrderCondition orderCondition = new OrderDto.OrderCondition(startAt, endAt);
        CustomPage<OrderDto.Response> orderProducts = ordersService.getOrderProducts(inputCondition, orderCondition, pageable);
        return ResponseEntity.ok(ApiResponse.success(orderProducts));
    }

    // 주문 상태 호출
    @GetMapping("/orders/status")
    public ResponseEntity<ApiResponse<List<String>>> getStatus (
            @RequestParam Long id) {
        List<String> orderStatusInfo = ordersService.getOrderStatusInfo(id);
        return ResponseEntity.ok(ApiResponse.success(orderStatusInfo));
    }

    // 주문 상태 변경
    @PatchMapping("/orders/status")
    public ResponseEntity<ApiResponse<String>> updateOrderStatus (
            @RequestParam Long id,
            @RequestParam(name = "status", required = false) String orderStatus) {
        ordersService.updateOrderStatus(id, orderStatus);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 출고일 변경
    @PatchMapping("/orders/expect_date")
    public ResponseEntity<ApiResponse<String>> updateOrderExpectDate (
            @RequestParam Long id,
            @RequestBody DateDto updateDate) {
        ordersService.updateOrderExpectDate(id, updateDate);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 주문 상점 변경
    @PatchMapping("/orders/store")
    public ResponseEntity<ApiResponse<String>> updateOrderStore (
            @AccessToken String accessToken,
            @RequestParam Long id,
            @RequestBody StoreDto.Request updateStoreDto) {
        ordersService.updateOrderStore(accessToken, id, updateStoreDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @PatchMapping("/orders/factory")
    public ResponseEntity<ApiResponse<String>> updateOrderFactory (
            @AccessToken String accessToken,
            @RequestParam Long id,
            @RequestBody FactoryDto.Request updateFactoryDto) {
        ordersService.updateOrderFactory(accessToken, id, updateFactoryDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 주문 삭제
    @DeleteMapping("/orders")
    public ResponseEntity<ApiResponse<String>> deletedOrder(
            @AccessToken String accessToken,
            @RequestParam Long id) {
        ordersService.deletedOrder(accessToken, id);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    // 출고 예정 조회
    @GetMapping("/orders/expect")
    public ResponseEntity<ApiResponse<CustomPage<OrderDto.Response>>> getOrderExpect(
            @RequestParam(required = false) String input,
            @RequestParam(required = false) String endAt,
            @PageableDefault(size = 16) Pageable pageable) {

        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.ExpectCondition expectCondition = new OrderDto.ExpectCondition(endAt);
        CustomPage<OrderDto.Response> expectProducts = ordersService.getExpectProducts(inputCondition, expectCondition, pageable);

        return ResponseEntity.ok(ApiResponse.success(expectProducts));
    }

    @GetMapping("/orders/delete")
    public ResponseEntity<ApiResponse<CustomPage<OrderDto.Response>>> getOrderDeleted(
            @RequestParam(required = false) String input,
            @RequestParam(required = false) String startAt,
            @RequestParam(required = false) String endAt,
            @PageableDefault(size = 16) Pageable pageable) {

        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OrderCondition orderCondition = new OrderDto.OrderCondition(startAt, endAt);
        CustomPage<OrderDto.Response> deletedProducts = ordersService.getDeletedProducts(inputCondition, orderCondition, pageable);

        return ResponseEntity.ok(ApiResponse.success(deletedProducts));
    }

}
