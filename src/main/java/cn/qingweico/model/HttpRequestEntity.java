package cn.qingweico.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

/**
 * @author zqw
 * @date 2025/10/24
 */
@Data
@Builder
public class HttpRequestEntity {
    /*请求url*/
    @NonNull
    private String requestUrl;
    /*请求方式*/
    @Builder.Default
    private RequestMethod requestMethod = RequestMethod.GET;
    /*请求头*/
    Map<String, String> requestHeaders;
    /*请求体*/
    Map<String, String> requestBody;
    /*代理域名*/
    private String proxyHost;
    /*代理端口*/
    private int proxyPort;
    /*连接超时时间(ms)*/
    @Builder.Default
    private int connectTimeout = 3000;
    /*读取超时时间(ms)*/
    @Builder.Default
    private int readTimeout = 5000;
    @Builder.Default
    /*请求时间戳*/
    private long epoch = System.currentTimeMillis();
}
