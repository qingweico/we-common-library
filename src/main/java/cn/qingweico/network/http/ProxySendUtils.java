package cn.qingweico.network.http;

import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeTypeUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * Mime 常量 {@link ContentType} {@link MimeTypeUtils} {@link org.springframework.util.MimeType}
 *
 * @author zqw
 * @date 2025/7/23
 */
@Slf4j
public class ProxySendUtils {

    public static String sendPost(String url, String data, String domain, String host, Integer port) {
        Response response = null;
        String responseBodyString = null;
        MediaType mediaType = MediaType.parse(ContentType.JSON.getValue());
        RequestBody body = RequestBody.create(data, mediaType);
        String txUrl = domain + url;
        try {
            Request request = new Request.Builder()
                    .url(txUrl)
                    .method(ServletUtil.METHOD_POST, body)
                    .addHeader(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue()).build();
            OkHttpClient httpClient = new OkHttpClient()
                    .newBuilder()
                    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)))
                    .readTimeout(59, TimeUnit.SECONDS)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .build();
            log.info("call [{}], param:{}", txUrl, data);
            response = httpClient.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                responseBodyString = responseBody.string();
            }
        } catch (Exception e) {
            log.error("call [{}], param:{} {}", txUrl, data, e.getMessage(), e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        LoggerFactory.getLogger(ProxySendUtils.class).info("call [{}], param:{}, response:{}", txUrl, data, responseBodyString);
        return responseBodyString;
    }

    public static void main(String[] args) {
        String proxyHost = "127.0.0.1";
        int proxyPort = 8848;
        String url = "/post";
        String domain = "https://httpbin.org";
        String data = "{}";
        System.out.println(ProxySendUtils.sendPost(url, data, domain, proxyHost, proxyPort));
    }
}
