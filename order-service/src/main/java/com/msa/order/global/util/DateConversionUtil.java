package com.msa.order.global.util;

import java.time.*;

public class DateConversionUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static OffsetDateTime StringToOffsetDateTime(String createAt) {
        LocalDate parse = LocalDate.parse(createAt);
        LocalTime now = LocalTime.now(KST);
        LocalDateTime localDateTime = LocalDateTime.of(parse, now);
        return localDateTime.atZone(KST).toOffsetDateTime();
    }

}
