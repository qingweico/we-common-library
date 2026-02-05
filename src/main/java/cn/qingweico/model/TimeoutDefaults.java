package cn.qingweico.model;

/**
 * @author zqw
 * @date 2026/2/5
 */
public class TimeoutDefaults {
    /**TCP建立连接超时时间*/
    public static final int CONNECT_TIMEOUT_MS = 5000;
    /**等待服务器返回数据的超时(读超时)*/
    public static final int SOCKET_TIMEOUT_MS  = 10000;
    /**从连接池获取连接的等待时间*/
    public static final int CONNECTION_REQUEST_TIMEOUT_MS = 30000;
    /**整体请求超时时间(整个 HTTP Call 的总耗时上限)*/
    public static final int CALL_TIMEOUT_MS = 30000;
}
