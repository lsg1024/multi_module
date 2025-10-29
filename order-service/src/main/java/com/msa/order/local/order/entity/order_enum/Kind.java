package com.msa.order.local.order.entity.order_enum;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Kind {
    CREATE("생성"),
    UPDATE("수정"),
    DELETED("삭제");

    private final String displayName;

    Kind(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
