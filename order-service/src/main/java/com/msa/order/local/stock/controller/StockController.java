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

@RestController
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // 주문 -> 재고 조회
    @GetMapping("/orders/stock-register")
    public ResponseEntity<ApiResponse<StockDto.stockResponse>> getOrderStock() {
//        stockService
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 주문 -> 재고
    @PatchMapping("/orders/stock-register")
    public ResponseEntity<ApiResponse<String>> updateOrderToStock(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @RequestParam(name = "order_status") String orderStatus,
            @Valid @RequestBody StockDto.stockRequest stockDto) {
        stockService.updateOrderStatus(accessToken, flowCode, orderStatus, stockDto);
        return ResponseEntity.ok(ApiResponse.success("재고 등록 완료"));
    }

    @PostMapping("/stocks")
    public ResponseEntity<ApiResponse<String>> createStock(
            @AccessToken String accessToken,
            @RequestParam(name = "order_type") String orderType,
            @Valid @RequestBody StockDto.createStockRequest stockDto) {

        stockService.saveStock(accessToken, orderType, stockDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @GetMapping("/stock")
    public ResponseEntity<ApiResponse<StockDto.ResponseDetail>> getStockDetail(
            @RequestParam(name = "id") Long flowCode) {

        StockDto.ResponseDetail detailStock = stockService.getDetailStock(flowCode);
        return ResponseEntity.ok(ApiResponse.success(detailStock));
    }

    @GetMapping("/stocks")
    public ResponseEntity<ApiResponse<CustomPage<StockDto.Response>>> getStocks(
            @RequestParam(name = "search") String inputSearch,
            @RequestParam(name = "startAt") String startAt,
            @RequestParam(name = "endAt") String endAt,
            @RequestParam(name = "type", required = false) String orderType,
            @PageableDefault(size = 20) Pageable pageable) {

        StockDto.StockCondition condition = new StockDto.StockCondition(startAt, endAt);
        CustomPage<StockDto.Response> stocks = stockService.getStocks(inputSearch, orderType, condition, pageable);

        return ResponseEntity.ok(ApiResponse.success(stocks));
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
    public ResponseEntity<ApiResponse<String>> updateRentalToStock(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @RequestParam(name = "order_type") String orderType) {

        stockService.recoveryStock(accessToken, flowCode, orderType);
        return ResponseEntity.ok(ApiResponse.success("반납 완료"));
    }

    // 재고 -> 삭제
    @DeleteMapping("/stocks/delete")
    public ResponseEntity<ApiResponse<String>> deleteStock(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode) {
        stockService.stockToDelete(accessToken, flowCode);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    //삭제 -> 재고
    @PatchMapping("/stocks/delete/recovery")
    public ResponseEntity<ApiResponse<String>> updateDeleteToStock(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @RequestParam(name = "order_type") String orderType) {
        stockService.recoveryStock(accessToken, flowCode, orderType);
        return ResponseEntity.ok(ApiResponse.success("복구 완료"));
    }

}
