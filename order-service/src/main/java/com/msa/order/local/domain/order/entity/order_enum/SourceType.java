package com.msa.order.local.domain.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum SourceType {
    ORDER("주문"),
    FIX("수리"),
    NORMAL("일반");

    private final String displayName;

    SourceType(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static Optional<SourceType> fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(s -> s.getDisplayName().equals(displayName))
                .findFirst();
    }
}
