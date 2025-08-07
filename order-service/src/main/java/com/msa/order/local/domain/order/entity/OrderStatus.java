package com.msa.order.local.domain.order.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    ORDER("주문"), // 주문
    INVENTORY("재고"), // 재고
    SHIPPING("출고"), // 출고
    READY_MADE("기성"), // 기성 대체
    RENT("대여"), // 대여
    FIX("수리"), // 수리
    CANCEL("취소"), // 취소
    DELETE("삭제"); // 삭제 -> 제품 등록 후 어딘가에서 한번이라도 취소한 경우

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
