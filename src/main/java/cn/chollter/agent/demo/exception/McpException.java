package cn.chollter.agent.demo.exception;

/**
 * MCP (Model Context Protocol) 相关异常
 * 当 MCP 服务器通信或操作失败时抛出
 *
 * @author Chollter
 * @since 1.0.0
 */
public class McpException extends AgentException {

    public McpException(String message) {
        super("MCP_ERROR", message);
    }

    public McpException(String message, Throwable cause) {
        super("MCP_ERROR", message, cause);
    }

    public McpException(String serverName, String message) {
        super("MCP_ERROR",
                String.format("MCP 服务器 '%s' 错误: %s", serverName, message),
                message);
    }

    public McpException(String serverName, String message, Throwable cause) {
        super("MCP_ERROR",
                String.format("MCP 服务器 '%s' 错误: %s", serverName, message),
                cause);
    }
}
