package cn.qingweico.datetime;

import org.apache.commons.lang3.time.FastDateFormat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author zqw
 * @date 2021/4/7
 * @see org.apache.commons.lang3.time.DateUtils
 */
public class DateUtil {
    private static final String DATE_TIME_FORMATTER = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER);

    /**
     * 将字符串解析为 {@link LocalDateTime} 对象
     *
     * @param dateStr 符合默认格式(yyyy-MM-dd HH:mm:ss)的日期时间字符串
     * @return 解析后的 LocalDateTime 对象
     * @throws java.time.format.DateTimeParseException 如果字符串格式不匹配(需要严格匹配)
     * @see #DEFAULT_DATE_TIME_FORMATTER
     */
    public static LocalDateTime parse(String dateStr) {
        return java.time.LocalDateTime.parse(dateStr, DEFAULT_DATE_TIME_FORMATTER);
    }

    /**
     * 使用指定pattern格式化当前时间
     *
     * @param pattern 日期格式模式
     * @return 格式化后的日期字符串
     * @see FastDateFormat
     */
    public static String format(String pattern) {
        return FastDateFormat.getInstance(pattern).format(new Date());
    }

    /**
     * 使用默认格式格式化日期对象
     *
     * @param date 要格式化的日期对象
     * @return 格式化后的日期字符串 {@link #DATE_TIME_FORMATTER}
     * @see #DATE_TIME_FORMATTER
     */
    public static String format(Date date) {
        return FastDateFormat.getInstance(DATE_TIME_FORMATTER).format(date);
    }

    /**
     * 使用指定pattern格式化日期对象
     *
     * @param date    要格式化的日期对象
     * @param pattern 日期格式模式
     * @return 格式化后的日期字符串
     */
    public static String format(Date date, String pattern) {
        return FastDateFormat.getInstance(pattern).format(date);
    }

    /**
     * 获取当前时间的默认格式字符串
     *
     * @return 当前时间的格式化字符串 {@link #DATE_TIME_FORMATTER}
     * @see #format(Date)
     */
    public static String now() {
        return format(new Date());
    }

    /**
     * 将毫秒数转换为格式化的日期时间字符串
     * 2025-08-09T08:30:11.511 这种时间格式的被称为 ISO-8601
     * 特点:
     * ---- ISO-8601 是国际标准化组织(ISO)制定的日期和时间表示法的国际标准
     * ---- 按从大到小的顺序排列
     * ---- 使用数字表示, 避免语言差异
     * ---- 支持时区信息
     * ---- 使用 T 分隔日期和时间
     * ---- 可扩展(可包含毫秒、微秒等)
     *
     * @param time 毫秒数
     * @return {@link #DATE_TIME_FORMATTER 格式的字符串日期}
     * @see DateTimeFormatter#ISO_LOCAL_DATE_TIME
     */
    public static String epochMillisToLocalDateTime(long time) {
        Instant instant = Instant.ofEpochMilli(time);
        LocalDateTime ld = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return ld.format(DEFAULT_DATE_TIME_FORMATTER);
    }
}
