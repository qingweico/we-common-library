package cn.qingweico.utils;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * @author zqw
 * @date 2026/5/22
 */
@Component
public class I18nUtil {
    private static MessageSource messageSource;

    public static final String DEFAULT_LANGUAGE = "zh-CN";

    public I18nUtil() {
    }

    public static String getTranslateMsg(String code, Object... params) {
        return messageSource.getMessage(code, params, Arrays.toString(params), getLocale());
    }

    public static String getMessage(String code, Object[] param, String defaultMessage) {
        return messageSource.getMessage(code, param, defaultMessage, getLocale());
    }

    public static String getMessage(String code, Object[] param, String defaultMessage, Locale locale) {
        return messageSource.getMessage(code, param, defaultMessage, locale);
    }

    public static String getLangId() {
        return DEFAULT_LANGUAGE;
    }

    private static Locale getLocale() {
        String langId = getLangId();
        return LocaleUtils.toLocale(StrUtil.replace(langId, 2, 3, '_'));
    }

    @Autowired
    public void setMessageSource(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }
}
