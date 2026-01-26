package com.msa.order.local.order.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChangeTracker {

    private final List<String> changes = new ArrayList<>();
    private final String operation;

    public ChangeTracker(String operation) {
        this.operation = operation;
    }

    /**
     * 문자열 값 변경을 추적합니다.
     */
    public ChangeTracker track(String fieldName, String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(formatChange(fieldName, oldValue, newValue));
        }
        return this;
    }

    /**
     * 숫자 값 변경을 추적합니다.
     */
    public ChangeTracker track(String fieldName, Integer oldValue, Integer newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(formatChange(fieldName,
                    oldValue != null ? oldValue.toString() : null,
                    newValue != null ? newValue.toString() : null));
        }
        return this;
    }

    /**
     * BigDecimal 값 변경을 추적합니다.
     */
    public ChangeTracker track(String fieldName, BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null && newValue == null) {
            return this;
        }
        if (oldValue == null || newValue == null || oldValue.compareTo(newValue) != 0) {
            changes.add(formatChange(fieldName,
                    oldValue != null ? oldValue.toPlainString() : null,
                    newValue != null ? newValue.toPlainString() : null));
        }
        return this;
    }

    /**
     * boolean 값 변경을 추적합니다.
     */
    public ChangeTracker track(String fieldName, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            changes.add(formatChange(fieldName, String.valueOf(oldValue), String.valueOf(newValue)));
        }
        return this;
    }

    /**
     * Long 값 변경을 추적합니다.
     */
    public ChangeTracker track(String fieldName, Long oldValue, Long newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(formatChange(fieldName,
                    oldValue != null ? oldValue.toString() : null,
                    newValue != null ? newValue.toString() : null));
        }
        return this;
    }

    /**
     * 변경 사항이 있는지 확인합니다.
     */
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * 변경 내용을 텍스트로 반환합니다.
     * 예: "재고 수정 | 금중량: 10.5 → 11.2, 사이즈: M → L"
     */
    public String buildContent() {
        if (changes.isEmpty()) {
            return operation;
        }
        return operation + " | " + String.join(", ", changes);
    }

    /**
     * 변경 내용만 반환합니다 (작업명 제외).
     * 예: "금중량: 10.5 → 11.2, 사이즈: M → L"
     */
    public String getChangesOnly() {
        return String.join(", ", changes);
    }

    private String formatChange(String fieldName, String oldValue, String newValue) {
        String old = oldValue != null ? oldValue : "(없음)";
        String newVal = newValue != null ? newValue : "(없음)";
        return fieldName + ": " + old + " → " + newVal;
    }
}
