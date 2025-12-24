package com.msa.order.global.util;

import org.springframework.util.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateConversionUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static OffsetDateTime StringToOffsetDateTime(String dateAt) {

        if (!StringUtils.hasText(dateAt)) {
            return null;
        }

        if (dateAt.equalsIgnoreCase("null")) {
            return null;
        }

        try {
            LocalDate parse = LocalDate.parse(dateAt);
            LocalTime now = LocalTime.now(KST);
            LocalDateTime localDateTime = LocalDateTime.of(parse, now);
            return localDateTime.atZone(KST).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static OffsetDateTime LocalDateToOffsetDateTime(String dateAt) {
        if (!StringUtils.hasText(dateAt)) {
            return null;
        }

        LocalDateTime localDateTime = LocalDateTime.parse(dateAt, FORMATTER);

        return localDateTime.atZone(KST).toOffsetDateTime();
    }

    public static LocalDate OffsetDateTimeToLocalDate(OffsetDateTime dateAt) {
        if (dateAt == null) {
            return null;
        }

        return dateAt.atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDate();
    }

}
