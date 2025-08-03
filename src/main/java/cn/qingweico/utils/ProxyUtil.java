package cn.qingweico.utils;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

/**
 * 动态代理工具类,用于创建代理对象并控制方法调用行为
 * <p>
 * 该工具类主要功能
 * <ul>
 *   <li>将目标对象和包装对象结合创建代理</li>
 *   <li>自动检测并处理final类或final方法的代理限制</li>
 *   <li>通过方法拦截器灵活控制代理行为</li>
 * </ul>
 *
 * @author zqw
 * @date 2025/7/26
 */
public class ProxyUtil {

    private static final Logger logger = LoggerFactory.getLogger(ProxyUtil.class);

    /**
     * 包装目标对象创建代理
     *
     * @param delegate        原始目标对象,不能为null
     * @param wrapper         包装对象的生成函数,不能为null
     * @param giveUpWhenFinal 当遇到final限制时是否放弃代理
     *                        true-放弃代理返回原始对象,false-返回包装对象
     * @param <T>             目标对象类型
     * @param <E>             包装对象类型(必须是T的子类型)
     * @return 代理对象(当可以代理时)或原始对象/包装对象(根据giveUpWhenFinal参数)
     * @throws IllegalArgumentException 如果delegate或wrapper为null
     * <p><b>使用示例</b></p>
     * <pre>{@code
     * Service original = new ServiceImpl();
     * Service proxy = ProxyUtil.wrap(original, s -> new ServiceDecorator(s), true);
     * }</pre>
     */
    public static <T, E extends T> T wrap(T delegate, Function<T, E> wrapper, boolean giveUpWhenFinal) {
        if (delegate == null || wrapper == null) {
            throw new IllegalArgumentException("Delegate and wrapper must not be null");
        }

        T wrappedBean = wrapper.apply(delegate);
        boolean isFinal = isFinal(delegate);
        if (isFinal) {
            if (giveUpWhenFinal) {
                logger.debug("Final restriction encountered, returning original object");
                return delegate;
            }
            logger.debug("Final restriction encountered, returning wrapped object");
            return wrappedBean;
        } else {
            ProxyFactoryBean factory = new ProxyFactoryBean();
            factory.setProxyTargetClass(true);
            factory.addAdvice(new ExecutorMethodInterceptor<>(wrappedBean));
            factory.setTarget(delegate);
            @SuppressWarnings("unchecked")
            T t = (T) factory.getObject();
            return t;
        }
    }

    /**
     * 包装目标对象创建代理(默认不放弃final限制的代理)
     *
     * @param delegate 原始目标对象,不能为null
     * @param wrapper  包装对象的生成函数,不能为null
     * @param <T>      目标对象类型
     * @param <E>      包装对象类型(必须是T的子类型)
     * @return 代理对象或包装对象(当遇到final限制时)
     * @see #wrap(Object, Function, boolean)
     */
    public static <T, E extends T> T wrap(T delegate, Function<T, E> wrapper) {
        return wrap(delegate, wrapper, false);
    }

    /**
     * 检测对象是否包含final限制(类或public方法)
     *
     * @param object 要检测的对象
     * @param <T>    对象类型
     * @return true-对象有final限制,false-没有final限制
     * @implNote 检测逻辑
     * 1. 首先检查类是否被final修饰
     * 2. 然后检查所有public非Object方法是否有final修饰
     */
    private static <T> boolean isFinal(T object) {
        boolean classFinal = Modifier.isFinal(object.getClass().getModifiers());
        if (classFinal) {
            return true;
        }
        try {
            for (Method method : ReflectionUtils.getAllDeclaredMethods(object.getClass())) {
                if (method.getDeclaringClass().equals(Object.class)) {
                    continue;
                }
                Method m = ReflectionUtils.findMethod(object.getClass(), method.getName(), method.getParameterTypes());
                if (m != null && Modifier.isPublic(m.getModifiers()) && Modifier.isFinal(m.getModifiers())) {
                    return true;
                }
            }
        } catch (IllegalAccessError er) {
            logger.warn("Illegal access error when checking final methods", er);
            return false;
        }
        return false;
    }

    /**
     * 方法拦截器实现,负责将方法调用转发到包装对象
     *
     * @param <T> 包装对象类型
     */
    static final class ExecutorMethodInterceptor<T> implements MethodInterceptor {

        private final T wrapper;

        /**
         * 创建拦截器实例
         *
         * @param wrapper 包装对象,不能为null
         */
        ExecutorMethodInterceptor(T wrapper) {
            this.wrapper = wrapper;
        }

        /**
         * 拦截方法调用
         *
         * @param invocation 方法调用信息
         * @return 方法调用结果
         * @throws Throwable 如果方法调用抛出异常
         * @implSpec 执行逻辑
         * 1. 尝试在包装对象中查找对应方法
         * 2. 找到则调用包装对象的方法
         * 3. 未找到则调用原始对象的方法
         */
        @Override
        public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
            Method proxyedMethod = getProxyedMethod(invocation, wrapper);
            if (proxyedMethod != null) {
                try {
                    return proxyedMethod.invoke(wrapper, invocation.getArguments());
                } catch (InvocationTargetException ex) {
                    Throwable cause = ex.getCause();
                    throw (cause != null) ? cause : ex;
                }
            } else {
                return invocation.proceed();
            }
        }

        /**
         * 在包装对象中查找与调用方法匹配的方法
         */
        private Method getProxyedMethod(MethodInvocation invocation, Object object) {
            Method method = invocation.getMethod();
            return ReflectionUtils.findMethod(object.getClass(), method.getName(), method.getParameterTypes());
        }
    }
}
