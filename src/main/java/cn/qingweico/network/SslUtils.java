package cn.qingweico.network;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * @author zqw
 * @date 2025/7/26
 */
public class SslUtils {
    private static void trustAllHttpsCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
    static class miTM implements TrustManager, X509TrustManager {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }
    }
    /**
     * 忽略HTTPS请求的SSL证书, 必须在openConnection之前调用
     */
    public static void ignoreSsl() throws Exception{
        HostnameVerifier hv = (urlHostName, session) -> true;
        trustAllHttpsCertificates();
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }
}
