package com.msa.common.global.common_enum.expense_enum;

public enum ExpenseType {
    INCOME("INCOME", "입고"),
    EXPENSE("EXPENSE", "출고");

    private final String code;
    private final String description;

    ExpenseType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
