package com.msa.common.global.common_enum.sale_enum;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum SaleStatus {
    SALE("판매"),
    RETURN("반품"),
    PAYMENT("결제"),
    DISCOUNT("DC"),
    PAYMENT_TO_BANK("통장"),
    WG("WG"),
    PURCHASE("매입");

    private final String displayName;

    SaleStatus(String displayName) {
        this.displayName = displayName;
    }
    @JsonValue
    public String getDisplayName() { return displayName; }

    public static SaleStatus fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(status -> status.displayName.equals(displayName))
                .findFirst()
                .orElse(null);
    }

}
