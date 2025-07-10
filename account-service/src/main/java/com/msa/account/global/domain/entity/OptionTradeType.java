package com.msa.account.global.domain.entity;

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
}
