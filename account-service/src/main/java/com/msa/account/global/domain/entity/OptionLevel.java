package com.msa.account.global.domain.entity;

import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;

@Table
@RequiredArgsConstructor
public enum OptionLevel {

    ONE("ONE", "1"),
    TWO("TWO", "2"),
    THREE("THREE", "3"),
    FOUR("FOUR", "4");

    private final String key;
    private final String level;

    public static String getLevelByKey(String key) {
        for (OptionLevel type : OptionLevel.values()) {
            if (type.key.equals(key)) {
                return type.level;
            }
        }
        return null;
    }
}
