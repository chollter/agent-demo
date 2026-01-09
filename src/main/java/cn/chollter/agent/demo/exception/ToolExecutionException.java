package cn.chollter.agent.demo.exception;

/**
 * 工具执行异常
 * 当工具执行过程中发生错误时抛出
 *
 * @author Chollter
 * @since 1.0.0
 */
public class ToolExecutionException extends AgentException {

    public ToolExecutionException(String toolName, String message) {
        super("TOOL_EXECUTION_ERROR",
                String.format("工具 '%s' 执行失败: %s", toolName, message),
                message);
    }

    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super("TOOL_EXECUTION_ERROR",
                String.format("工具 '%s' 执行失败: %s", toolName, message),
                cause);
    }

    public ToolExecutionException(String toolName, Throwable cause) {
        super("TOOL_EXECUTION_ERROR",
                String.format("工具 '%s' 执行失败", toolName),
                cause);
    }
}
