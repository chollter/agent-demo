package cn.chollter.agent.demo.exception;

import lombok.Getter;

/**
 * Agent 异常基类
 * 所有 Agent 相关异常的父类
 *
 * @author Chollter
 * @since 1.0.0
 */
@Getter
public class AgentException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 错误详情（用于内部日志）
     */
    private final String detail;

    public AgentException(String message) {
        super(message);
        this.errorCode = "AGENT_ERROR";
        this.detail = null;
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AGENT_ERROR";
        this.detail = cause != null ? cause.getMessage() : null;
    }

    public AgentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.detail = null;
    }

    public AgentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.detail = cause != null ? cause.getMessage() : null;
    }

    public AgentException(String errorCode, String message, String detail) {
        super(message);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
