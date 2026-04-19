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

/**
 * 주문 관리 REST 컨트롤러.
 *
 * *주문 생성·단건 조회·목록 조회·수정·삭제 및 상태 변경 API를 제공한다.
 *
 * *제공 엔드포인트 요약:
 *
 *   - {@code POST /orders} — 주문 생성
 *   - {@code GET /order} — 주문 단건 상세 조회
 *   - {@code GET /orders} — 주문 목록 페이징 조회 (다중 필터·정렬 지원)
 *   - {@code PATCH /order} — 주문 전체 수정
 *   - {@code PATCH /orders/status} — 주문 상태 변경
 *   - {@code PATCH /orders/delivery-date} — 출고일 변경
 *   - {@code PATCH /orders/store} — 주문 상점 변경
 *   - {@code PATCH /orders/factory} — 주문 공장 변경
 *   - {@code DELETE /orders/delete} — 주문 삭제
 *   - {@code GET /orders/deliveries} — 출고 예정 목록 조회
 *   - {@code GET /orders/deleted} — 삭제된 주문 목록 조회
 *   - {@code GET /filters/*} — 공장·상점·세트유형·컬러·분류·재질 필터 목록 조회
 * 
 *
 * *의존성: {@link com.msa.order.local.order.service.OrdersService}
 */
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
            @RequestParam(name = "searchField", required = false) String searchField,
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "classification", required = false) String classificationName,
            @RequestParam(name = "material", required = false) String materialName,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sort,
            @RequestParam(name = "order_status", required = false) String orderStatus,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<OrderDto.Response> orderProducts = ordersService.getOrderProducts(accessToken, input, searchField, startAt, endAt, factoryName,
                storeName, setTypeName, colorName, classificationName, materialName, sortField, sort, orderStatus, pageable);
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
            @AccessToken String accessToken,
            @RequestParam String id,
            @RequestParam(name = "status") String productStatus) {
        ordersService.updateOrderStatus(accessToken, id, productStatus);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 출고일 변경
    @PatchMapping("/orders/delivery-date")
    public ResponseEntity<ApiResponse<String>> updateOrderDeliveryDate(
            @AccessToken String accessToken,
            @RequestParam String id,
            @Valid @RequestBody DateDto updateDate) {
        ordersService.updateOrderDeliveryDate(accessToken, id, updateDate);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 주문 상점 변경
    @PatchMapping("/orders/store")
    public ResponseEntity<ApiResponse<String>> updateOrderStore (
            @AccessToken String accessToken,
            @RequestParam String id,
            @Valid @RequestBody StoreDto.Request updateStoreDto) {
        ordersService.updateOrderStore(accessToken, id, updateStoreDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @PatchMapping("/orders/factory")
    public ResponseEntity<ApiResponse<String>> updateOrderFactory (
            @AccessToken String accessToken,
            @RequestParam String id,
            @Valid @RequestBody FactoryDto.Request updateFactoryDto) {
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
            @RequestParam(name = "searchField", required = false) String searchField,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "classification", required = false) String classificationName,
            @RequestParam(name = "material", required = false) String materialName,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<OrderDto.Response> expectProducts = ordersService.getDeliveryProducts(accessToken,
                input, searchField, endAt, factoryName, storeName, setTypeName,
                colorName, classificationName, materialName, sortField, sort, pageable);

        return ResponseEntity.ok(ApiResponse.success(expectProducts));
    }

    @GetMapping("/orders/deleted")
    public ResponseEntity<ApiResponse<CustomPage<OrderDto.Response>>> getOrderDeleted(
            @AccessToken String accessToken,
            @RequestParam(name = "search", required = false) String input,
            @RequestParam(name = "searchField", required = false) String searchField,
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "classification", required = false) String classificationName,
            @RequestParam(name = "material", required = false) String materialName,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sort,
            @RequestParam(name = "order_status") String orderStatus,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<OrderDto.Response> deletedProducts = ordersService.getDeletedProducts(accessToken, input, searchField, startAt, endAt, factoryName,
                storeName, setTypeName, colorName, classificationName, materialName, sortField, sort, orderStatus, pageable);

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
            @RequestParam(name = "classification", required = false) String classificationName,
            @RequestParam(name = "material", required = false) String materialName,
            @RequestParam(name = "order_status") String orderStatus) {

        List<String> filterFactories = ordersService.getFilterFactories(startAt, endAt, factoryName,
                storeName, setTypeName, colorName, classificationName, materialName, orderStatus);
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
            @RequestParam(name = "classification", required = false) String classificationName,
            @RequestParam(name = "material", required = false) String materialName,
            @RequestParam(name = "order_status") String orderStatus) {

        List<String> filterStores = ordersService.getFilterStores(startAt, endAt, factoryName, storeName, setTypeName, colorName, classificationName, materialName, orderStatus);
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
            @RequestParam(name = "classification", required = false) String classificationName,
            @RequestParam(name = "material", required = false) String materialName,
            @RequestParam(name = "order_status") String orderStatus) {

        List<String> filterSetType = ordersService.getFilterSetType(startAt, endAt, factoryName, storeName, setTypeName, colorName, classificationName, materialName, orderStatus);
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
            @RequestParam(name = "classification", required = false) String classificationName,
            @RequestParam(name = "material", required = false) String materialName,
            @RequestParam(name = "order_status") String orderStatus) {
        List<String> filterColors = ordersService.getFilterColors(startAt, endAt, factoryName, storeName, setTypeName, colorName, classificationName, materialName, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterColors));
    }

    // 분류 리스트 배열
    @GetMapping("/filters/classification")
    public ResponseEntity<ApiResponse<List<String>>> getClassificationNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "classification", required = false) String classificationName,
            @RequestParam(name = "material", required = false) String materialName,
            @RequestParam(name = "order_status") String orderStatus) {
        List<String> filterClassifications = ordersService.getFilterClassifications(startAt, endAt, factoryName, storeName, setTypeName, colorName, classificationName, materialName, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterClassifications));
    }

    // 재질 리스트 배열
    @GetMapping("/filters/material")
    public ResponseEntity<ApiResponse<List<String>>> getMaterialNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "classification", required = false) String classificationName,
            @RequestParam(name = "material", required = false) String materialName,
            @RequestParam(name = "order_status") String orderStatus) {
        List<String> filterMaterials = ordersService.getFilterMaterials(startAt, endAt, factoryName, storeName, setTypeName, colorName, classificationName, materialName, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterMaterials));
    }

}
