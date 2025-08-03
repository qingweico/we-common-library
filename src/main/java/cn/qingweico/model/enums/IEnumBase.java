package cn.qingweico.model.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 枚举基接口, 提供枚举类型的通用操作能力
 *
 * <p><b>基础用法示例:</b></p>
 * <pre>{@code
 * public enum UserStatus implements IEnumBase {
 *     ENABLED("1", "启用"),
 *     DISABLED("0", "禁用");
 *
 *     private final String code;
 *     private final String text;
 *
 *     UserStatus(String code, String text) {
 *         this.code = code;
 *         this.text = text;
 *     }
 *
 *     @Override public String getCode() { return this.code; }
 *     @Override public String getText() { return this.text; }
 * }
 *
 * // 使用示例
 * boolean isEnabled = UserStatus.ENABLED.is("1"); // true
 * UserStatus status = IEnumBase.parse(UserStatus.class, "0"); // DISABLED
 * }</pre>
 *
 * @author zqw
 * @date 2025/8/1
 * @see Enum
 */
public interface IEnumBase {

    /**
     * 获取枚举的唯一编码
     *
     * @return 非空的枚举编码字符串
     */
    String getCode();

    /**
     * 获取枚举的显示文本
     *
     * @return 非空的枚举描述文本
     */
    String getText();

    /**
     * 判断当前枚举是否匹配指定编码或名称
     *
     * <p><b>使用示例:</b></p>
     * <pre>{@code
     * Color.RED.is("RED")  // true
     * Color.RED.is("1")    // 假设getCode()返回"1"
     * }</pre>
     *
     * @param codeOrName 要匹配的编码或枚举名称(大小写敏感)
     * @return 匹配成功返回true，否则false
     */
    default boolean is(String codeOrName) {
        return StringUtils.equals(this.getCode(), codeOrName)
                || StringUtils.equals(codeOrName, this.getName());
    }

    /**
     * 静态方法:判断枚举实例是否匹配指定编码或名称
     *
     * @param unifyEnum 枚举实例(非null)
     * @param codeOrName 要匹配的编码或名称
     * @return 匹配结果
     * @see #is(String)
     */
    static boolean is(IEnumBase unifyEnum, String codeOrName) {
        if(unifyEnum == null) {
            return false;
        }
        return unifyEnum.is(codeOrName);
    }

    /**
     * 将字符串转换为枚举实例
     * <p><b>注意:</b>该方法可能返回null</p>
     *
     * <p><b>使用示例:</b></p>
     * <pre>{@code
     * // 可能返回null的转换
     * Status status = IEnumBase.parse(Status.class, "1");
     * }</pre>
     *
     * @param <T> 枚举类型
     * @param clazz 目标枚举类(非null)
     * @param codeOrName 要转换的编码或名称
     * @return 匹配的枚举实例，未找到时返回null
     */
    static <T extends IEnumBase> T parse(Class<T> clazz, String codeOrName) {
        if(clazz == null) {
            return null;
        }
        T[] all = clazz.getEnumConstants();
        for (T obj : all) {
            if (obj.is(codeOrName)) {
                return obj;
            }
        }
        return null;
    }

    /**
     * 安全转换字符串到枚举(带默认值)
     *
     * <p><b>使用示例:</b></p>
     * <pre>{@code
     * // 安全转换(带默认值)
     * Status status = IEnumBase.parse(Status.class, "invalid", Status.UNKNOWN);
     * }</pre>
     *
     * @param <T> 枚举类型
     * @param clazz 目标枚举类
     * @param codeOrName 要转换的编码或名称
     * @param nvl 转换失败时返回的默认值
     * @return 匹配的枚举实例或默认值(不会返回null)
     * @see #parse(Class, String)
     */
    static <T extends IEnumBase> T parse(Class<T> clazz, String codeOrName, T nvl) {
        try {
            T value = parse(clazz, codeOrName);
            return value != null ? value : nvl;
        } catch (Exception e) {
            return nvl;
        }
    }

    /**
     * 获取枚举的标准名称
     * <p>优先返回{@link Enum#name()}，非枚举类型则返回{@link #getCode()}</p>
     *
     * @return 非空的名称字符串
     */
    default String getName() {
        return this instanceof Enum
                ? ((Enum<?>) this).name()
                : getCode();
    }
}
