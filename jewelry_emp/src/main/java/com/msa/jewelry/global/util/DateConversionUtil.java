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

    public static LocalDateTime ParseLocalDateTime(String dateAt) {
        if (!StringUtils.hasText(dateAt)) {
            return null;
        }

        return LocalDateTime.parse(dateAt, FORMATTER);
    }

    public static LocalDate LocalDateTimeToLocalDate(LocalDateTime dateAt) {
        if (dateAt == null) {
            return null;
        }

        return dateAt.toLocalDate();
    }

    public static String LocalDateTimeToString(LocalDateTime dateAt) {
        if (dateAt == null) {
            return null;
        }
        return dateAt.format(FORMATTER);
    }
}
