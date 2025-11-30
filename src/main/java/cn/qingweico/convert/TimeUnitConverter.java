package cn.qingweico.convert;

import cn.qingweico.model.enums.ConversionMethod;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * @author zqw
 * @date 2025/11/30
 */
public class TimeUnitConverter {

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
}
