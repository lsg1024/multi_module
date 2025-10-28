package com.msa.common.global.util;

import java.util.Optional;

public class AuditorHolder {
    private static final ThreadLocal<String> AUDITOR = new ThreadLocal<>();

    public static void setAuditor(String auditor) {
        AUDITOR.set(auditor);
    }

    public static Optional<String> getAuditor() {
        return Optional.ofNullable(AUDITOR.get());
    }

    public static void clear() {
        AUDITOR.remove();
    }
}