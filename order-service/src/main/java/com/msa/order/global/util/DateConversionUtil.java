package com.msa.order.global.util;

import org.springframework.util.StringUtils;

import java.time.*;

public class DateConversionUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static OffsetDateTime StringToOffsetDateTime(String dateAt) {

        if (!StringUtils.hasText(dateAt)) {
            return null;
        }

        LocalDate parse = LocalDate.parse(dateAt);
        LocalTime now = LocalTime.now(KST);
        LocalDateTime localDateTime = LocalDateTime.of(parse, now);
        return localDateTime.atZone(KST).toOffsetDateTime();
    }

    public static OffsetDateTime plusBusinessDay(OffsetDateTime base, Integer businessDays) {
        if (businessDays < 0) {
            throw new IllegalArgumentException("잘못된 날짜 설정입니다.");
        }

        OffsetDateTime d = base;
        int added = 0;

        while (added < businessDays) {
            d = d.plusDays(1);
            DayOfWeek w = d.atZoneSimilarLocal(KST).getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return d;
    }

}
