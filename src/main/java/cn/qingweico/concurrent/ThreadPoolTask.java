package cn.qingweico.concurrent;

import cn.qingweico.concurrent.pool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 提供线程执行任务的工具类
 * 使用内部的线程池,使用后即关闭,超时时间为60s
 *
 * @author zqw
 * @date 2025/12/24
 */
@Slf4j
public class ThreadPoolTask {
    private static final ExecutorService INNER_THREAD_POOL = ThreadPoolBuilder.create();

    public static void waitForExec(int times, Runnable runnable) {
        if (times <= 0) {
            throw new IllegalArgumentException("times must > 0");
        }
        CountDownLatch latch = new CountDownLatch(times);
        for (int i = 0; i < times; i++) {
            INNER_THREAD_POOL.execute(() -> {
                try {
                    runnable.run();
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
            INNER_THREAD_POOL.shutdown();
            if (!INNER_THREAD_POOL.awaitTermination(60, TimeUnit.SECONDS)) {
                INNER_THREAD_POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


    public static void waitForAllOf(int times, Runnable runnable) {
        if (times <= 0) {
            throw new IllegalArgumentException("times must > 0");
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(runnable, INNER_THREAD_POOL);
            futures.add(future);
        }
        INNER_THREAD_POOL.shutdown();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(60, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            INNER_THREAD_POOL.shutdownNow();
            throw new RuntimeException(e);
        }
    }
}
