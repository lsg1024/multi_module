package com.msa.order.local.domain.sale.sale_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum SaleStatus {
    SALE("판매"),
    RETURN("반품"),
    PAYMENT("결제"),
    DISCOUNT("DC"),
    PAYMENT_TO_BANK("통장"),
    WG("WG");

    private final String displayName;

    SaleStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {return displayName; }

    public static Optional<SaleStatus> fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(s -> s.getDisplayName().equals(displayName))
                .findFirst();
    }
}
