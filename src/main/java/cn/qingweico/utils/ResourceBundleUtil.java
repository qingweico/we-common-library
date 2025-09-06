package cn.qingweico.utils;

import jodd.util.ResourceBundleMessageResolver;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

/**
 * @author zqw
 * @date 2025/8/31
 */
public class ResourceBundleUtil {
    public static String getMessage(String key, Locale locale, Object... params) {
        ResourceBundleMessageResolver resolver = new ResourceBundleMessageResolver();
        String message = resolver.findDefaultMessage(locale, key);
        return formatMessageIfNecessary(message, locale, params);
    }

    public static String getMessage(String bundleName, String key, Locale locale, Object... params) {
        ResourceBundleMessageResolver resolver = new ResourceBundleMessageResolver();
        String message = resolver.getMessage(bundleName, locale, key);
        return formatMessageIfNecessary(message, locale, params);
    }

    private static String formatMessageIfNecessary(String message, Locale locale, Object... params) {
        if (message == null) {
            return null;
        }
        // need to replace placeholder parameter
        if (params != null && params.length > 0) {
            MessageFormat formatter = new MessageFormat(message, locale);
            return formatter.format(params);
        }
        return message;
    }

    public static String findMessage(String bundleName, String key, Locale locale, List<String> bundleNames) {
        ResourceBundleMessageResolver resolver = new ResourceBundleMessageResolver();
        if (bundleNames != null && !bundleNames.isEmpty()) {
            bundleNames.forEach(resolver::addDefaultBundle);
        }
        return resolver.findMessage(bundleName, locale, key);
    }
}
