package com.msa.product.local.grade;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum WorkGrade {
    GRADE_1("ONE", "1"),
    GRADE_2("TWO", "2"),
    GRADE_3("THREE","3"),
    GRADE_4("FOUR", "4");

    private final String key;
    private final String level;

    public static String getLevelByKey(String key) {
        for (WorkGrade type : WorkGrade.values()) {
            if (type.key.equals(key)) {
                return type.level;
            }
        }
        return null;
    }

    public String getLevel() {
        return this.level;
    }
}
