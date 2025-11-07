package com.msa.order.local.order.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.order.dto.DateDto;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.dto.StoreDto;
import com.msa.order.local.order.service.OrdersService;
import com.msa.order.local.stock.dto.StockDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

        Long flowCode = ordersService.saveOrder(accessToken, orderType, orderDto);

        return ResponseEntity.ok(ApiResponse.success("생성 완료", String.valueOf(flowCode)));
    }

    //단일 값 -> 상세 조회
    @GetMapping("/order")
    public ResponseEntity<ApiResponse<OrderDto.ResponseDetail>> getOrder(
            @RequestParam Long id) {

        OrderDto.ResponseDetail orderDetail = ordersService.getOrder(id);
        return ResponseEntity.ok(ApiResponse.success(orderDetail));
    }

    @GetMapping("/orders/stock-register")
    public ResponseEntity<ApiResponse<List<StockDto.ResponseDetail>>> getOrderRegisterStock(
            @RequestParam(name = "ids") List<Long> flowCodes) {
        List<StockDto.ResponseDetail> orderRegisterStock = ordersService.getOrderRegisterStock(flowCodes);

        return ResponseEntity.ok(ApiResponse.success(orderRegisterStock));
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
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sort,
            @RequestParam(name = "order_status", required = false) String orderStatus,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<OrderDto.Response> orderProducts = ordersService.getOrderProducts(accessToken, input, startAt, endAt, factoryName,
                storeName, setTypeName, colorName, sortField, sort, orderStatus, pageable);
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
    public ResponseEntity<ApiResponse<String>> updateProductStatus(
            @RequestParam String id,
            @RequestParam(name = "status") String productStatus) {
        ordersService.updateOrderStatus(id, productStatus);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 출고일 변경
    @PatchMapping("/orders/delivery-date")
    public ResponseEntity<ApiResponse<String>> updateOrderDeliveryDate(
            @RequestParam String id,
            @RequestBody DateDto updateDate) {
        ordersService.updateOrderDeliveryDate(id, updateDate);
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
    @DeleteMapping("/orders/delete")
    public ResponseEntity<ApiResponse<String>> deletedOrder(
            @AccessToken String accessToken,
            @RequestParam(name = "id") String flowCode) {
        ordersService.deletedOrders(accessToken, flowCode);
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
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<OrderDto.Response> expectProducts = ordersService.getDeliveryProducts(accessToken,
                input, endAt, factoryName, storeName, setTypeName,
                colorName, sortField, sort, pageable);

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
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sort,
            @RequestParam(name = "order_status") String orderStatus,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<OrderDto.Response> deletedProducts = ordersService.getDeletedProducts(accessToken, input, startAt, endAt, factoryName,
                storeName, setTypeName, colorName, sortField, sort, orderStatus, pageable);

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
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "order_status") String orderStatus) {

        List<String> filterFactories = ordersService.getFilterFactories(startAt, endAt, factoryName,
                storeName, setTypeName, colorName, orderStatus);
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
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "order_status") String orderStatus) {

        List<String> filterStores = ordersService.getFilterStores(startAt, endAt, factoryName, storeName, setTypeName, colorName, orderStatus);
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
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "order_status") String orderStatus) {

        List<String> filterSetType = ordersService.getFilterSetType(startAt, endAt, factoryName, storeName, setTypeName, colorName, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterSetType));
    }

    @GetMapping("/filters/color")
    public ResponseEntity<ApiResponse<List<String>>> getColorNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "order_status") String orderStatus) {
        List<String> filterColors = ordersService.getFilterColors(startAt, endAt, factoryName, storeName, setTypeName, colorName, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterColors));
    }

}
