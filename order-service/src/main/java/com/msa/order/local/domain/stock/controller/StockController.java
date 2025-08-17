package com.msa.order.local.domain.stock.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.stock.dto.StockDto;
import com.msa.order.local.domain.stock.service.StockService;
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

    @GetMapping("/stocks")
    public ResponseEntity<ApiResponse<CustomPage<StockDto.Response>>> getStocks(
            @RequestParam(name = "search") String inputSearch,
            @RequestParam(name = "startAt") String startAt,
            @RequestParam(name = "endAt") String endAt,
            @PageableDefault(size = 20) Pageable pageable) {

        StockDto.StockCondition condition = new StockDto.StockCondition(startAt, endAt);
        CustomPage<StockDto.Response> stocks = stockService.getStocks(inputSearch, condition, pageable);

        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @PostMapping("/stocks")
    public ResponseEntity<ApiResponse<String>> createStock(
            @AccessToken String accessToken,
            @RequestParam(name = "order_type") String orderType,
            @Valid @RequestBody StockDto.OrderStockRequest stockDto) {

        stockService.saveStock(accessToken, orderType, stockDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

}
