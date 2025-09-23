package com.msa.order.local.order.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.service.OrdersService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FixController {

    private final OrdersService ordersService;

    public FixController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    // 수리 생성 - Order 공유

    // 수리 상세 조회

    // 수리 목록
    @GetMapping("/fixes")
    public ResponseEntity<ApiResponse<CustomPage<OrderDto.Response>>> getFixes(
            @AccessToken String accessToken,
            @RequestParam(name = "search", required = false) String input,
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "order_status") String orderType,
            @PageableDefault(size = 20) Pageable pageable) {

        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName);
        OrderDto.OrderCondition fixCondition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderType);
        CustomPage<OrderDto.Response> fixProducts = ordersService.getFixProducts(accessToken, inputCondition, fixCondition, pageable);

        return ResponseEntity.ok(ApiResponse.success(fixProducts));
    }

    // 수리 수정

    // 수리 -> 재고

    // 수리 -> 주문

    // 수리 상태 변경 (재고, 주문, 삭제에서 다시 복원된 경우 표기 위함)


}
