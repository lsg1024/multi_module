package com.msa.order.local.domain.order.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.order.local.domain.order.dto.OrderDto;
import com.msa.order.local.domain.order.service.OrdersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<String>> createOrders(
            @AccessToken String accessToken,
            HttpServletRequest request,
            @Valid @RequestBody OrderDto.Request orderDto) {

        ordersService.saveOrder(accessToken, request, orderDto);

        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    //단일 값
//    @GetMapping("/orders/{id}")
//    public ResponseEntity<ApiResponse<OrderDto.Response>> getOrder(
//            @PathVariable Long id) {
//
//        ordersService.getOrder(id);
//    }

}
