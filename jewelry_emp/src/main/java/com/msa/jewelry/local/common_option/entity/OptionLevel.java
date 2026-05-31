package com.msa.jewelry.local.common_option.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OptionLevel {

    ONE("ONE", "1"),
    TWO("TWO", "2"),
    THREE("THREE", "3"),
    FOUR("FOUR", "4");

    private final String key;
    private final String grade;

    public static String getLevelByKey(String key) {
        for (OptionLevel type : OptionLevel.values()) {
            if (type.key.equals(key)) {
                return type.grade;
            }
        }
        return null;
    }

    public static OptionLevel fromInput(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();
        for (OptionLevel level : values()) {
            if (level.key.equalsIgnoreCase(trimmed) || level.grade.equals(trimmed)) {
                return level;
            }
        }
        return null;
    }

    public String getGrade() {
        return this.grade;
    }
}
