package cn.chollter.agent.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP配置类
 * 从application.yml中读取MCP服务器配置
 */
@Configuration
@ConfigurationProperties(prefix = "mcp")
@Data
public class McpConfig {

    /**
     * 是否启用MCP
     */
    private boolean enabled = true;

    /**
     * MCP服务器列表
     */
    private List<McpServer> servers = new ArrayList<>();

    @Data
    public static class McpServer {
        /**
         * 服务器名称（唯一标识）
         */
        private String name;

        /**
         * 服务器类型：stdio 或 sse
         */
        private String type = "stdio";

        /**
         * 服务器命令（仅stdio类型）
         * 例如：npx -y @modelcontextprotocol/server-filesystem
         */
        private String command;

        /**
         * 服务器参数（仅stdio类型）
         */
        private List<String> args = new ArrayList<>();

        /**
         * 环境变量
         */
        private List<String> env = new ArrayList<>();

        /**
         * 服务器URL（仅SSE类型）
         */
        private String url;

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }
}
