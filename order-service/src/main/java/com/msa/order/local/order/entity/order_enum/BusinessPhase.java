package com.msa.order.local.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BusinessPhase {
    ORDER("주문"),
    WAITING("대기"),
    UPDATE("수정"),
    FAIL("실패"),
    STOCK("재고"),
    FIX("수리"),
    NORMAL("일반"),
    RENTAL("대여"),
    RETURN("반품"),
    SALE("판매"),
    DELETED("삭제");

    private final String displayName;

    BusinessPhase(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
