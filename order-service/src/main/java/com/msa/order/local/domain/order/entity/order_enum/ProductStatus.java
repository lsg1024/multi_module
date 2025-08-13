package com.msa.order.local.domain.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum ProductStatus {
    ORDER("주문"),
    FAILED("실패"),
    STOCK("재고"),
    SALE("판매"),
    RENT("대여"),
    FIX("수리"),
    CANCEL("취소"),    // 운영상 필요한 경우 유지 권장
    DELETE("삭제");

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
