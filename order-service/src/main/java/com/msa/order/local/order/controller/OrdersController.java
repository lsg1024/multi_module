package com.msa.order.local.order.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.order.dto.DateDto;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.dto.StoreDto;
import com.msa.order.local.order.service.OrdersService;
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

        log.info("createOrders = {}", orderDto.toString());

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
    @GetMapping("/orders") // URL 경로는 고정 -> 거래처, 상점 파라미터 추가 필요
    public ResponseEntity<ApiResponse<CustomPage<OrderDto.Response>>> getOrders(
            @AccessToken String accessToken,
            @RequestParam(name = "search", required = false) String input,
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "order_status") String orderStatus,
            @PageableDefault(size = 20) Pageable pageable) {

        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName);
        OrderDto.OrderCondition orderCondition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        CustomPage<OrderDto.Response> orderProducts = ordersService.getOrderProducts(accessToken, inputCondition, orderCondition, pageable);
        return ResponseEntity.ok(ApiResponse.success(orderProducts));
    }

    //주문 전체 수정
    @PatchMapping("/order")
    public ResponseEntity<ApiResponse<String>> updateOrder(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @RequestParam(name = "order_status") String orderStatus,
            @Valid @RequestBody OrderDto.Request orderDto) {
        ordersService.updateOrder(accessToken, flowCode, orderStatus, orderDto);

        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 주문 상태 호출
    @GetMapping("/orders/status")
    public ResponseEntity<ApiResponse<List<String>>> getStatus (
            @RequestParam String id) {
        List<String> orderStatusInfo = ordersService.getOrderStatusInfo(id);
        return ResponseEntity.ok(ApiResponse.success(orderStatusInfo));
    }

    // 주문 상태 변경
    @PatchMapping("/orders/status")
    public ResponseEntity<ApiResponse<String>> updateOrderStatus (
            @RequestParam String id,
            @RequestParam(name = "status") String orderStatus) {
        ordersService.updateOrderStatus(id, orderStatus);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 출고일 변경
    @PatchMapping("/orders/expect_date")
    public ResponseEntity<ApiResponse<String>> updateOrderExpectDate (
            @RequestParam String id,
            @RequestBody DateDto updateDate) {
        ordersService.updateOrderExpectDate(id, updateDate);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 주문 상점 변경
    @PatchMapping("/orders/store")
    public ResponseEntity<ApiResponse<String>> updateOrderStore (
            @AccessToken String accessToken,
            @RequestParam String id,
            @RequestBody StoreDto.Request updateStoreDto) {
        ordersService.updateOrderStore(accessToken, id, updateStoreDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @PatchMapping("/orders/factory")
    public ResponseEntity<ApiResponse<String>> updateOrderFactory (
            @AccessToken String accessToken,
            @RequestParam String id,
            @RequestBody FactoryDto.Request updateFactoryDto) {
        ordersService.updateOrderFactory(accessToken, id, updateFactoryDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 주문 삭제
    @DeleteMapping("/orders")
    public ResponseEntity<ApiResponse<String>> deletedOrder(
            @AccessToken String accessToken,
            @RequestParam String id) {
        ordersService.deletedOrder(accessToken, id);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    // 출고 예정 조회
    @GetMapping("/orders/deliveries")
    public ResponseEntity<ApiResponse<CustomPage<OrderDto.Response>>> getOrderExpect(
            @AccessToken String accessToken,
            @RequestParam(name = "search", required = false) String input,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "order_status") String orderStatus,
            @PageableDefault(size = 20) Pageable pageable) {

        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName);
        OrderDto.ExpectCondition expectCondition = new OrderDto.ExpectCondition(endAt, optionCondition, orderStatus);
        CustomPage<OrderDto.Response> expectProducts = ordersService.getDeliveryProducts(accessToken, inputCondition, expectCondition, pageable);

        return ResponseEntity.ok(ApiResponse.success(expectProducts));
    }

    @GetMapping("/orders/deleted")
    public ResponseEntity<ApiResponse<CustomPage<OrderDto.Response>>> getOrderDeleted(
            @AccessToken String accessToken,
            @RequestParam(name = "search", required = false) String input,
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "order_status") String orderStatus,
            @PageableDefault(size = 20) Pageable pageable) {

        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName);
        OrderDto.OrderCondition orderCondition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        CustomPage<OrderDto.Response> deletedProducts = ordersService.getDeletedProducts(accessToken, inputCondition, orderCondition, pageable);

        return ResponseEntity.ok(ApiResponse.success(deletedProducts));
    }

    // 공장 리스트 배열 // 페이징 처리하면 로딩된 페이징 값의 데이터로만 구성 가능하니 별도 호출이 필요
    @GetMapping("/filters/factory")
    public ResponseEntity<ApiResponse<List<String>>> getFactoryNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "order_status") String orderStatus) {

        List<String> filterFactories = ordersService.getFilterFactories(startAt, endAt, factoryName, storeName, setTypeName, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterFactories));
    }

    // 상점 리스트 배열
    @GetMapping("/filters/store")
    public ResponseEntity<ApiResponse<List<String>>> getStoreNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "order_status") String orderStatus) {

        List<String> filterStores = ordersService.getFilterStores(startAt, endAt, factoryName, storeName, setTypeName, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterStores));
    }

    // 유형 리스트 배열
    @GetMapping("/filters/set-type")
    public ResponseEntity<ApiResponse<List<String>>> getSetTypeNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "order_status") String orderStatus) {

        List<String> filterSetType = ordersService.getFilterSetType(startAt, endAt, factoryName, storeName, setTypeName, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterSetType));
    }

}
