package com.yunshu.zhixue.common.util;

import com.yunshu.zhixue.common.constant.AppConstants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 时间转换：数据库统一存 UTC，接口展示与日期筛选按 Asia/Shanghai。
 */
public final class TimeUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtils() {
    }

    /** UTC LocalDateTime → Asia/Shanghai LocalDateTime。 */
    public static LocalDateTime utcToBiz(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return utc.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(AppConstants.BIZ_ZONE)
                .toLocalDateTime();
    }

    /** Asia/Shanghai 自然日 00:00 → UTC LocalDateTime（查询下界）。 */
    public static LocalDateTime bizDayStartToUtc(LocalDate date) {
        return date.atStartOfDay(AppConstants.BIZ_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    /** Asia/Shanghai 自然日次日 00:00 → UTC LocalDateTime（查询上界，开区间）。 */
    public static LocalDateTime bizDayEndExclusiveToUtc(LocalDate date) {
        return date.plusDays(1).atStartOfDay(AppConstants.BIZ_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    /** 格式化为 Asia/Shanghai 字符串（CSV 导出等）。 */
    public static String formatBiz(LocalDateTime utc) {
        LocalDateTime biz = utcToBiz(utc);
        return biz == null ? "" : biz.format(DATE_TIME_FORMATTER);
    }
}
