package cn.qingweico.network;

import cn.qingweico.convert.ByteUnitConverter;
import cn.qingweico.model.HttpRequestEntity;
import cn.qingweico.model.enums.ConversionMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * {@link NetworkUtils#httpRequest(HttpRequestEntity) 并发测试}
 * 200 并发 2000个请求
 * 使用 jconsole 记录 OldGen 占用、线程峰值以及耗时(吞吐量)
 *
 * @author zqw
 * @date 2026/01/13
 */
@Slf4j
public class NetworkUtilsTest {

    private static final int THREADS = 200;
    private static final int REQUESTS = 2000;

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        String s = scanner.nextLine();
        if("OK".equalsIgnoreCase(s)) {
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            CountDownLatch latch = new CountDownLatch(REQUESTS);

            long start = System.currentTimeMillis();

            for (int i = 0; i < REQUESTS; i++) {
                pool.submit(() -> {
                    try {
                        HttpRequestEntity hre = getRequestEntity();
                        NetworkUtils.httpRequest(hre, ConversionMethod.APACHE);
                    }catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            System.out.println("耗时(ms): " + ByteUnitConverter.convert(System.currentTimeMillis() - start));
            pool.shutdown();
        }

        log.info("测试完成");
        System.out.println("按任意键并回车退出...");
        scanner = new Scanner(System.in);
        scanner.nextLine();

    }

    private static HttpRequestEntity getRequestEntity() {
        return HttpRequestEntity.builder()
                .requestUrl("https://httpbin.org/post")
                .httpMethod(HttpMethod.POST)
                .requestBody(Map.of("a", "1", "b", "2"))
                .connectTimeout(100000)
                .readTimeout(100000)
                .requestTimeout(100000)
                .build();
    }

}
