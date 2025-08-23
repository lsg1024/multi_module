package com.msa.order.local.domain.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum ProductStatus {
    RECEIPT("접수"), // 접수,
    WAITING("대기"), // 대기
    RECEIPT_FAILED("접수 실패"), // 접수 실패
    EXPECT("출고"),
    DELETE("삭제"),
    NONE("NONE");

    private final String displayName;

    ProductStatus(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static Optional<ProductStatus> fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(s -> s.getDisplayName().equals(displayName))
                .findFirst();
    }
}
