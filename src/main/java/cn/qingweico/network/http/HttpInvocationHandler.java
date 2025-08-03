package cn.qingweico.network.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author zqw
 * @date 2025/7/26
 */
@Slf4j
public class HttpInvocationHandler implements InvocationHandler {
    private final CloseableHttpClient realClient;

    public HttpInvocationHandler(CloseableHttpClient realClient) {
        this.realClient = realClient;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("请求开始: 参数={}", args);

        Object result = method.invoke(realClient, args);

        if (result instanceof HttpResponse) {
            log.info("响应状态: {}", ((HttpResponse) result).getStatusLine());
        }
        return result;
    }
}
