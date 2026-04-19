package com.msa.order.local.stock.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.stock.dto.InventoryDto;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.service.StockService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 재고 관리 REST 컨트롤러.
 *
 * *재고 생성·조회·수정·삭제, 주문→재고 등록, 재고→대여, 대여→반납, 반납→재고 복구,
 * 재고 조사 기능 API를 제공한다.
 *
 * *제공 엔드포인트 요약:
 *
 *   - {@code GET /stock} — 재고 단건 상세 조회
 *   - {@code GET /stocks} — 재고 목록 페이징 조회 (다중 필터·정렬 지원)
 *   - {@code POST /stocks} — 재고 직접 생성
 *   - {@code PATCH /orders/stock-register} — 주문 → 재고 등록
 *   - {@code PATCH /stock} — 재고 수정
 *   - {@code GET /stocks/rental/history} — 대여 이력 조회
 *   - {@code PATCH /stocks/rental} — 재고 → 대여 상태 변경
 *   - {@code PATCH /stocks/rental/return} — 대여 → 반납 상태 변경
 *   - {@code PATCH /stocks/rollback} — 반납·삭제 → 재고 복구
 *   - {@code DELETE /stocks/delete} — 재고 삭제
 *   - {@code GET /stocks/inventory} — 재고 조사 목록 조회
 *   - {@code POST /stocks/inventory/prepare} — 재고 조사 초기화
 *   - {@code POST /stocks/inventory/check} — 재고 항목 조사 확인
 *   - {@code GET /stocks/inventory/statistics} — 재고 조사 통계 조회
 *   - {@code GET /stocks/filters/*} — 공장·상점·세트유형·컬러·분류·재질 필터 목록 조회
 * 
 *
 * *의존성: {@link com.msa.order.local.stock.service.StockService}
 */
@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 상품명 기반 재고 수량 일괄 조회 (카탈로그용)
     * 상품명 목록을 받아 각 상품의 활성 재고(STOCK, NORMAL, RENTAL) 수량을 반환한다.
     */
    @PostMapping("/stocks/count-by-names")
    public ResponseEntity<ApiResponse<java.util.Map<String, Integer>>> getStockCountByProductNames(
            @RequestBody List<String> productNames) {

        java.util.Map<String, Integer> counts = stockService.getStockCountByProductNames(productNames);
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    @GetMapping("/stock")
    public ResponseEntity<ApiResponse<List<StockDto.ResponseDetail>>> getStockDetail(
            @RequestParam(name = "ids") List<Long> flowCodes) {

        List<StockDto.ResponseDetail> detailStock = stockService.getDetailStock(flowCodes);
        return ResponseEntity.ok(ApiResponse.success(detailStock));
    }

    @GetMapping("/stocks")
    public ResponseEntity<ApiResponse<CustomPage<StockDto.Response>>> getStocks(
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

        CustomPage<StockDto.Response> stocks = stockService.getStocks(input, searchField, startAt, endAt, factoryName,
                storeName, setTypeName, colorName, classificationName, materialName, sortField, sort, orderStatus, pageable);

        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    // 주문 -> 재고
    @PatchMapping("/orders/stock-register")
    public ResponseEntity<ApiResponse<String>> updateOrderToStock(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @RequestParam(name = "order_status") String orderStatus,
            @Valid @RequestBody StockDto.StockRegisterRequest stockDto) {

        stockService.updateOrderToStock(accessToken, flowCode, orderStatus, stockDto);

        return ResponseEntity.ok(ApiResponse.success("재고 등록 완료"));
    }

    @PostMapping("/stocks")
    public ResponseEntity<ApiResponse<String>> createStock(
            @AccessToken String accessToken,
            @RequestParam(name = "order_type") String orderType,
            @Valid @RequestBody StockDto.Request stockDto) {

        stockService.saveStock(accessToken, orderType, stockDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    // 재고 수정 기능 필요
    @PatchMapping("/stock")
    public ResponseEntity<ApiResponse<String>> updateStock(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @Valid @RequestBody StockDto.updateStockRequest updateDto) {

        stockService.updateStock(accessToken, flowCode, updateDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @GetMapping("/stocks/rental/history")
    public ResponseEntity<ApiResponse<CustomPage<StockDto.Response>>> getRentalHistory(
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
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<StockDto.Response> pastRentalHistory = stockService.getPastRentalHistory(input, searchField, startAt, endAt, factoryName,
                storeName, setTypeName, colorName, classificationName, materialName, sortField, sort, pageable);

        return ResponseEntity.ok(ApiResponse.success(pastRentalHistory));
    }

    //재고 -> 대여
    @PatchMapping("/stocks/rental")
    public ResponseEntity<ApiResponse<String>> updateStockToRental(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @Valid @RequestBody StockDto.StockRentalRequest stockDto) {
        stockService.stockToRental(accessToken, flowCode, stockDto);
        return ResponseEntity.ok(ApiResponse.success("변경 완료"));
    }

    //대여 -> 반납
    @PatchMapping("/stocks/rental/return")
    public ResponseEntity<ApiResponse<String>> updateRentalToReturn(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @RequestParam(name = "order_type") String orderType) {

        stockService.rentalToReturn(accessToken, flowCode, orderType);
        return ResponseEntity.ok(ApiResponse.success("반납 완료"));
    }

    //반납 && 삭제 -> 재고
    @PatchMapping("/stocks/rollback")
    public ResponseEntity<ApiResponse<String>> updateReturnToStock(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @RequestParam(name = "order_type") String orderType) {

        stockService.rollBackStock(accessToken, flowCode, orderType);
        return ResponseEntity.ok(ApiResponse.success("복구 완료"));
    }

    // 재고 -> 삭제
    @DeleteMapping("/stocks/delete")
    public ResponseEntity<ApiResponse<String>> deleteStock(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode) {

        stockService.stockToDelete(accessToken, flowCode);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    // 공장 리스트 배열
    @GetMapping("/stocks/filters/factory")
    public ResponseEntity<ApiResponse<List<String>>> getFactoryNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "order_status", required = false) String orderStatus) {

        List<String> filterFactories = stockService.getFilterFactories(startAt, endAt, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterFactories));
    }

    // 상점 리스트 배열
    @GetMapping("/stocks/filters/store")
    public ResponseEntity<ApiResponse<List<String>>> getStoreNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "order_status", required = false) String orderStatus) {

        List<String> filterStores = stockService.getFilterStores(startAt, endAt, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterStores));
    }

    // 유형 리스트 배열
    @GetMapping("/stocks/filters/set-type")
    public ResponseEntity<ApiResponse<List<String>>> getSetTypeNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "order_status", required = false) String orderStatus) {

        List<String> filterSetType = stockService.getFilterSetType(startAt, endAt, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterSetType));
    }

    @GetMapping("/stocks/filters/color")
    public ResponseEntity<ApiResponse<List<String>>> getColorNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "order_status", required = false) String orderStatus) {
        List<String> filterColors = stockService.getFilterColors(startAt, endAt, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterColors));
    }

    // 분류 리스트 배열
    @GetMapping("/stocks/filters/classification")
    public ResponseEntity<ApiResponse<List<String>>> getClassificationNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "order_status", required = false) String orderStatus) {
        List<String> filterClassifications = stockService.getFilterClassifications(startAt, endAt, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterClassifications));
    }

    // 재질 리스트 배열
    @GetMapping("/stocks/filters/material")
    public ResponseEntity<ApiResponse<List<String>>> getMaterialNames(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "order_status", required = false) String orderStatus) {
        List<String> filterMaterials = stockService.getFilterMaterials(startAt, endAt, orderStatus);
        return ResponseEntity.ok(ApiResponse.success(filterMaterials));
    }

    @GetMapping("/stocks/inventory")
    public ResponseEntity<ApiResponse<CustomPage<InventoryDto.Response>>> getInventoryStocks(
            @RequestParam(name = "searchField", required = false) String searchField,
            @RequestParam(name = "searchValue", required = false) String searchValue,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sortOrder,
            @RequestParam(name = "stockChecked", required = false) String stockChecked,
            @RequestParam(name = "orderStatus", required = false) String orderStatus,
            @RequestParam(name = "materialName", required = false) String materialName,
            @PageableDefault(size = 40) Pageable pageable) {

        CustomPage<InventoryDto.Response> inventoryStocks = stockService.getInventoryStocks(
                searchField, searchValue, sortField, sortOrder, stockChecked, orderStatus, materialName, pageable);

        return ResponseEntity.ok(ApiResponse.success(inventoryStocks));
    }

    @GetMapping("/stocks/inventory/filters/material")
    public ResponseEntity<ApiResponse<List<String>>> getInventoryMaterials() {
        List<String> materials = stockService.getInventoryMaterials();
        return ResponseEntity.ok(ApiResponse.success(materials));
    }

    @PostMapping("/stocks/inventory/prepare")
    public ResponseEntity<ApiResponse<InventoryDto.ResetResponse>> prepareInventoryCheck(
            @AccessToken String accessToken) {

        InventoryDto.ResetResponse response = stockService.prepareInventoryCheck();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/stocks/inventory/check")
    public ResponseEntity<ApiResponse<InventoryDto.CheckResponse>> checkStock(
            @AccessToken String accessToken,
            @RequestParam(name = "flowCode") Long flowCode) {

        InventoryDto.CheckResponse response = stockService.checkStock(flowCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stocks/inventory/statistics")
    public ResponseEntity<ApiResponse<InventoryDto.StatisticsResponse>> getInventoryStatistics() {
        InventoryDto.StatisticsResponse response = stockService.getInventoryStatistics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
