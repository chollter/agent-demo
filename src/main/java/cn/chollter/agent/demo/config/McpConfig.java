package cn.chollter.agent.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * 编排策略：priority(优先级), load-balance(负载均衡), round-robin(轮询)
     */
    private OrchestrationStrategy orchestrationStrategy = OrchestrationStrategy.PRIORITY;

    /**
     * MCP服务器列表
     */
    private List<McpServer> servers = new ArrayList<>();

    /**
     * 编排策略枚举
     */
    public enum OrchestrationStrategy {
        /**
         * 优先级策略：按服务器优先级选择工具
         */
        PRIORITY,

        /**
         * 轮询策略：依次循环选择服务器
         */
        ROUND_ROBIN,

        /**
         * 并行策略：同时尝试所有服务器，使用第一个成功的响应
         */
        PARALLEL
    }

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

        /**
         * 优先级（数字越小优先级越高，默认100）
         * 用于优先级编排策略
         */
        private int priority = 100;

        /**
         * 权重（用于负载均衡，默认1）
         * 权重越高，被选中的概率越大
         */
        private int weight = 1;

        /**
         * 标签（用于工具分组和分类）
         * 例如：["file", "filesystem"] 或 ["search", "web"]
         */
        private Set<String> tags = new HashSet<>();

        /**
         * 是否为备用服务器（降级使用）
         */
        private boolean fallback = false;

        /**
         * 最大并发连接数
         */
        private int maxConnections = 10;

        /**
         * 连接超时时间（秒）
         */
        private int connectTimeoutSeconds = 10;
    }
}
