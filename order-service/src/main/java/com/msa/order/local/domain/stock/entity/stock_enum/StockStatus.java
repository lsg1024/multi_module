package com.msa.order.local.domain.stock.entity.stock_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum StockStatus {

    STOCK("재고"),
    NORMAL("일반"),
    RENTAL("대여"),
    RETURN("반납"),
    SALE("판매"),
    DELETE("삭제");


    private final String displayName;

    StockStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {return displayName; }

    public static Optional<StockStatus> fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(s -> s.getDisplayName().equals(displayName))
                .findFirst();
    }
}
