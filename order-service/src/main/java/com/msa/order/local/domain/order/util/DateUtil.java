package com.msa.order.local.domain.order.util;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class DateUtil {

    public static OffsetDateTime plusBusinessDay(OffsetDateTime base, Integer businessDays, ZoneId businessZone) {
        if (businessDays < 0) {
            throw new IllegalArgumentException("잘못된 날짜 설정입니다.");
        }

        OffsetDateTime d = base;
        int added = 0;

        while (added < businessDays) {
            d = d.plusDays(1);
            DayOfWeek w = d.atZoneSimilarLocal(businessZone).getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return d;
    }
}
