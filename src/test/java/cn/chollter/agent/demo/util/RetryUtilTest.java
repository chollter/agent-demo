package cn.chollter.agent.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RetryUtil 测试类
 *
 * @author Chollter
 * @since 1.0.0
 */
class RetryUtilTest {

    @Test
    void testSuccessfulOperation() throws Exception {
        String result = RetryUtil.executeWithRetry(() -> "success");
        assertEquals("success", result);
    }

    @Test
    void testRetryOnFailure() throws Exception {
        int[] attempts = {0};

        String result = RetryUtil.executeWithRetry(
                () -> {
                    attempts[0]++;
                    if (attempts[0] < 3) {
                        throw new RuntimeException("Failed");
                    }
                    return "success after retries";
                },
                3
        );

        assertEquals("success after retries", result);
        assertEquals(3, attempts[0]);
    }

    @Test
    void testRetryFailure() {
        int[] attempts = {0};

        Exception exception = assertThrows(Exception.class, () ->
                RetryUtil.executeWithRetry(
                        () -> {
                            attempts[0]++;
                            throw new RuntimeException("Always fails");
                        },
                        2
                )
        );

        assertEquals("Always fails", exception.getMessage());
        assertEquals(3, attempts[0]); // 初始尝试 + 2次重试
    }

    @Test
    void testCustomRetryConfig() throws Exception {
        int[] attempts = {0};

        RetryUtil.RetryConfig config = RetryUtil.RetryConfig.builder()
                .maxRetries(1)
                .initialDelayMs(100)
                .maxDelayMs(500)
                .backoffMultiplier(2.0)
                .build();

        String result = RetryUtil.executeWithRetry(
                () -> {
                    attempts[0]++;
                    if (attempts[0] < 2) {
                        throw new RuntimeException("Failed");
                    }
                    return "success";
                },
                config
        );

        assertEquals("success", result);
        assertEquals(2, attempts[0]);
    }

    @Test
    void testRunnableWithException() throws Exception {
        int[] executions = {0};

        RetryUtil.executeWithRetry(
                () -> {
                    executions[0]++;
                    if (executions[0] < 2) {
                        throw new RuntimeException("Failed");
                    }
                },
                2
        );

        assertEquals(2, executions[0]);
    }
}
