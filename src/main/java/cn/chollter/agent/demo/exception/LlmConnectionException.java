package cn.chollter.agent.demo.exception;

/**
 * LLM 连接异常
 * 当与 LLM 服务通信失败时抛出
 *
 * @author Chollter
 * @since 1.0.0
 */
public class LlmConnectionException extends AgentException {

    public LlmConnectionException(String message) {
        super("LLM_CONNECTION_ERROR", message);
    }

    public LlmConnectionException(String provider, Throwable cause) {
        super("LLM_CONNECTION_ERROR",
                String.format("连接到 LLM 提供商 '%s' 失败", provider),
                cause);
    }
}
