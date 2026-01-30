package cn.qingweico.model;

import lombok.Builder;
import lombok.Getter;
import org.apache.http.client.config.RequestConfig;

/**
 * per-call-configuration
 *
 * @author zqw
 * @date 2025/8/2
 */
@Builder
@Getter
public class RequestConfigOptions {
    /**
     * 代理域名
     */
    private String proxyHost;
    /**
     * 代理端口
     */
    private int proxyPort;
    /**
     * TCP建立连接超时时间
     */
    private Integer connectTimeout;
    /**
     * 从连接池中获取连接时允许等待的最长时间
     */
    private Integer connectionRequestTimeout;
    /**
     * 等待服务器返回数据的最大时间(等价于读超时/readTimeout)
     */
    private Integer socketTimeout;
    /**
     * 整体请求超时时间(整个 HTTP Call 的总耗时上限)
     */
    private Integer callTimeout;
}
