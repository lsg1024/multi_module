package com.msa.order.local.stock.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.service.StockService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
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

        CustomPage<StockDto.Response> stocks = stockService.getStocks(input, startAt, endAt, factoryName,
                storeName, setTypeName, colorName, sortField, sort, orderStatus, pageable);

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
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<StockDto.Response> pastRentalHistory = stockService.getPastRentalHistory(input, startAt, endAt, factoryName,
                storeName, setTypeName, colorName, sortField, sort, pageable);

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


}
