package cn.qingweico.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.http.HttpMethod;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    private HttpMethod httpMethod = HttpMethod.GET;
    /*请求头*/
    Map<String, String> requestHeaders;
    /*请求体*/
    Map<String, String> requestBody;
    /*复杂对象请求体*/
    Map<String, Object> complexBody;
    /*请求元数据*/
    private RequestMeta meta;
    /*请求级配置*/
    private RequestConfigOptions requestConfigOptions;
    /*字符集*/
    @Builder.Default
    private Charset charset = StandardCharsets.UTF_8;
}
