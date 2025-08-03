package cn.qingweico.model;

import lombok.Builder;
import lombok.Getter;
import org.apache.http.client.config.RequestConfig;

/**
 * property source {@link RequestConfig}
 *
 * @author zqw
 * @date 2025/8/2
 */
@Builder
@Getter
public class RequestConfigOptions {
    // 连接超时时间(ms)
    @Builder.Default
    private int connectTimeout = -1;
    // 连接请求超时时间(ms)
    @Builder.Default
    private int connectionRequestTimeout = -1;
    // socket 读写超时时间(ms)
    @Builder.Default
    private int socketTimeout = -1;
}
