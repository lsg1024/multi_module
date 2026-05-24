package com.msa.jewelry.global.util;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateConversionUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateConversionUtil() {
    }

    /**
     * "yyyy-MM-dd" 형식 날짜 문자열을 KST 기준 {@link LocalDateTime} (입력 시점의 시각 부분 사용)
     * 으로 변환. 입력이 비어있거나 "null" 이면 null.
     */
    public static LocalDateTime StringToLocalDateTime(String dateAt) {
        if (!StringUtils.hasText(dateAt)) {
            return null;
        }

        if (dateAt.equalsIgnoreCase("null")) {
            return null;
        }

        try {
            LocalDate parse = LocalDate.parse(dateAt);
            LocalTime now = LocalTime.now(KST);
            return LocalDateTime.of(parse, now);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * "yyyy-MM-dd HH:mm:ss" 형식 문자열을 {@link LocalDateTime} 으로 파싱.
     */
    public static LocalDateTime ParseLocalDateTime(String dateAt) {
        if (!StringUtils.hasText(dateAt)) {
            return null;
        }

        return LocalDateTime.parse(dateAt, FORMATTER);
    }

    /**
     * {@link LocalDateTime} → {@link LocalDate} (당일 날짜 부분 추출).
     * 애플리케이션·DB 가 모두 KST 기준이므로 별도 zone 변환 없이 toLocalDate() 동등.
     */
    public static LocalDate LocalDateTimeToLocalDate(LocalDateTime dateAt) {
        if (dateAt == null) {
            return null;
        }

        return dateAt.toLocalDate();
    }

    /**
     * {@link LocalDateTime} → "yyyy-MM-dd HH:mm:ss" 형식 문자열.
     */
    public static String LocalDateTimeToString(LocalDateTime dateAt) {
        if (dateAt == null) {
            return null;
        }
        return dateAt.format(FORMATTER);
    }
}
