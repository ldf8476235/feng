package com.yyds.feng.op.utils;

import java.time.*;

public class WeekUtils {

    public static class WeekRange {
        public LocalDate start;  // 周日
        public LocalDate end;    // 周六
    }

    public static WeekRange getLastWeekRange() {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDateTime now = LocalDateTime.now(zone);

        // 计算“本周周日 08:00”的时间点
        LocalDateTime thisWeekStart = getWeekStartSunday8(now);

        // 如果当前时间 < 周日 8:00，说明仍属于上一周 → 再往前推一个星期
        if (now.isBefore(thisWeekStart)) {
            thisWeekStart = thisWeekStart.minusWeeks(1);
        }

        // 上一周的开始和结束
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1).toLocalDate();
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        WeekRange r = new WeekRange();
        r.start = lastWeekStart;
        r.end = lastWeekEnd;
        return r;
    }

    /**
     * 计算当前日期对应的“本周周日 08:00”
     */
    private static LocalDateTime getWeekStartSunday8(LocalDateTime dt) {
        // LocalDate 的 DayOfWeek: MON=1 ... SUN=7，因此要把周日变成0
        int dow = dt.getDayOfWeek().getValue() % 7; // 周日=0
        LocalDate sunday = dt.toLocalDate().minusDays(dow);
        return sunday.atTime(8, 0);
    }


}
