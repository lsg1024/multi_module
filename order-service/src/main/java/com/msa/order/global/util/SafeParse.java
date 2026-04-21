package com.msa.order.global.util;

import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class SafeParse {
    private SafeParse() {}

    /**
     * 빈 문자열·공백뿐인 문자열·리터럴 "null"/"undefined" 를 모두 null 로 간주한다.
     * (프론트 직렬화 과정에서 JS 의 null/undefined 가 문자열로 섞여 들어오는 경우를 방어)
     */
    private static boolean isBlankOrNullLiteral(String s) {
        if (!StringUtils.hasText(s)) return true;
        String t = s.trim();
        return "null".equalsIgnoreCase(t) || "undefined".equalsIgnoreCase(t);
    }

    public static Long toLongOrNull(String s) {
        if (isBlankOrNullLiteral(s)) return null;
        try { return Long.valueOf(s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("숫자 형식 오류: '" + s + "'"); }
    }

    public static Integer toIntegerOrNull(String s) {
        if (isBlankOrNullLiteral(s)) return null;
        try { return Integer.valueOf(s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("정수 형식 오류: '" + s + "'"); }
    }

    public static BigDecimal toBigDecimalOrNull(String s) {
        if (isBlankOrNullLiteral(s)) return null;
        try { return new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("소수 형식 오류: '" + s + "'"); }
    }

    public static LocalDate toLocalDateOrNull(String s) {
        if (isBlankOrNullLiteral(s)) return null;
        try { return LocalDate.parse(s.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("날짜 형식 오류(yyyy-MM-dd 필요): '" + s + "'"); }
    }
}
