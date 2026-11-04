package com.temple.vazhipadu.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class TimeUtils {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private TimeUtils() {
    }

    public static Instant getStartOfDayInIst(LocalDate date) {
        return date.atStartOfDay(IST_ZONE).toInstant();
    }

    public static Instant getEndOfDayInIst(LocalDate date) {
        return date.plusDays(1).atStartOfDay(IST_ZONE).toInstant();
    }

    public static LocalDate toIstLocalDate(Instant instant) {
        return instant.atZone(IST_ZONE).toLocalDate();
    }
}
