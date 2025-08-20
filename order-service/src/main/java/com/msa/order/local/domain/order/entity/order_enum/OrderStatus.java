package com.msa.order.local.domain.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum OrderStatus {
    ORDER("주문"),
    FIX("수리"),
    STOCK("재고"),
    NORMAL("일반"),
    RENTAL("대여"),
    RETURN("반납"),
    SALE("판매"),
    DELETE("삭제"),
    NONE("NONE");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {return displayName; }

    public static Optional<OrderStatus> fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(s -> s.getDisplayName().equals(displayName))
                .findFirst();
    }
}

