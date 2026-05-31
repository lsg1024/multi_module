package com.msa.jewelry.local.common_option.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OptionTradeType {

    WEIGHT("WEIGHT", "중량"),
    PRICE("PRICE", "시세");

    private final String key;
    private final String title;

    public static String getTitleByKey(String key) {
        for (OptionTradeType type : OptionTradeType.values()) {
            if (type.key.equals(key)) {
                return type.title;
            }
        }
        return null;
    }

    public static OptionTradeType fromInput(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();
        for (OptionTradeType type : values()) {
            if (type.key.equalsIgnoreCase(trimmed) || type.title.equals(trimmed)) {
                return type;
            }
        }
        return null;
    }
}
