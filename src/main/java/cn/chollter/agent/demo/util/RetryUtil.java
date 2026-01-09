package cn.chollter.agent.demo.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 重试工具类
 * 提供带指数退避的重试机制
 *
 * @author Chollter
 * @since 1.0.0
 */
@Slf4j
public class RetryUtil {

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 默认初始延迟（毫秒）
     */
    private static final long DEFAULT_INITIAL_DELAY_MS = 1000;

    /**
     * 默认最大延迟（毫秒）
     */
    private static final long DEFAULT_MAX_DELAY_MS = 10000;

    /**
     * 默认退避倍数
     */
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

    /**
     * 可能抛出异常的 Supplier
     */
    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }

    /**
     * 可能抛出异常的 Runnable
     */
    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }

    /**
     * 执行带重试的操作
     *
     * @param operation 要执行的操作
     * @param <T>       返回类型
     * @return 操作结果
     * @throws Exception 当所有重试都失败时抛出最后一次的异常
     */
    public static <T> T executeWithRetry(SupplierWithException<T> operation) throws Exception {
        return executeWithRetry(operation, DEFAULT_MAX_RETRIES);
    }

    /**
     * 执行带重试的操作
     *
     * @param operation  要执行的操作
     * @param maxRetries 最大重试次数
     * @param <T>        返回类型
     * @return 操作结果
     * @throws Exception 当所有重试都失败时抛出最后一次的异常
     */
    public static <T> T executeWithRetry(SupplierWithException<T> operation, int maxRetries) throws Exception {
        return executeWithRetry(operation, maxRetries, DEFAULT_INITIAL_DELAY_MS, DEFAULT_MAX_DELAY_MS);
    }

    /**
     * 执行带重试的操作（带自定义延迟）
     *
     * @param operation        要执行的操作
     * @param maxRetries       最大重试次数
     * @param initialDelayMs   初始延迟（毫秒）
     * @param maxDelayMs       最大延迟（毫秒）
     * @param <T>              返回类型
     * @return 操作结果
     * @throws Exception 当所有重试都失败时抛出最后一次的异常
     */
    public static <T> T executeWithRetry(
            SupplierWithException<T> operation,
            int maxRetries,
            long initialDelayMs,
            long maxDelayMs
    ) throws Exception {
        return executeWithRetry(operation, maxRetries, initialDelayMs, maxDelayMs, DEFAULT_BACKOFF_MULTIPLIER);
    }

    /**
     * 执行带重试的操作（完整配置）
     *
     * @param operation         要执行的操作
     * @param maxRetries        最大重试次数
     * @param initialDelayMs    初始延迟（毫秒）
     * @param maxDelayMs        最大延迟（毫秒）
     * @param backoffMultiplier 退避倍数
     * @param <T>               返回类型
     * @return 操作结果
     * @throws Exception 当所有重试都失败时抛出最后一次的异常
     */
    public static <T> T executeWithRetry(
            SupplierWithException<T> operation,
            int maxRetries,
            long initialDelayMs,
            long maxDelayMs,
            double backoffMultiplier
    ) throws Exception {
        Exception lastException = null;
        long currentDelayMs = initialDelayMs;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("重试第 {} 次，延迟 {} ms", attempt, currentDelayMs);
                    Thread.sleep(currentDelayMs);
                    // 计算下一次延迟（指数退避）
                    currentDelayMs = (long) Math.min(currentDelayMs * backoffMultiplier, maxDelayMs);
                }

                T result = operation.get();
                if (attempt > 0) {
                    log.info("重试成功，共尝试 {} 次", attempt + 1);
                }
                return result;

            } catch (Exception e) {
                lastException = e;
                log.warn("操作失败（尝试 {}/{}）: {}",
                        attempt + 1, maxRetries + 1, e.getMessage());

                // 如果是最后一次尝试，不再重试
                if (attempt >= maxRetries) {
                    break;
                }
            }
        }

        log.error("所有重试都失败，共尝试 {} 次", maxRetries + 1);
        throw lastException;
    }

    /**
     * 执行带重试的操作（Runnable 版本）
     *
     * @param operation 要执行的操作
     * @throws Exception 当所有重试都失败时抛出最后一次的异常
     */
    public static void executeWithRetry(RunnableWithException operation) throws Exception {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, DEFAULT_MAX_RETRIES);
    }

    /**
     * 执行带重试的操作（Runnable 版本，带自定义重试次数）
     *
     * @param operation  要执行的操作
     * @param maxRetries 最大重试次数
     * @throws Exception 当所有重试都失败时抛出最后一次的异常
     */
    public static void executeWithRetry(RunnableWithException operation, int maxRetries) throws Exception {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, maxRetries, DEFAULT_INITIAL_DELAY_MS, DEFAULT_MAX_DELAY_MS);
    }

    /**
     * 重试配置类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RetryConfig {
        @lombok.Builder.Default
        private int maxRetries = DEFAULT_MAX_RETRIES;
        @lombok.Builder.Default
        private long initialDelayMs = DEFAULT_INITIAL_DELAY_MS;
        @lombok.Builder.Default
        private long maxDelayMs = DEFAULT_MAX_DELAY_MS;
        @lombok.Builder.Default
        private double backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
    }

    /**
     * 使用配置对象执行重试
     *
     * @param operation 要执行的操作
     * @param config    重试配置
     * @param <T>       返回类型
     * @return 操作结果
     * @throws Exception 当所有重试都失败时抛出最后一次的异常
     */
    public static <T> T executeWithRetry(SupplierWithException<T> operation, RetryConfig config) throws Exception {
        return executeWithRetry(
                operation,
                config.getMaxRetries(),
                config.getInitialDelayMs(),
                config.getMaxDelayMs(),
                config.getBackoffMultiplier()
        );
    }
}
