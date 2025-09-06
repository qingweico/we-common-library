package cn.qingweico.constants;

import cn.hutool.core.text.StrPool;
import org.apache.commons.lang3.StringUtils;

/**
 * 全局符号
 * @see jodd.util.StringPool
 * @author zqw
 */
public interface Symbol extends StrPool {
    // 符号

    /**
     * Class: "class"
     */
    String CLASS = "class";

    /**
     * And: "&"
     */
    String AND = "&";

    /**
     * Equal : "="
     */
    String EQUAL = "=";

    /**
     * FULL_STOP: "。"
     */
    String FULL_STOP = "。";

    /**
     * SEMICOLON: ";"
     */
    String SEMICOLON = ";";
    /**
     * POUND_SIGN: "#"; `#`有很多种读法
     */
    String POUND_SIGN = "#";

    /**
     * @see StringUtils#SPACE
     */
    String WHITE_SPACE = StringUtils.SPACE;
    /**
     * @see StringUtils#EMPTY
     */
    String EMPTY = StringUtils.EMPTY;
}
