package com.yyds.feng.op.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class WeekUtilsTest {
    public static void main(String[] args) {
        test("2025-12-07T07:00:00");  // 周日 07:00（未到8点 → 属于上一周）
        test("2025-12-07T08:00:00");  // 周日 08:00（新的一周）
        test("2025-12-08T10:00:00");  // 周一
        test("2025-12-13T23:00:00");  // 周六
    }

    private static void test(String dateTimeStr) {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDateTime dt = LocalDateTime.parse(dateTimeStr);

        System.out.println("=======================================");
        System.out.println("当前时间: " + dt);

        WeekUtils.WeekRange range = getLastWeekRangeForTest(dt);
        System.out.println("上周开始: " + range.start);
        System.out.println("上周结束: " + range.end);
        System.out.println("=======================================");
    }

    /**
     * ★★★ 用于测试的版本 —— 用传入时间代替 now()
     */
    private static WeekUtils.WeekRange getLastWeekRangeForTest(LocalDateTime now) {
        ZoneId zone = ZoneId.of("Asia/Shanghai");

        // 本周日 08:00
        LocalDateTime thisWeekStart = getWeekStartSunday8(now);

        // 如果当前时间 < 周日 08:00，说明仍在上一周
        if (now.isBefore(thisWeekStart)) {
            thisWeekStart = thisWeekStart.minusWeeks(1);
        }

        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1).toLocalDate();
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        WeekUtils.WeekRange r = new WeekUtils.WeekRange();
        r.start = lastWeekStart;
        r.end = lastWeekEnd;
        return r;
    }

    private static LocalDateTime getWeekStartSunday8(LocalDateTime dt) {
        int dow = dt.getDayOfWeek().getValue() % 7; // 周日=0
        LocalDate sunday = dt.toLocalDate().minusDays(dow);
        return sunday.atTime(8, 0);
    }
}
