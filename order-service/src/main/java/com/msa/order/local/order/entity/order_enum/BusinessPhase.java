package com.msa.order.local.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum BusinessPhase {
    ORDER("주문"),
    WAITING("대기"),
    ORDER_FAIL("주문 실패"),
    ORDER_UPDATE_FAIL("주문 업데이트 실패"),
    STOCK("재고"),
    STOCK_FAIL("재고 실패"),
    FIX("수리"),
    NORMAL("일반"),
    RENTAL("대여"),
    RETURN("반환"),
    SALE("판매"),
    DELETE("삭제");

    private final String displayName;

    BusinessPhase(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static Optional<BusinessPhase> fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(s -> s.getDisplayName().equals(displayName))
                .findFirst();
    }
}
