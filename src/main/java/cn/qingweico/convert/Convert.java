package cn.qingweico.convert;

import cn.qingweico.constants.Symbol;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author zqw
 * @date 2025/7/26
 */
public class Convert {

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
     * @param date 给定的 Date 类型的日期
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
     * @param dateStr 字符串形式的date
     * @param strFormat 当前字符串所属的日期格式
     * @param targetFormat 想要转换为的日期格式
     * @return String类型的日期格式
     */
    public static String dateFormat(String dateStr, String strFormat, String targetFormat) {
        Date date = toDate(dateStr, strFormat);
        return Convert.toString(date, targetFormat);
    }

    /**
     * 将一个整数拆分为指定数量的批次, 尽可能均匀分配余数
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
}
