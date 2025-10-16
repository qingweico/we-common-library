package cn.qingweico.convert;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.qingweico.constants.Symbol;
import cn.qingweico.model.enums.ConversionMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zqw
 * @date 2025/7/26
 */
@Slf4j
public class Convert {

    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    private static final long TB = GB * 1024;
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
     * 将数字转换为字符串(四舍五入)
     *
     * @param number BigDecimal
     * @param format 格式化格式
     * @return 格式化后的String
     */
    public static String toString(BigDecimal number, String format) {
        DecimalFormat formatter = new DecimalFormat(format);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(number);
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

    public static String toString(Object obj) {
        return toString(obj, Symbol.EMPTY);
    }

    public static String toString(Object obj, String nvl) {
        try {
            if (obj != null) {
                if (obj instanceof String) {
                    return (String) obj;
                } else if (obj instanceof Double) {
                    return BigDecimal.valueOf((Double) obj).stripTrailingZeros().toPlainString();
                } else if (obj instanceof Float) {
                    return new BigDecimal(String.valueOf(((Float) obj).floatValue())).stripTrailingZeros().toPlainString();
                } else if (obj instanceof Date) {
                    return toString((Date) obj);
                } else if (obj instanceof BigDecimal) {
                    return new BigDecimal(((BigDecimal) obj).toPlainString()).stripTrailingZeros().toPlainString();
                } else if (obj instanceof Number) {
                    return String.valueOf(((Number) obj).longValue());
                } else {
                    return String.valueOf(obj);
                }
            } else {
                return nvl;
            }
        } catch (Exception e) {
            return nvl;
        }
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
        return Convert.toString(date, targetFormat);
    }

    /**
     * 将一个整数拆分为指定数量的批次, 尽可能均匀分配余数
     *
     * @param value 要拆分的整数值, 必须是非负整数(value >= 0)
     * @param batch 要拆分的批次数量, 必须是正整数(batch > 0)
     * @return 包含拆分结果的数组
     */
    public static int[] splitInteger(int value, int batch) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative");
        }
        if (batch <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        int quotient = value / batch;
        int remainder = value % batch;

        int[] result = new int[batch];
        for (int i = 0; i < batch; i++) {
            if (remainder > 0) {
                result[i] = quotient + 1;
                remainder--;
            } else {
                result[i] = quotient;
            }
        }
        return result;
    }

    /**
     * 将各种类型的数值对象安全地转换为 {@link BigDecimal} 类型
     *
     * @param obj The object to be converted
     * @return BigDecimal obj
     */
    public static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        try {
            if (obj instanceof String) {
                if (StringUtils.isBlank(obj.toString())) {
                    return BigDecimal.ZERO;
                }
                return new BigDecimal((String) obj);
            } else if (obj instanceof Double) {
                return BigDecimal.valueOf((Double) obj);
            } else if (obj instanceof Long) {
                return BigDecimal.valueOf((Long) obj);
            } else if (obj instanceof Float) {
                return new BigDecimal(obj.toString());
            } else if (obj instanceof Integer) {
                return new BigDecimal((Integer) obj);
            } else if (obj instanceof BigInteger) {
                return new BigDecimal((BigInteger) obj);
            } else {
                throw new ClassCastException("Can Not make [" + obj + "] into a BigDecimal.");
            }
        } catch (NumberFormatException e) {
            throw new ClassCastException("Can Not make [" + obj + "] into a BigDecimal.");
        }
    }

    /**
     * 将输入字符串中的字母转换为小写, 并升序排列
     *
     * @param s 待处理的字符串
     * @return 仅含小写字母, 按升序排列
     */
    public static String toLower(String s) {
        return toLower(s, false);
    }

    /**
     * 将输入字符串中的字母转换为小写, 并排序
     *
     * @param s   待处理的字符串
     * @param des 是否倒序
     * @return 仅含小写字母, 并排序
     */
    public static String toLower(String s, boolean des) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        int n = s.length();
        ArrayList<Character> list = new ArrayList<>(n);
        boolean allLower = true;
        for (int i = 0; i < n; i++) {
            int c = s.charAt(i);
            if (((c - 'A') | ('Z' - c)) >= 0) {
                allLower = false;
                break;
            }
        }
        if (allLower) {
            char[] chars = s.toCharArray();
            for (var c : chars) {
                list.add(c);
            }
        } else {
            for (int i = 0; i < n; i++) {
                int c = s.charAt(i);
                if (Character.isLetter(c)) {
                    if (((c - 'A') | ('Z' - c)) >= 0) {
                        list.add((char) (c + 0x20));
                    } else {
                        list.add((char) c);
                    }
                }
            }
        }

        list.sort((a, b) -> des ? b - a : a - b);
        return list.stream().map(String::valueOf).collect(Collectors.joining());
    }

    /**
     * @see FileUtils#byteCountToDisplaySize(long)
     */
    public static String byteCountToDisplaySize(long bytes) {
        if (bytes >= TB) {
            return format(bytes, TB, "TB");
        } else if (bytes >= GB) {
            return format(bytes, GB, "GB");
        } else if (bytes >= MB) {
            return format(bytes, MB, "MB");
        } else if (bytes >= KB) {
            return format(bytes, KB, "KB");
        } else {
            return bytes + " B";
        }
    }

    private static String format(long bytes, long unit, String suffix) {
        double value = (double) bytes / unit;
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        return decimalFormat.format(value) + StrUtil.SPACE + suffix;
    }

    public static String convertMills(long milliseconds, ConversionMethod method) {
        switch (method) {
            case COMMONS -> {
                return commonsImpl(milliseconds);
            }
            case JODA -> {
                return jodaImpl(milliseconds);
            }
            case JDK -> {
                return jdkImpl(milliseconds);
            }
            default -> throw new IllegalArgumentException("Unsupported conversion method: " + method);
        }
    }

    public static String convertMills(long milliseconds) {
        return convertMills(milliseconds, ConversionMethod.JDK);
    }

    private static String commonsImpl(long milliseconds) {
        return DurationFormatUtils.formatDuration(milliseconds, "HH小时mm分钟ss秒SSS毫秒");
    }

    private static String jodaImpl(long milliseconds) {
        Duration duration = new Duration(milliseconds);
        Period period = duration.toPeriod();
        PeriodFormatter formatter = new PeriodFormatterBuilder().printZeroAlways().appendHours().appendSuffix("小时").appendMinutes().appendSuffix("分钟").appendSecondsWithOptionalMillis().appendSuffix("秒").toFormatter();
        return formatter.print(period);
    }

    private static String jdkImpl(long milliseconds) {
        java.time.Duration jdkDuration = java.time.Duration.ofMillis(milliseconds);
        long hours = jdkDuration.toHours();
        jdkDuration = jdkDuration.minusHours(hours);
        long minutes = jdkDuration.toMinutes();
        jdkDuration = jdkDuration.minusMinutes(minutes);
        long seconds = jdkDuration.toSeconds();
        jdkDuration = jdkDuration.minusSeconds(seconds);
        long millis = jdkDuration.toMillis();
        return String.format("%d小时%d分钟%d秒%d毫秒", hours, minutes, seconds, millis);
    }

    public static String prettyJson(Object source) {
        if (ObjectUtils.isEmpty(source)) {
            return StrPool.EMPTY_JSON;
        }
        try {
            if (source instanceof String str) {
                if (JSONUtil.isTypeJSON(str)) {
                    return new JSONObject(str).toString(4);
                }
                return str;
            }
            if (source instanceof Map<?, ?> map) {
                return new JSONObject(map).toString(4);
            }

            if (source instanceof JSONTokener jsonTokener) {
                return new JSONObject(jsonTokener).toString(4);
            }
            return new JSONObject(source).toString(4);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
            return source.toString();
        }
    }



    public static void main(String[] args) {
        System.out.println(toLower("ABCD&&*^&^EFG"));
        System.out.println(toLower("Zda@%^!@#$%^&*()*^$$#$EaqpoiRdq"));
        System.out.println(toLower("abcdefg", true));
    }

}
