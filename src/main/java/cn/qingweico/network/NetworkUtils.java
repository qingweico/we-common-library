package cn.qingweico.network;

import cn.hutool.core.text.StrPool;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.qingweico.concurrent.pool.ThreadPoolBuilder;
import cn.qingweico.constants.Symbol;
import cn.qingweico.convert.StringConvert;
import cn.qingweico.model.HttpRequestEntity;
import cn.qingweico.model.Poem;
import cn.qingweico.model.RequestConfigOptions;
import cn.qingweico.model.enums.ConversionMethod;
import cn.qingweico.network.http.HttpInvocationHandler;
import com.google.common.io.Closeables;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.netty.channel.ChannelOption;
import jodd.util.StringPool;
import kong.unirest.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.Headers;
import okhttp3.MultipartBody;
import okio.Buffer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.transport.ProxyProvider;

import java.io.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.http.HttpRequest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * @author zqw
 * @date 2022/6/25
 */
@Slf4j
public class NetworkUtils {
    private static final int GLOBAL_CONNECT_TIMEOUT = 5;
    private static final int GLOBAL_READ_TIMEOUT = 10;
    private static final int GLOBAL_REQUEST_TIMEOUT = 30;
    private static final int CPU = Runtime.getRuntime().availableProcessors();
    private static final Dispatcher DISPATCHER = new Dispatcher(ThreadPoolBuilder.builder()
            .corePoolSize(CPU * 2)
            .maxPoolSize(CPU * 2)
            .threadPoolName("OKHTTP-dispatcher")
            .build()
    );

    private static final ConnectionPool CONNECTION_POOL = new ConnectionPool(50, 5, TimeUnit.MINUTES);


    /**
     * OKHTTP 全局客户端配置
     * 如果有统一代理,在这里配置
     * proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(host, port)))
     */
    private static final OkHttpClient OKHTTP_CLIENT = new OkHttpClient.Builder()
            .dispatcher(DISPATCHER)
            .connectionPool(CONNECTION_POOL)
            .connectTimeout(GLOBAL_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(GLOBAL_READ_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(GLOBAL_REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .build();


    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;
    private static final CloseableHttpClient APACHE_CLIENT;
    private static final RequestConfig DEFAULT_REQUEST_CONFIG;
    static {
        DISPATCHER.setMaxRequests(200);
        DISPATCHER.setMaxRequestsPerHost(50);

        CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
        CONNECTION_MANAGER.setMaxTotal(200);
        CONNECTION_MANAGER.setDefaultMaxPerRoute(50);


        DEFAULT_REQUEST_CONFIG = RequestConfig.custom()
                .setConnectTimeout(GLOBAL_CONNECT_TIMEOUT * 1000)
                .setSocketTimeout(GLOBAL_READ_TIMEOUT * 1000)
                .setConnectionRequestTimeout(GLOBAL_REQUEST_TIMEOUT * 1000)
                .build();

        /*
         * Apache 全局客户端
         * 如果有统一代理,在这里配置
         * setProxy(new HttpHost(host, port));
         */
        APACHE_CLIENT = HttpClientBuilder.create()
                .setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG)
                .setConnectionManager(CONNECTION_MANAGER)
                .evictIdleConnections(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 返回机器的硬件地址(通常是 MAC 地址)
     * Mac OS平台上使用 {@link InetAddress#getLocalHost()} 获取的是本地回环地址
     * 使用 {@link NetworkInterface#getByInetAddress(InetAddress)} 根据回环地址获取的
     * 本地回环接口(lo0)没有 Mac 地址, 需要根据不同平台去特定处理
     *
     * @return MAC 地址
     * @throws SocketException SocketException
     */
    public static String getLocalMac() throws SocketException {
        InetAddress ia = NetworkAddressResolver.getLocalAddress();
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
        log.info("Mac Address: {}", getLocalMac());
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


    private static boolean isGzip(Map<String, List<String>> headers) {
        return getHeaderList(headers, Header.CONTENT_ENCODING.getValue()).stream().anyMatch("gzip"::equalsIgnoreCase);
    }

    private static boolean isDeflate(Map<String, List<String>> headers) {
        return getHeaderList(headers, Header.CONTENT_ENCODING.getValue()).stream().anyMatch("deflate"::equalsIgnoreCase);
    }

    /**
     * 使用 Apache {@link HttpClient} 发起 HTTP 请求
     */
    private static String apacheClientRequest(HttpRequestEntity hre) {
        String requestUrl = hre.getRequestUrl();
        String httpMethod = hre.getHttpMethod().name();
        Map<String, String> requestBody = hre.getRequestBody();
        Map<String, String> requestHeaders = hre.getRequestHeaders();
        HttpRequestBase request;
        infoLog("请求的URL ====> {}, 请求方式 -> [{}], 请求时间戳 -> {}", requestUrl, httpMethod, hre.getEpoch());
        if (HttpGet.METHOD_NAME.equals(httpMethod)) {
            request = new HttpGet(requestUrl);
        } else if (HttpPost.METHOD_NAME.equals(httpMethod)) {
            HttpPost httpPost = new HttpPost(requestUrl);
            if (requestBody != null) {
                // 添加请求体
                String contentType = requestHeaders == null ? MimeTypeUtils.APPLICATION_JSON_VALUE
                        : requestHeaders.containsKey(Header.CONTENT_TYPE.getValue())
                        ? requestHeaders.remove(Header.CONTENT_TYPE.getValue()) : MimeTypeUtils.APPLICATION_JSON_VALUE;
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
                    requestBody.forEach((k, v) ->
                            multipartEntityBuilder.addPart(k, new StringBody(v, org.apache.http.entity.ContentType.TEXT_PLAIN)));
                    // Add FileBody
                    httpEntity = multipartEntityBuilder.build();
                } else {
                    StringEntity stringEntity = new StringEntity(mergeRequestBodyIfNecesaryToString(requestBody, hre.getComplexBody()), hre.getCharset());
                    stringEntity.setContentType(contentType);
                    httpEntity = stringEntity;
                }
                infoBodyLog(formEntityToString(httpEntity));
                httpPost.setEntity(httpEntity);
                httpPost.setHeader(httpEntity.getContentType());
            } else {
                HttpEntity httpEntity = new StringEntity(StrPool.EMPTY_JSON, hre.getCharset());
                httpPost.setEntity(httpEntity);
                httpPost.setHeader(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue());
            }
            request = httpPost;
        } else {
            throw new IllegalArgumentException("不支持的请求方法: " + httpMethod);
        }
        // 添加请求头
        if (requestHeaders != null) {
            requestHeaders.forEach(request::addHeader);
            requestHeaders = Arrays.stream(request.getAllHeaders())
                    .collect(Collectors.toMap(org.apache.http.Header::getName,
                            org.apache.http.Header::getValue));
            infoHeadersLog(StringConvert.prettyJson(requestHeaders));
        }
        if (StringUtils.isNotEmpty(hre.getProxyHost())) {
            return unsupportedPreRequestProxyWarring();
        }

        CloseableHttpResponse response = null;
        try {
            response = APACHE_CLIENT.execute(request);
            StatusLine statusLine = response.getStatusLine();
            HttpEntity httpEntity = response.getEntity();
            String result = EntityUtils.toString(httpEntity, hre.getCharset());
            log.info("请求成功, 返回的状态信息为 ===> {}, 响应信息为 ===> {}", statusLine.toString(), StringConvert.prettyJson(result));
            return result;
        } catch (IOException e) {
            log.error("请求失败, 异常信息为 ===> {}", e.getMessage(), e);
        } finally {
            try {
                if (response != null) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    response.close();
                }
            } catch (IOException e) {
                log.error("{}", e.getMessage(), e);
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * 使用 OKHttp {@link OkHttpClient} 发起 HTTP 请求
     */
    private static String okhttpRequest(HttpRequestEntity hre) {
        Request request;
        String requestUrl = hre.getRequestUrl();
        String httpMethod = hre.getHttpMethod().name();
        Map<String, String> requestBody = hre.getRequestBody();
        Map<String, String> requestHeaders = hre.getRequestHeaders();
        infoLog("请求的URL ====> {}, 请求方式 -> [{}], 请求时间戳 -> {}", requestUrl, httpMethod, hre.getEpoch());
        Request.Builder builder = new Request.Builder().url(requestUrl);
        if (httpMethod.equals(HttpMethod.GET.name())) {
            builder.get();
        } else if (httpMethod.equals(HttpMethod.POST.name())) {
            if (requestBody != null) {
                String contentType = requestHeaders == null ? MimeTypeUtils.APPLICATION_JSON_VALUE
                        : requestHeaders.containsKey(Header.CONTENT_TYPE.getValue())
                        ? requestHeaders.remove(Header.CONTENT_TYPE.getValue()) : MimeTypeUtils.APPLICATION_JSON_VALUE;
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
                    body = RequestBody.create(mergeRequestBodyIfNecesaryToString(requestBody, hre.getComplexBody()), mediaType);
                }
                builder.post(body);
                infoBodyLog(formBodyToString(body));
                mediaType = body.contentType();
                if (mediaType != null) {
                    builder.addHeader(Header.CONTENT_TYPE.getValue(), mediaType.toString());
                }
            } else {
                MediaType mediaType = MediaType.get(MimeTypeUtils.APPLICATION_JSON_VALUE);
                RequestBody body = RequestBody.create(StrPool.EMPTY_JSON, mediaType);
                builder.post(body);
                builder.addHeader(Header.CONTENT_TYPE.getValue(), mediaType.toString());
            }
        } else {
            throw new IllegalArgumentException("不支持的请求方法: " + httpMethod);
        }
        if (requestHeaders != null) {
            requestHeaders.forEach(builder::addHeader);
        }
        request = builder.build();
        Headers headers = request.headers();
        infoHeadersLog(StringConvert.prettyJson(headers.toMultimap()));

        if (StringUtils.isNotEmpty(hre.getProxyHost())) {
            return unsupportedPreRequestProxyWarring();
        }

        try (Response response = OKHTTP_CLIENT.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String result = responseBody.string();
                    log.info("请求成功, 响应信息为 ===> {}", StringConvert.prettyJson(result));
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

    /**
     * 使用 JDK 原生 {@link HttpURLConnection} 发起 HTTP 请求
     */
    private static String httpUrlConnectionRequest(HttpRequestEntity hre) {
        HttpURLConnection connection = null;
        try {
            String requestUrl = hre.getRequestUrl();
            String httpMethod = hre.getHttpMethod().name();
            Map<String, String> requestHeaders = hre.getRequestHeaders();
            Map<String, String> requestBody = hre.getRequestBody();
            int connectTimeout = hre.getConnectTimeout();
            int readTimeout = hre.getReadTimeout();
            infoLog("请求的URL ====> {}, 请求方式 -> [{}], 请求时间戳 -> {}", requestUrl, httpMethod, hre.getEpoch());
            URL url = new URL(requestUrl);
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
            if (StringUtils.isNotEmpty(hre.getProxyHost())) {
                java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(hre.getProxyHost(), hre.getProxyPort()));
                connection = (HttpURLConnection) url.openConnection(proxy);
                infoLog("已启用代理服务器 ====> {}", proxy.address());
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setRequestMethod(httpMethod);
            if (requestHeaders != null) {
                // 向 HTTP 请求中添加请求头
                requestHeaders.forEach(connection::addRequestProperty);
            }
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            // 手动处理重定向
            connection.setInstanceFollowRedirects(false);

            connection.setUseCaches(false);
            // 设置 HTTP 请求允许从服务器读取数据
            connection.setDoInput(true);
            if (isRequestBodyAllowedHttpMethod(httpMethod)) {
                HttpURLConnection fc = connection;
                String body = fromRequestBodyToString(requestBody, requestHeaders, hre.getComplexBody(), hre.getCharset(),
                        header -> fc.setRequestProperty(header.getName(), header.getValue()));
                infoBodyLog(StringConvert.prettyJson(body));
                byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

                boolean isGzip = isGzip(connection.getRequestProperties());
                boolean isDeflate = isDeflate(connection.getRequestProperties());

                if (!isGzip && !isDeflate) {
                    // 受限制的请求头, sun.net.http.allowRestrictedHeaders
                    connection.setRequestProperty(Header.CONTENT_LENGTH.getValue(), String.valueOf(bodyBytes.length));
                    connection.setFixedLengthStreamingMode(bodyBytes.length);
                } else {
                    // 设置 HTTP 请求的传输模式为分块传输(适用于发送大量数据或流式数据, 可以避免将所有数据缓冲在内存中)
                    connection.setChunkedStreamingMode(8196);
                }
                infoHeadersLog(StringConvert.prettyJson(connection.getRequestProperties()));
                OutputStream os = null;

                try {
                    // 设置 HTTP 请求允许发送数据到服务器(用于发送请求体)
                    connection.setDoOutput(true);
                    os = connection.getOutputStream();
                    if (isGzip) {
                        os = new GZIPOutputStream(os);
                    } else if (isDeflate) {
                        os = new InflaterOutputStream(os);
                    }
                    os.write(bodyBytes);
                    os.flush();
                } finally {
                    IOUtils.close(os);
                }
            } else {
                // 设置请求体前 getOutputStream 会设置 connecting 为 true, 避免 Already connected
                infoHeadersLog(StringConvert.prettyJson(connection.getRequestProperties()));
            }
            // 服务器返回给客户端的响应头信息
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            org.json.JSONObject responseHeaders = new org.json.JSONObject();
            // 响应头的第一行是状态行, 不是一个标准的 HTTP 头, 使用 null 作为key标识
            headerFields.forEach((key, value) -> responseHeaders.put(Objects.requireNonNullElse(key, "Status"), value));
            infoResponseHeaderLog(responseHeaders.toString(4));
            try (InputStream inputStream = connection.getInputStream();
                 FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                String response = outputStream.toString();
                log.info("请求成功, 返回的响应信息为 ===> {}", StringConvert.prettyJson(response));
                return response;
            } catch (IOException e) {
                log.error("请求发生异常 ===> {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                if (connection.getErrorStream() != null) {
                    String errResponse = IOUtils.toString(connection.getErrorStream(), StandardCharsets.UTF_8);
                    log.info("响应的异常信息为 ===> {}", StringConvert.prettyJson(errResponse));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * 使用 {@link RestTemplate} 客户端发起 HTTP 请求
     * RestTemplate 默认使用 {@link SimpleClientHttpRequestFactory}
     * 其内部使用 JDK 原生的 {@link HttpURLConnection}, TCP 连接无法复用
     * 使用 Apache {@link HttpClient} 替代
     *
     * @param hre 请求实体
     * @return 响应字符串内容
     */
    private static String restTemplateRequest(HttpRequestEntity hre) {
        String requestUrl = hre.getRequestUrl();
        HttpMethod httpMethod = hre.getHttpMethod();
        Map<String, String> requestBody = hre.getRequestBody();
        Map<String, String> requestHeaders = hre.getRequestHeaders();
        infoLog("请求的URL ====> {}, 请求方式 -> [{}], 请求时间戳 -> {}", requestUrl, httpMethod, hre.getEpoch());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(hre.getConnectTimeout());
        factory.setReadTimeout(hre.getReadTimeout());
        if (StringUtils.isNotEmpty(hre.getProxyHost())) {
            java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(hre.getProxyHost(), hre.getProxyPort()));
            factory.setProxy(proxy);
            infoLog("已启用代理服务器 ====> {}", proxy.address());
        }
        RestTemplate restTemplate = new RestTemplate(factory);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        // 设置请求头
        if (requestHeaders != null) {
            requestHeaders.forEach(headers::add);
        }
        org.springframework.http.HttpEntity<?> entity;
        if (isRequestBodyAllowedHttpMethod(httpMethod)) {
            if (requestBody != null) {
                String contentType = requestHeaders == null ? MimeTypeUtils.APPLICATION_JSON_VALUE
                        : requestHeaders.getOrDefault(Header.CONTENT_TYPE.getValue(), MimeTypeUtils.APPLICATION_JSON_VALUE);
                if (ContentType.FORM_URLENCODED.getValue().equals(contentType) ||
                        ContentType.MULTIPART.getValue().equals(contentType)) {
                    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                    requestBody.forEach(body::add);
                    entity = new org.springframework.http.HttpEntity<>(body, headers);
                } else {
                    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    entity = new org.springframework.http.HttpEntity<>(mergeRequestBodyIfNecesaryToString(requestBody, hre.getComplexBody()), headers);
                }
            } else {
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                entity = new org.springframework.http.HttpEntity<>(Symbol.EMPTY_JSON, headers);
            }
            log.info("请求实体 ===> {}", StringConvert.prettyJson(entity.toString()));
        } else {
            entity = new org.springframework.http.HttpEntity<>(headers);
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, httpMethod, entity, String.class);
            String responseBody = response.getBody();
            org.springframework.http.HttpHeaders responseHeaders = response.getHeaders();
            infoResponseHeaderLog(StringConvert.prettyJson(responseHeaders.toSingleValueMap()));
            infoResponseLog(response.getStatusCode(), StringConvert.prettyJson(responseBody));
            return responseBody;
        } catch (Exception e) {
            logError(e);
            return StringUtils.EMPTY;
        }
    }

    /**
     * 使用 Spring {@link WebClient} 客户端发起 HTTP 请求
     *
     * @param hre 请求实体
     * @return 响应字符串内容
     */
    private static String webClientRequest(HttpRequestEntity hre) {
        String requestUrl = hre.getRequestUrl();
        HttpMethod httpMethod = hre.getHttpMethod();
        infoLog("请求的URL ====> {}, 请求方式 -> [{}], 请求时间戳 -> {}", requestUrl, httpMethod, hre.getEpoch());
        Map<String, String> requestBody = hre.getRequestBody();
        Map<String, String> requestHeaders = hre.getRequestHeaders();

        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, hre.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(hre.getRequestTimeout()));
        if (StringUtils.isNotEmpty(hre.getProxyHost())) {
            httpClient = httpClient.proxy(spec -> spec.type(ProxyProvider.Proxy.HTTP)
                    .host(hre.getProxyHost())
                    .port(hre.getProxyPort()));
            infoLog("已启用代理服务器 ====> {}", hre.getProxyHost() + StringPool.COLON + hre.getProxyPort());
        }
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        WebClient.RequestBodyUriSpec requestSpec = webClient.method(httpMethod);
        requestSpec.uri(requestUrl);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (requestHeaders != null) {
            requestHeaders.forEach(headers::add);
        }

        if (isRequestBodyAllowedHttpMethod(httpMethod)) {
            String contentType = headers.getFirst(Header.CONTENT_TYPE.getValue());
            if (contentType == null) {
                contentType = MimeTypeUtils.APPLICATION_JSON_VALUE;
            }
            org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.parseMediaType(contentType);
            headers.setContentType(mediaType);
            if (ContentType.FORM_URLENCODED.getValue().equals(contentType)) {
                LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                if (requestBody != null) {
                    requestBody.forEach(formData::add);
                }
                infoBodyLog(StringConvert.prettyJson(formData.toSingleValueMap()));
                requestSpec.body(BodyInserters.fromFormData(formData));
            } else if (ContentType.MULTIPART.getValue().equals(contentType)) {
                LinkedMultiValueMap<String, Object> multipartData = new LinkedMultiValueMap<>();
                if (requestBody != null) {
                    requestBody.forEach(multipartData::add);
                }
                infoBodyLog(StringConvert.prettyJson(multipartData.toSingleValueMap()));
                requestSpec.body(BodyInserters.fromMultipartData(multipartData));
            } else {
                String body = requestBody == null ? Symbol.EMPTY_JSON
                        : mergeRequestBodyIfNecesaryToString(requestBody, hre.getComplexBody());
                infoBodyLog(StringConvert.prettyJson(body));
                requestSpec.body(BodyInserters.fromValue(body));
            }
        } else {
            infoBodyLog(StringConvert.prettyJson(Symbol.EMPTY));
        }
        requestSpec.headers(httpHeaders -> httpHeaders.addAll(headers));
        infoHeadersLog(StringConvert.prettyJson(headers.toSingleValueMap()));

        try {
            ResponseEntity<String> response = requestSpec.retrieve().toEntity(String.class).block();
            if (response != null) {
                infoResponseHeaderLog(StringConvert.prettyJson(response.getHeaders().toSingleValueMap()));
                infoResponseLog(response.getStatusCode(), StringConvert.prettyJson(response.getBody()));
                return response.getBody();
            }
        } catch (WebClientResponseException e) {
            org.springframework.http.HttpHeaders responseHeaders = e.getHeaders();
            infoResponseHeaderLog(StringConvert.prettyJson(responseHeaders.toSingleValueMap()));
            log.error("请求失败, 状态码为 ===> {}, 响应体 ===> {}", e.getRawStatusCode(),
                    StringConvert.prettyJson(e.getResponseBodyAsString()), e);
        } catch (Exception e) {
            logError(e);
        }
        return StringUtils.EMPTY;
    }

    /**
     * 使用 JDK 11 {@link java.net.http.HttpClient} 发起 HTTP 请求
     */
    private static String httpClientRequest(HttpRequestEntity hre) {
        URI uri = URI.create(hre.getRequestUrl());
        String httpMethod = hre.getHttpMethod().name();
        Map<String, String> requestBody = hre.getRequestBody();
        Map<String, String> requestHeaders = hre.getRequestHeaders();
        infoLog("请求的URL ====> {}, 请求方式 -> [{}], 请求时间戳 -> {}", uri, httpMethod, hre.getEpoch());
        java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder()
                // 用于控制建立连接的时间
                .connectTimeout(Duration.of(hre.getConnectTimeout(), ChronoUnit.MILLIS));
        if (StringUtils.isNotEmpty(hre.getProxyHost())) {
            ProxySelector proxySelector = ProxySelector.of(new InetSocketAddress(hre.getProxyHost(), hre.getProxyPort()));
            builder.proxy(proxySelector);
            infoLog("已启用代理服务器 ====> {}", proxySelector.select(uri));
        }
        java.net.http.HttpClient client = builder.build();
        java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                // 用于控制整个请求的响应时间, 如果在指定的时间内没有收到响应, 请求将失败
                .timeout(Duration.of(hre.getRequestTimeout(), ChronoUnit.MILLIS))
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .uri(uri);
        if (requestHeaders != null) {
            requestHeaders.forEach(requestBuilder::setHeader);
        }
        java.net.http.HttpRequest.BodyPublisher bodyPublisher;
        if (isRequestBodyAllowedHttpMethod(httpMethod)) {
            String body = fromRequestBodyToString(requestBody, requestHeaders, hre.getComplexBody(), hre.getCharset(),
                    (header) -> requestBuilder.setHeader(header.getName(), header.getValue()));
            infoBodyLog(StringConvert.prettyJson(body));
            bodyPublisher = java.net.http.HttpRequest.BodyPublishers.ofString(body);
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }
        requestBuilder.method(httpMethod, bodyPublisher);
        java.net.http.HttpRequest request = requestBuilder.build();
        infoHeadersLog(StringConvert.prettyJson(request.headers().map()));
        try {
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            infoResponseHeaderLog(StringConvert.prettyJson(response.headers().map()));
            infoResponseLog(response.statusCode(), StringConvert.prettyJson(responseBody));
            return responseBody;
        } catch (Exception e) {
            logError(e);
        }
        return StringUtils.EMPTY;
    }

    /**
     * 使用 unirest {@link kong.unirest.Unirest} 发起 HTTP 请求
     */
    private static String unirestRequest(HttpRequestEntity hre) {
        String requestUrl = hre.getRequestUrl();
        String httpMethod = hre.getHttpMethod().name();
        infoLog("请求的URL ====> {}, 请求方式 -> [{}], 请求时间戳 -> {}", requestUrl, httpMethod, hre.getEpoch());
        Config config = Unirest.config();
        if (StringUtils.isNotEmpty(hre.getProxyHost())) {
            kong.unirest.Proxy proxy = new kong.unirest.Proxy(hre.getProxyHost(), hre.getProxyPort());
            config.proxy(proxy);
            infoLog("已启用代理服务器 ====> {}", proxy.getHost() + StringPool.COLON + proxy.getPort());
        }
        config.socketTimeout(hre.getRequestTimeout());
        config.connectTimeout(hre.getConnectTimeout());
        Map<String, String> requestHeaders = hre.getRequestHeaders();
        HttpRequestWithBody httpRequest = Unirest.request(httpMethod, requestUrl);
        Map<String, String> requestBody = hre.getRequestBody();
        kong.unirest.HttpRequest<?> hr;
        if (isRequestBodyAllowedHttpMethod(httpMethod)) {
            if (requestBody != null) {
                String contentType = requestHeaders == null
                        ? MimeTypeUtils.APPLICATION_JSON_VALUE : requestHeaders.containsKey(Header.CONTENT_TYPE.getValue())
                        ? requestHeaders.remove(Header.CONTENT_TYPE.getValue()) : MimeTypeUtils.APPLICATION_JSON_VALUE;
                if (ContentType.FORM_URLENCODED.getValue().equals(contentType)) {
                    Map<String, Object> parameters = new HashMap<>(requestBody);
                    hr = httpRequest.fields(parameters);
                } else if (ContentType.MULTIPART.getValue().equals(contentType)) {
                    kong.unirest.MultipartBody multipartBody = httpRequest.multiPartContent();
                    requestBody.forEach(multipartBody::field);
                    hr = multipartBody;
                } else {
                    String body = mergeRequestBodyIfNecesaryToString(requestBody, hre.getComplexBody());
                    httpRequest.header(Header.CONTENT_TYPE.getValue(), contentType);
                    hr = httpRequest.body(body);
                }
            } else {
                httpRequest.header(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue());
                hr = httpRequest.body(Symbol.EMPTY_JSON);
            }
            infoBodyLog(unirestBodyToString(hr));
        } else {
            hr = httpRequest;
        }
        if (requestHeaders != null) {
            // 修复了 3.7.04 unirest-java 中 no multipart boundary was found 问题
            hr.headers(requestHeaders);
        }
        infoHeadersLog(fromHeadersToString(httpRequest.getHeaders().all()));
        try {
            kong.unirest.HttpResponse<String> httpResponse = hr.asString();
            String responseBody = httpResponse.getBody();
            kong.unirest.Headers responseHeaders = httpResponse.getHeaders();
            int status = httpResponse.getStatus();
            String statusText = httpResponse.getStatusText();
            infoResponseHeaderLog(fromHeadersToString(responseHeaders.all()));
            infoResponseLog(status + Symbol.WHITE_SPACE + statusText, StringConvert.prettyJson(responseBody));
            return responseBody;
        } catch (UnirestException e) {
            logError(e);
        }
        return StringUtils.EMPTY;
    }

    /**
     * @param hre    HTTP请求实体类
     * @param client 底层发起 HTTP 请求的 Client
     * @return HTTP请求返回的内容字符串, 读取失败返回空字符串
     */
    public static String httpRequest(HttpRequestEntity hre, ConversionMethod client) {
        switch (client) {
            case JDK -> {
                return httpUrlConnectionRequest(hre);
            }
            case APACHE -> {
                return apacheClientRequest(hre);
            }
            case OKHTTP -> {
                return okhttpRequest(hre);
            }
            case SPRING -> {
                return restTemplateRequest(hre);
            }
            case JDK11 -> {
                return httpClientRequest(hre);
            }
            case UNIREST -> {
                return unirestRequest(hre);
            }
            case WEBFLUX -> {
                return webClientRequest(hre);
            }
            default -> throw new IllegalArgumentException("Unsupported HTTP Client: " + client);
        }
    }

    /**
     * @see #httpRequest(HttpRequestEntity, ConversionMethod)
     */
    public static String httpRequest(HttpRequestEntity hre) {
        return httpRequest(hre, ConversionMethod.JDK);
    }


    private static String formEntityToString(HttpEntity entity) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            entity.writeTo(out);
            String content = out.toString();
            return StringConvert.prettyJson(content);
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
            return StringConvert.prettyJson(content);
        } catch (IOException e) {
            return null;
        }
    }

    private static String fromRequestBodyToString(Map<String, String> requestBody,
                                                  Map<String, String> requestHeaders,
                                                  Map<String, Object> complexBody,
                                                  Charset charset,
                                                  Consumer<org.apache.http.Header> consumer) {
        String body;
        if (requestBody != null) {
            String contentType = requestHeaders == null ? MimeTypeUtils.APPLICATION_JSON_VALUE
                    : requestHeaders.containsKey(Header.CONTENT_TYPE.getValue())
                    ? requestHeaders.remove(Header.CONTENT_TYPE.getValue()) : MimeTypeUtils.APPLICATION_JSON_VALUE;
            if (ContentType.FORM_URLENCODED.getValue().equals(contentType)) {
                body = HttpUtil.toParams(requestBody);
            } else if (ContentType.MULTIPART.getValue().equals(contentType)) {
                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                multipartEntityBuilder.setCharset(charset);
                requestBody.forEach(multipartEntityBuilder::addTextBody);
                HttpEntity httpEntity = multipartEntityBuilder.build();
                org.apache.http.Header header = httpEntity.getContentType();
                // 更新下请求头Content-Type
                consumer.accept(header);
                body = formEntityToString(httpEntity);
            } else {
                org.apache.http.Header header = new BasicHeader(Header.CONTENT_TYPE.getValue(), contentType);
                consumer.accept(header);
                body = mergeRequestBodyIfNecesaryToString(requestBody, complexBody);
            }
        } else {
            org.apache.http.Header header = new BasicHeader(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue());
            consumer.accept(header);
            body = StrPool.EMPTY_JSON;
        }
        return body;
    }

    private static String fromHeadersToString(List<kong.unirest.Header> headers) {
        if (headers == null) {
            return StringUtils.EMPTY;
        }
        Map<String, List<String>> collect = headers
                .stream()
                .collect(Collectors.groupingBy(
                        kong.unirest.Header::getName,
                        Collectors.mapping(kong.unirest.Header::getValue, Collectors.toList())
                ));


        return StringConvert.prettyJson(collect);
    }

    private static String unirestBodyToString(kong.unirest.HttpRequest<?> hr) {
        Optional<Body> optional = hr.getBody();
        Object rbs = Symbol.EMPTY;
        if (optional.isPresent()) {
            Body body = optional.get();
            if (body.isEntityBody()) {
                rbs = body.uniPart().getValue().toString();
            } else {
                rbs = body.multiParts().stream()
                        .collect(Collectors.groupingBy(
                                BodyPart::getName,
                                Collectors.mapping(BodyPart::getValue, Collectors.toList())
                        ));
            }
        }
        return StringConvert.prettyJson(rbs);
    }

    private static void infoLog(String msg, Object... args) {
        log.info(msg, args);
    }

    private static void infoHeadersLog(String headers) {
        infoLog("请求头 ===> {}", headers);
    }

    private static void infoBodyLog(String body) {
        infoLog("请求体 ===> {}", body);
    }

    private static void infoResponseHeaderLog(String responseHeader) {
        infoLog("响应头 ===> {}", responseHeader);
    }

    private static void infoResponseLog(Object status, String responseBody) {
        log.info("请求成功, 状态信息 ===> {}, 返回的响应信息为 ===> {}", status,
                StringConvert.prettyJson(responseBody));
    }
    private static void logError(Exception e) {
        log.error("请求失败, 错误信息为 ===> {}", e.getMessage(), e);
    }

    private static boolean isRequestBodyAllowedHttpMethod(HttpMethod httpMethod) {
        return HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod) ||
                HttpMethod.PATCH.equals(httpMethod) || HttpMethod.DELETE.equals(httpMethod);
    }

    private static boolean isRequestBodyAllowedHttpMethod(String httpMethod) {
        return isRequestBodyAllowedHttpMethod(HttpMethod.valueOf(httpMethod));
    }

    private static String mergeRequestBodyIfNecesaryToString(Map<String, String> requestBody,
                                                             Map<String, Object> complexBody) {
        org.json.JSONObject jo = new org.json.JSONObject(requestBody);
        if (complexBody != null) {
            complexBody.forEach(jo::put);
        }
        return jo.toString();
    }
    private static String unsupportedPreRequestProxyWarring() {
        log.warn("只支持全局代理, 可以配置全局代理或者走其他客户端");
        return StringUtils.EMPTY;
    }
}
