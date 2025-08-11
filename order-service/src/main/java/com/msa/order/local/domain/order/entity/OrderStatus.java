package com.msa.order.local.domain.order.entity;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum OrderStatus {
    ORDER("주문"), // 주문
    ORDER_AWAIT("주문 대기"),
    ORDER_AWAIT_FAILED("주문 실패"), // 주문 실패
    AWAIT("대기"), // 대기,
    INVENTORY("재고"), // 재고
    SHIPPING("출고"), // 출고
    READY_MADE("기성"), // 기성 대체
    RENT("대여"), // 대여
    FIX("수리"), // 수리
    CANCEL("취소"), // 취소 -> 제품 등록 후 어딘가에서 한번이라도 취소한 경우
    DELETE("삭제"); // 삭제

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static Optional<OrderStatus> fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(s -> s.getDisplayName().equals(displayName))
                .findFirst();
    }
}
