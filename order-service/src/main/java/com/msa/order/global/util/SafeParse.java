package com.msa.order.global.util;

import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class SafeParse {
    private SafeParse() {}

    public static Long toLongOrNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        try { return Long.valueOf(s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("숫자 형식 오류: '" + s + "'"); }
    }

    public static Integer toIntegerOrNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        try { return Integer.valueOf(s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("정수 형식 오류: '" + s + "'"); }
    }

    public static BigDecimal toBigDecimalOrNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        try { return new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("소수 형식 오류: '" + s + "'"); }
    }

    public static LocalDate toLocalDateOrNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        try { return LocalDate.parse(s.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("날짜 형식 오류(yyyy-MM-dd 필요): '" + s + "'"); }
    }
}
