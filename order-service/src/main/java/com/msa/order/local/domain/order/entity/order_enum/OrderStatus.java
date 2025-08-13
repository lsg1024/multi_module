package com.msa.order.local.domain.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum OrderStatus {
    RECEIPT("접수"), // 접수
    WAITING("대기"), // 대기
    RECEIPT_FAILED("접수 실패"), // 접수 실패
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

