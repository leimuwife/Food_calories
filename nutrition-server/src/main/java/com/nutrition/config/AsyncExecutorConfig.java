package com.nutrition.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

/**
 * 异步线程池配置类
 * 创建独立的线程池用于第三方网络请求（如微信审核接口），隔离业务线程
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncExecutorConfig {

    /**
     * 线程池核心线程数
     */
    private static final int CORE_POOL_SIZE = 3;

    /**
     * 线程池最大线程数（限制并发线程数，防止短时间大量调用微信接口触发限流）
     */
    private static final int MAX_POOL_SIZE = 5;

    /**
     * 线程空闲存活时间（秒）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 任务队列容量
     */
    private static final int QUEUE_CAPACITY = 100;

    /**
     * 自定义线程名称前缀
     */
    private static final String THREAD_NAME_PREFIX = "wx-audit-";

    /**
     * 创建用于微信审核的自定义线程池
     * 禁止使用默认ForkJoinPool处理第三方网络请求，隔离业务线程
     *
     * @return ExecutorService 线程池实例
     */
    @Bean(name = "wxAuditExecutor")
    public ExecutorService wxAuditExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, THREAD_NAME_PREFIX + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        log.info("微信审核线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);

        return executor;
    }
}