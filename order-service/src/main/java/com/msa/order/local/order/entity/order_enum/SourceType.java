package com.msa.order.local.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SourceType {
    ORDER("주문"),
    FIX("수리"),
    NORMAL("일반"),
    SALE("판매"),
    RENTAL("대여");

    private final String displayName;

    SourceType(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
