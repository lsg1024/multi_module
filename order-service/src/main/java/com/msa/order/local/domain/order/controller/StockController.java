package com.msa.order.local.domain.order.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.stock.dto.StockDto;
import com.msa.order.local.domain.stock.service.StockService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
