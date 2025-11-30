package cn.qingweico.convert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author zqw
 * @date 2025/11/30
 */
public class DateConvert {
    private static final String DEFAULT_DATE_FORMAT_STRING = "yyyy-MM-dd";
    private static final ThreadLocal<SimpleDateFormat> DEFAULT_DATE_FORMAT_THREAD_LOCAL = new ThreadLocal<>();

    private static SimpleDateFormat getDefaultDateFormat() {
        SimpleDateFormat df = DEFAULT_DATE_FORMAT_THREAD_LOCAL.get();
        if (df == null) {
            df = new SimpleDateFormat(DEFAULT_DATE_FORMAT_STRING, Locale.CHINA);
            DEFAULT_DATE_FORMAT_THREAD_LOCAL.set(df);
        }
        return df;
    }
    /**
     * 格式化当前日期为字符串格式 默认按照 yyyy-MM-dd HH:mm:ss
     *
     * @param obj Date日期
     * @return yyyy-MM-dd HH:mm:ss 格式的日期字符串
     */
    public static String toString(Date obj) {
        return toString(obj, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 将字符串根据指定格式转换为日期
     *
     * @param dateString 日期字符串
     * @param format     指定的格式
     * @return Date
     */
    public static Date toDate(String dateString, String format) {
        try {
            if (format == null || DEFAULT_DATE_FORMAT_STRING.equals(format)) {
                return getDefaultDateFormat().parse(dateString);
            } else {
                return new SimpleDateFormat(format, Locale.CHINA).parse(dateString);
            }
        } catch (ParseException e) {
            throw new ClassCastException("无法将字符串" + dateString + "转换为Date类型");
        }
    }

    /**
     * 将日期根据指定格式转换为字符串
     *
     * @param date   给定的 Date 类型的日期
     * @param format 格式
     * @return format后的String
     */
    public static String toString(Date date, String format) {
        if (format == null || DEFAULT_DATE_FORMAT_STRING.equals(format)) {
            return getDefaultDateFormat().format(date);
        } else {
            return new SimpleDateFormat(format, Locale.CHINA).format(date);
        }
    }


    /**
     * 将日期字符串转换为指定格式
     *
     * @param dateStr      字符串形式的date
     * @param strFormat    当前字符串所属的日期格式
     * @param targetFormat 想要转换为的日期格式
     * @return String类型的日期格式
     */
    public static String dateFormat(String dateStr, String strFormat, String targetFormat) {
        Date date = toDate(dateStr, strFormat);
        return StringConvert.toString(date, targetFormat);
    }
}
