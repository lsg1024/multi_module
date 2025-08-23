package com.msa.order.local.domain.sale.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.order.local.domain.sale.service.SaleService;
import com.msa.order.local.domain.stock.dto.StockDto;
import com.msa.order.local.domain.stock.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaleController {

    private final SaleService saleService;
    private final StockService stockService;

    public SaleController(SaleService saleService, StockService stockService) {
        this.saleService = saleService;
        this.stockService = stockService;
    }

    //주문 -> 판매 type = SALE
    @PatchMapping("/sale_order")
    public ResponseEntity<ApiResponse<String>> updateOrderToSale(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @RequestParam(name = "order_type") String orderType,
            @Valid @RequestBody StockDto.orderStockRequest stockDto) {
        stockService.updateOrderStatus(accessToken, flowCode, orderType, stockDto);
        return ResponseEntity.ok(ApiResponse.success("등록 완료"));
    }


    //재고 -> 판매 type = SALE
    @PatchMapping("/sale_stock")
    public ResponseEntity<ApiResponse<String>> updateStockToSale(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @RequestParam(name = "order_type") String orderType,
            @Valid @RequestBody StockDto.orderStockRequest stockDto) {
        stockService.updateOrderStatus(accessToken, flowCode, orderType, stockDto);
        return ResponseEntity.ok(ApiResponse.success("등록 완료"));
    }

    //

}
