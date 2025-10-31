package com.msa.order.local.sale.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.sale.entity.dto.SaleDto;
import com.msa.order.local.sale.entity.dto.SaleRow;
import com.msa.order.local.sale.service.SaleService;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.service.StockService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SaleController {

    private final SaleService saleService;
    private final StockService stockService;

    public SaleController(SaleService saleService, StockService stockService) {
        this.saleService = saleService;
        this.stockService = stockService;
    }

    //판매 관리 데이터 목록들
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<CustomPage<SaleRow>>> getSales(
            @RequestParam(name = "search", required = false) String input,
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "type", required = false) String material,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<SaleRow> sale = saleService.getSale(input,startAt, endAt, material, pageable);
        return ResponseEntity.ok(ApiResponse.success(sale));
    }

    //주문 -> 판매
    @PatchMapping("/orders/order_sale")
    public ResponseEntity<ApiResponse<String>> updateOrderToSale(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @Valid @RequestBody StockDto.StockRegisterRequest stockDto) {
        stockService.updateOrderToStock(accessToken, flowCode, "STOCK", stockDto);
        saleService.orderToSale(accessToken, flowCode);
        return ResponseEntity.ok(ApiResponse.success("등록 완료"));
    }


    //재고 -> 판매
    @PatchMapping("/sales/stock_sale")
    public ResponseEntity<ApiResponse<String>> updateStockToSale(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode,
            @Valid @RequestBody StockDto.stockRequest stockDto) {
        saleService.stockToSale(accessToken, flowCode, stockDto);
        return ResponseEntity.ok(ApiResponse.success("등록 완료"));
    }

    //결제, DC, 결통, WG...
    @PostMapping("/sales")
    public ResponseEntity<ApiResponse<String>> createPayment(
            @AccessToken String accessToken,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempKey,
            @Valid @RequestBody SaleDto.Request saleDto) {

        if (idempKey == null || idempKey.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Idempotency-Key header required"));
        }

        saleService.createPayment(accessToken, idempKey, saleDto);
        return ResponseEntity.ok(ApiResponse.success("등록 완료"));
    }

    @DeleteMapping("/sales")
    public ResponseEntity<ApiResponse<String>> deletedSale(
            @AccessToken String accessToken,
            @RequestParam(name = "id") Long flowCode) {

        saleService.cancelSale(accessToken, flowCode);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    //판매 기록
    @GetMapping("/sale/past")
    public ResponseEntity<ApiResponse<List<SaleDto.SaleDetailDto>>> findLastSaleHistory(
            @RequestParam(name = "store") Long storeId,
            @RequestParam(name = "product") Long productId,
            @RequestParam(name = "material") String materialName) {
        List<SaleDto.SaleDetailDto> saleDetailDtos = saleService.findSaleProductNameAndMaterial(storeId, productId, materialName);
        return ResponseEntity.ok(ApiResponse.success(saleDetailDtos));
    }

}
