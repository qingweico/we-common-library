package cn.qingweico.network;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.qingweico.convert.Convert;
import cn.qingweico.model.HttpRequestEntity;
import cn.qingweico.model.Poem;
import cn.qingweico.model.RequestConfigOptions;
import cn.qingweico.network.http.HttpInvocationHandler;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author zqw
 * @date 2022/6/25
 */
@Slf4j
public class NetworkUtils {
    public static String getLocalMac(InetAddress ia) throws SocketException {
        byte[] macAddress = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
        log.info("Mac Array: [{}], Mac Byte Array Length: {}", Arrays.toString(macAddress), macAddress.length);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < macAddress.length; i++) {
            if (i != 0) {
                result.append("-");
            }
            int tmp = macAddress[i] & 0xff;
            String str = Integer.toHexString(tmp);
            log.info("每8位: {}", str);
            if (str.length() == 1) {
                result.append("0").append(str);
            } else {
                result.append(str);
            }
        }
        return result.toString();
    }

    /**
     * 使用 Apache Http Client
     *
     * @param url            请求url
     * @param param          url参数
     * @param defaultCharset 字符编码
     * @return 请求返回的内容
     */
    public static String doGet(String url, HashMap<String, String> param, Charset defaultCharset) {
        // 创建Httpclient对象
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // 返回结果
        String resultString = "";
        // 执行url之后的响应
        CloseableHttpResponse response = null;
        try {
            // 创建uri
            URIBuilder builder = new URIBuilder(url);

            // 将参数封装到uri里面
            if (param != null) {
                for (String key : param.keySet()) {
                    builder.addParameter(key, param.get(key));
                }
            }
            URI uri = builder.build();
            // 创建HTTP GET请求
            HttpGet httpGet = new HttpGet(uri);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:62.0) Gecko/20100101 Firefox/62.0");
            // 执行请求
            response = httpclient.execute(httpGet);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                resultString = EntityUtils.toString(response.getEntity(), defaultCharset);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpclient.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return resultString;
    }

    public static String doGet(String url, HashMap<String, String> param) {
        return doGet(url, param, Charset.defaultCharset());
    }

    /**
     * 使用 Java.net.URLConnection
     *
     * @param url         发送请求的 URL
     * @param param       请求参数, 请求参数应该是 name1=value1&name2=value2 的形式
     * @param charsetName 编码类型
     */
    public static void sendGet(String url, String param, String charsetName) {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            log.info("sendGet - {}", urlNameString);
            URL realUrl = new URL(urlNameString);
            URLConnection connection = realUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.connect();
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), charsetName));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            log.info("rev - {}", result);
        } catch (Exception e) {
            log.error("sendGet error, url={}, param={}: {}", url, param, e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ex) {
                log.error("sendGet close, url={}, param={}: {}", url, param, ex.getMessage());
            }
        }
    }


    /**
     * @param url         请求 url
     * @param headers     Map<String, String> 请求头
     * @param requestBody { json str }  请求体以 JSON 格式发送, 请求头部为 {@code Content-Type : application/json}
     *                    发送表单数据 : {@code Content-Type : application/x-www-form-urlencoded} {@link UrlEncodedFormEntity} {@link BasicNameValuePair}
     */
    public static void sendPost(String url, Map<String, String> headers, String requestBody) {
        Assert.notNull(url, "url must not be null");
        Assert.isTrue(isValidJson(requestBody), "requestBody must be valid json");
        String charset = "UTF-8";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            setHttpPost(httpPost, headers, requestBody, charset);
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            log.info("Response Code : {}", statusCode);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String responseContent = EntityUtils.toString(entity);
                log.info("Response Content : {}", responseContent);
            }
        } catch (IOException e) {
            log.error("{}", e.getCause().getMessage());
        }
    }

    private static void setHttpPost(HttpPost httpPost, Map<String, String> formKeyValue) {
        List<NameValuePair> parameters = new ArrayList<>();
        httpPost.addHeader(Header.CONTENT_TYPE.getValue(), ContentType.FORM_URLENCODED.getValue());
        if (formKeyValue != null && !formKeyValue.isEmpty()) {
            formKeyValue.forEach((key, value) -> {
                BasicNameValuePair basicNameValuePair = new BasicNameValuePair(key, value);
                parameters.add(basicNameValuePair);
            });
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        entity.setContentType(ContentType.FORM_URLENCODED.getValue());
        httpPost.setEntity(entity);
    }


    private static void setHttpPost(HttpPost httpPost, Map<String, String> headers, String requestBody, String charset) throws UnsupportedEncodingException {
        httpPost.addHeader(Header.USER_AGENT.getValue(), "Mozilla/5.0");
        httpPost.addHeader(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue());
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                String headerValue = entry.getValue();
                httpPost.setHeader(headerName, headerValue);
            }
        }
        StringEntity stringEntity = new StringEntity(requestBody, charset);
        stringEntity.setContentType(ContentType.JSON.getValue());
        httpPost.setEntity(stringEntity);
    }

    public static boolean isValidJson(String jsonString) {
        try {
            JsonParser parser = new JsonParser();
            parser.parse(jsonString);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    public static void main(String[] args) throws UnknownHostException, SocketException {
        log.info("Mac Address: {}", getLocalMac(Inet4Address.getLocalHost()));
    }

    /**
     * 从API获取每日推荐诗词
     *
     * @return Poem
     */
    public static Poem fetchDailyRecommendedPoem() {
        String url = "https://api.codelife.cc/todayShici?lang=cn";
        String response = doGet(url, null, Charset.defaultCharset());
        JSONObject parsedObj = JSONUtil.parseObj(response);
        Object data = parsedObj.get("data");
        return JSONUtil.toBean(JSONUtil.toJsonStr(data), Poem.class);
    }

    /**
     * 发送HTTP POST请求到指定URL(使用JDK动态代理拦截 HTTP 请求), 支持自定义超时设置
     *
     * @param url  目标URL地址
     * @param body 请求体参数
     * @return 响应体字符串
     * @throws IOException 发生网络异常、连接超时或响应解析失败时抛出
     */
    public static String sendProxyPost(String url, Map<String, Object> body) throws IOException {
        RequestConfigOptions options = RequestConfigOptions.builder()
                .connectTimeout(2000)
                .connectionRequestTimeout(3000)
                .socketTimeout(5000)
                .build();
        return sendProxyPost(url, body, options);
    }

    public static String sendProxyPost(String url, Map<String, Object> body, RequestConfigOptions options) throws IOException {
        Assert.hasLength(url, "URL不能为空");
        StringEntity stringEntity = new StringEntity(JSONUtil.parse(body).toJSONString(0), StandardCharsets.UTF_8);
        stringEntity.setContentType(ContentType.JSON.getValue());
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(stringEntity);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(options.getConnectTimeout())
                .setConnectionRequestTimeout(options.getConnectionRequestTimeout())
                .setSocketTimeout(options.getSocketTimeout())
                .build();
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                })
                .setDefaultRequestConfig(requestConfig).build();
        // 创建代理类
        HttpInvocationHandler httpInvocationHandler = new HttpInvocationHandler(httpClient);
        // 创建代理对象
        HttpClient proxyClient = (HttpClient) Proxy.newProxyInstance(
                CloseableHttpClient.class.getClassLoader(), CloseableHttpClient.class.getInterfaces(), httpInvocationHandler);
        // 执行代理方法
        HttpResponse httpResponse = proxyClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();
        return EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);
    }

    public static InputStream getInputStreamByUrl(HttpRequestEntity hre, boolean downloadFully) throws IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        String url = hre.getRequestUrl();
        try {
            // URL中文件名称带有空格(LaxRedirectStrategy不起作用或者禁用重定向手动处理空格)
            HttpClientBuilder builder = HttpClients.custom()
                    .setRedirectStrategy(new LaxRedirectStrategy());
            //.disableRedirectHandling()
            if (StringUtils.isNotEmpty(hre.getProxyHost())) {
                builder.setProxy(new HttpHost(hre.getProxyHost(), hre.getProxyPort()));
            }
            client = builder.build();
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            response = client.execute(request);
            if (downloadFully) {
                byte[] data = EntityUtils.toByteArray(response.getEntity());
                // Downloads complete and closes connection automatically
                Closeables.close(response, true);
                Closeables.close(client, true);
                return new ByteArrayInputStream(data);
            }
            return response.getEntity().getContent();
        } catch (IOException e) {
            try {
                Closeables.close(response, false);
                Closeables.close(client, false);
            } catch (IOException ignored) {
                log.error(e.getMessage(), e);
            }
            throw new IOException("Failed to get input stream from URL: " + url, e);
        }
        // Do not close response or client stream, and closed by the caller
    }

    public static InputStream getInputStreamByUrl(HttpRequestEntity hre) throws IOException {
        return getInputStreamByUrl(hre, false);
    }

    public static List<String> getHeaderList(Map<String, List<String>> headers,
                                             String key) {
        List<String> empty = new ArrayList<>(0);
        if (headers == null) {
            return empty;
        }
        for (String k : headers.keySet()) {
            if (key.equalsIgnoreCase(k)) {
                List<String> values = headers.get(k);
                if (values != null) {
                    return new ArrayList<>(values);
                } else {
                    return empty;
                }
            }
        }
        return empty;
    }


    public static boolean isGzip(Map<String, List<String>> headers) {
        return getHeaderList(headers, Header.CONTENT_ENCODING.getValue()).stream().anyMatch("gzip"::equalsIgnoreCase);
    }

    public static boolean isDeflate(Map<String, List<String>> headers) {
        return getHeaderList(headers, Header.CONTENT_ENCODING.getValue()).stream().anyMatch("deflate"::equalsIgnoreCase);
    }

    public static String apacheClientRequest(HttpRequestEntity hre) {
        String requestUrl = hre.getRequestUrl();
        String requestMethod = hre.getRequestMethod().name();
        Map<String, String> requestBody = hre.getRequestBody();
        Map<String, String> requestHeaders = hre.getRequestHeaders();
        HttpRequestBase request;
        boolean enableProxy = false;
        infoLog("请求的URL ====> {}, 请求方式 -> [{}], 请求时间戳 -> {}", requestUrl, requestMethod, hre.getEpoch());
        if (HttpGet.METHOD_NAME.equals(requestMethod)) {
            request = new HttpGet(requestUrl);
        } else if (HttpPost.METHOD_NAME.equals(requestMethod)) {
            HttpPost httpPost = new HttpPost(requestUrl);
            if (requestBody != null) {
                // 添加请求体
                String contentType = requestHeaders.getOrDefault(Header.CONTENT_TYPE.getValue(), MimeTypeUtils.APPLICATION_JSON_VALUE);
                HttpEntity httpEntity;
                if (ContentType.FORM_URLENCODED.getValue().equals(contentType)) {
                    List<BasicNameValuePair> parameters = new ArrayList<>(requestBody.size());
                    requestBody.forEach((k, v) -> parameters.add(new BasicNameValuePair(k, v)));
                    UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(parameters, hre.getCharset());
                    urlEncodedFormEntity.setContentType(contentType);
                    httpEntity = urlEncodedFormEntity;
                } else if (ContentType.MULTIPART.getValue().equals(contentType)) {
                    MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                    multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    multipartEntityBuilder.setCharset(hre.getCharset());
                    requestBody.forEach((k, v) -> multipartEntityBuilder.addPart(k, new StringBody(v, org.apache.http.entity.ContentType.TEXT_PLAIN)));
                    // Add FileBody
                    httpEntity = multipartEntityBuilder.build();
                } else {
                    Gson gson = new Gson();
                    StringEntity stringEntity = new StringEntity(gson.toJson(requestBody), hre.getCharset());
                    stringEntity.setContentType(contentType);
                    httpEntity = stringEntity;
                }
                log.info("请求体 ===> {}", formEntityToString(httpEntity));
                httpPost.setEntity(httpEntity);
            }
            request = httpPost;
        } else {
            throw new IllegalArgumentException("不支持的请求方法: " + requestMethod);
        }
        // 添加请求头
        if (requestHeaders != null) {
            log.info("请求头 ===> {}", Convert.prettyJson(requestHeaders));
            requestHeaders.forEach(request::addHeader);
        }
        RequestConfig.Builder builder = RequestConfig.custom().setConnectTimeout(hre.getConnectTimeout()).setSocketTimeout(hre.getReadTimeout());
        if (StringUtils.isNotEmpty(hre.getProxyHost())) {
            builder.setProxy(new HttpHost(hre.getProxyHost(), hre.getProxyPort()));
            enableProxy = true;
        }
        RequestConfig requestConfig = builder.build();
        if (enableProxy) {
            infoLog("已启用代理服务器 ====> {}", requestConfig.getProxy());
        }
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            CloseableHttpResponse response = httpClient.execute(request);
            StatusLine statusLine = response.getStatusLine();
            HttpEntity httpEntity = response.getEntity();
            String result = EntityUtils.toString(httpEntity, hre.getCharset());
            log.info("请求成功, 返回的状态信息为 ===> {}, 响应信息为 ===> {}", statusLine.toString(), Convert.prettyJson(result));
            return result;
        } catch (IOException e) {
            log.error("请求失败, 异常信息为 ===> {}", e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    public static String okhttpRequest(HttpRequestEntity hre) {
        Request request;
        String requestUrl = hre.getRequestUrl();
        String requestMethod = hre.getRequestMethod().name();
        Map<String, String> requestBody = hre.getRequestBody();
        Map<String, String> requestHeaders = hre.getRequestHeaders();
        infoLog("请求的URL ====> {}, 请求方式 -> [{}], 请求时间戳 -> {}", requestUrl, requestMethod, hre.getEpoch());
        Request.Builder builder = new Request.Builder().url(requestUrl);
        if (requestMethod.equals(RequestMethod.GET.name())) {
            builder.get();
        } else if (requestMethod.equals(RequestMethod.POST.name())) {
            if (requestBody != null) {
                String contentType = requestHeaders.getOrDefault(Header.CONTENT_TYPE.getValue(), MimeTypeUtils.APPLICATION_JSON_VALUE);
                MediaType mediaType = MediaType.parse(contentType);
                RequestBody body;
                if (ContentType.FORM_URLENCODED.getValue().equals(contentType)) {
                    FormBody.Builder formBuilder = new FormBody.Builder();
                    requestBody.forEach(formBuilder::add);
                    body = formBuilder.build();
                } else if (ContentType.MULTIPART.getValue().equals(contentType)) {
                    MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                    requestBody.forEach(multipartBuilder::addFormDataPart);
                    body = multipartBuilder.build();
                } else {
                    body = RequestBody.create(new JSONObject(requestBody).toString(), mediaType);
                }
                builder.post(body);
                log.info("请求体为 ====> {}", formBodyToString(body));
            }
        } else {
            throw new IllegalArgumentException("不支持的请求方法: " + requestMethod);
        }
        if (requestHeaders != null) {
            infoLog("请求头 ===> {}", Convert.prettyJson(requestHeaders));
            requestHeaders.forEach(builder::addHeader);
        }
        request = builder.build();
        OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder().readTimeout(hre.getReadTimeout(), TimeUnit.MILLISECONDS).connectTimeout(hre.getConnectTimeout(), TimeUnit.MILLISECONDS);

        if (StringUtils.isNotEmpty(hre.getProxyHost())) {
            clientBuilder.proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(hre.getProxyHost(), hre.getProxyPort())));
            infoLog("已启用代理服务器 ====> {}", clientBuilder.getProxy$okhttp());
        }
        OkHttpClient client = clientBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String result = responseBody.string();
                    log.info("请求成功, 响应信息为 ===> {}", Convert.prettyJson(result));
                    return result;
                }
            } else {
                log.error("请求失败, 状态码为 ====> {}, 消息为 ====> {}", response.code(), response.message());
            }
        } catch (Exception e) {
            log.error("请求失败, 异常信息为 ===> {}", e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }


    public static String formEntityToString(HttpEntity entity) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            entity.writeTo(out);
            String content = out.toString();
            return Convert.prettyJson(content);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return StringUtils.EMPTY;
        }
    }

    public static String formBodyToString(RequestBody formBody) {
        try {
            Buffer buffer = new Buffer();
            formBody.writeTo(buffer);
            String content = buffer.readUtf8();
            return Convert.prettyJson(content);
        } catch (IOException e) {
            return null;
        }
    }

    public static void infoLog(String msg, Object... args) {
        log.info(msg, args);
    }
}
