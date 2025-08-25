package com.msa.order.local.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum Kind {
    CREATE("생성"),
    UPDATE("수정"),
    DELETE("삭제"),
    RESTORE("반납"),
    EXPECT("출고");

    private final String displayName;

    Kind(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static Optional<Kind> fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(s -> s.getDisplayName().equals(displayName))
                .findFirst();
    }
}
