package cn.chollter.agent.demo.exception;

/**
 * 可重试异常
 * 标记此类异常应该触发重试机制
 *
 * @author Chollter
 * @since 1.0.0
 */
public class RetryableException extends AgentException {

    private final int maxRetries;
    private final int currentAttempt;

    public RetryableException(String message, int maxRetries, int currentAttempt) {
        super("RETRYABLE_ERROR", message);
        this.maxRetries = maxRetries;
        this.currentAttempt = currentAttempt;
    }

    public RetryableException(String message, Throwable cause, int maxRetries, int currentAttempt) {
        super("RETRYABLE_ERROR", message, cause);
        this.maxRetries = maxRetries;
        this.currentAttempt = currentAttempt;
    }

    /**
     * 是否还可以重试
     */
    public boolean canRetry() {
        return currentAttempt < maxRetries;
    }

    /**
     * 获取下一次尝试的次数
     */
    public int getNextAttempt() {
        return currentAttempt + 1;
    }
}
