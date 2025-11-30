package cn.qingweico.convert;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @author zqw
 * @date 2024/1/30
 */
public final class NumberConvert {

    /**
     * 格式化double类型数据
     * pattern : #,###.00 : 千位分分割, 并保留两位小数, 不足两位则补0
     * #.00 : 小数位不足两位则补0
     * #.## : 最多显示两位小数, 不会添加额外的0
     * @param number double
     * @return String
     */
    public static String fixDouble(double number) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        return decimalFormat.format(number);
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
}
