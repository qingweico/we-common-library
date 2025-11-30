package cn.qingweico.convert;

import cn.hutool.core.text.StrPool;
import cn.hutool.json.JSONUtil;
import cn.qingweico.constants.Symbol;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zqw
 * @date 2025/7/26
 */
@Slf4j
public class StringConvert {

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
                    return DateConvert.toString((Date) obj);
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

    public static String prettyJson(Object source) {
        if (ObjectUtils.isEmpty(source)) {
            return StrPool.EMPTY_JSON;
        }
        try {
            if (source instanceof String str) {
                if (JSONUtil.isTypeJSONObject(str)) {
                    return new org.json.JSONObject(str).toString(4);
                }
                if (JSONUtil.isTypeJSONArray(str)) {
                    return new org.json.JSONArray(str).toString(4);
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
