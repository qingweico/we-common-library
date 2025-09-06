package cn.qingweico.reflect;

import cn.qingweico.io.Print;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;

/**
 * ---------------------------------- 反射工具类 ----------------------------------
 * 反射调用会带来不少的性能开销 原因有四个:
 * 变长参数方法导致的Object数组,基本类型的拆箱和装箱,方法内联,以及本地方法调用
 * {@link cn.hutool.core.util.ReflectUtil}
 * {@link org.springframework.util.ReflectionUtils}
 * Reflection Utility class, generic methods are defined from {@link FieldUtils},
 * {@link MethodUtils}, {@link ConstructorUtils}
 *
 * @author zqw
 * @see Method
 * @see Field
 * @see Constructor
 * @see Array
 * @see MethodUtils
 * @see FieldUtils
 * @see ConstructorUtils
 */
@Slf4j
public class ReflectUtils {
    private ReflectUtils() {
    }

    /**
     * 改变private/protected的成员变量为public
     */
    public static void makeAccessible(Object ins, Field field) {
        boolean notPubField = !Modifier.isPublic(field.getModifiers());
        boolean notPubClass = !Modifier.isPublic(field.getDeclaringClass().getModifiers());
        boolean isFinal = Modifier.isFinal(field.getModifiers());
        boolean cannotAccess = !field.canAccess(ins) && (notPubField || notPubClass || isFinal);
        if (cannotAccess) {
            field.setAccessible(true);
        }
    }

    /**
     * Convert {@link Array} object to {@link List}
     *
     * @param array array object
     * @return {@link List}
     * @throws IllegalArgumentException if the object argument is not an array
     */
    @Nonnull
    public static <T> List<T> toList(Object array) throws IllegalArgumentException {
        int length = Array.getLength(array);
        List<T> list = Lists.newArrayListWithCapacity(length);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            @SuppressWarnings("unchecked") T t = (T) toObject(element);
            list.add(t);
        }
        return list;
    }

    private static Object toObject(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> type = object.getClass();
        if (type.isArray()) {
            return toList(object);
        } else {
            return object;
        }
    }


    /**
     * Read non-static-fields value as {@link Map}
     *
     * @param object object to be read
     * @return fields value as {@link Map}
     */
    @Nonnull
    public static Map<String, Object> readFieldsAsMap(Object object) {
        Map<String, Object> fieldsAsMap = Maps.newLinkedHashMap();
        Class<?> type = object.getClass();
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {

            // To filter static fields
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);

            try {
                String fieldName = field.getName();
                Object fieldValue = field.get(object);
                if (fieldValue != null) {
                    Class<?> fieldValueType = fieldValue.getClass();
                    if (ClassUtils.isPrimitiveOrWrapper(fieldValueType)) {
                        System.out.printf("fieldValueType: [%s] isPrimitiveOrWrapper%n", fieldValueType);
                    } else if (fieldValueType.isArray()) {
                        fieldValue = toList(fieldValue);
                    } else if ("java.lang".equals(fieldValueType.getPackage().getName())) {
                        System.out.printf("fieldValueType [%s] class name start with java.lang%n", fieldValueType);
                    } else {
                        fieldValue = readFieldsAsMap(fieldValue);
                    }
                }
                fieldsAsMap.put(fieldName, fieldValue);
            } catch (IllegalAccessException e) {
                Print.err(e.getMessage());
            }
        }
        return fieldsAsMap;
    }

    /**
     * {@code jdk.internal.reflect.Reflection#getCallerClass()}
     */
    public static Class<?> getCallerClass() {
        return MethodHandles.lookup().lookupClass();
    }
}
