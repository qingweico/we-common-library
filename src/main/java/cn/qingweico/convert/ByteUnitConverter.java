package cn.qingweico.convert;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.io.FileUtils;

import java.text.DecimalFormat;

/**
 * @author zqw
 * @date 2025/11/30
 * @see FileUtils#byteCountToDisplaySize(long)
 */
public class ByteUnitConverter {
    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    private static final long TB = GB * 1024;

    public static String convert(long bytes) {
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

}
