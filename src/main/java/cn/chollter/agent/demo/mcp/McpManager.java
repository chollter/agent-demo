package cn.chollter.agent.demo.mcp;

import cn.chollter.agent.demo.agent.Tool;
import cn.chollter.agent.demo.config.McpConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MCP管理器
 * 管理所有MCP服务器连接和工具
 * 支持工具列表缓存
 */
@Slf4j
@Component
public class McpManager {

    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();
    private final List<Tool> mcpTools = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final McpConfig mcpConfig;

    // 工具列表缓存，10分钟过期，5分钟刷新
    private final LoadingCache<String, List<Map<String, Object>>> toolCache;

    public McpManager(McpConfig mcpConfig, ObjectMapper objectMapper) {
        this.mcpConfig = mcpConfig;
        this.objectMapper = objectMapper;

        // 初始化工具列表缓存
        this.toolCache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .refreshAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100)
            .build(this::loadToolsFromServer);

        initialize();
    }

    private void initialize() {
        if (!mcpConfig.isEnabled()) {
            log.info("MCP未启用");
            return;
        }

        log.info("初始化MCP管理器，配置了 {} 个服务器", mcpConfig.getServers().size());

        for (McpConfig.McpServer serverConfig : mcpConfig.getServers()) {
            if (!serverConfig.isEnabled()) {
                log.info("跳过已禁用的MCP服务器: {}", serverConfig.getName());
                continue;
            }

            try {
                initializeServer(serverConfig);
            } catch (Exception e) {
                log.error("初始化MCP服务器失败: {}", serverConfig.getName(), e);
            }
        }

        log.info("MCP管理器初始化完成，共加载 {} 个工具", mcpTools.size());
    }

    private void initializeServer(McpConfig.McpServer serverConfig) throws Exception {
        log.info("连接MCP服务器: {} 类型: {}", serverConfig.getName(), serverConfig.getType());

        if ("stdio".equals(serverConfig.getType())) {
            List<String> command = new ArrayList<>();
            command.add(serverConfig.getCommand());
            if (serverConfig.getArgs() != null) {
                command.addAll(serverConfig.getArgs());
            }

            McpClient client = new McpClient(
                serverConfig.getName(),
                command,
                serverConfig.getEnv()
            );

            clients.put(serverConfig.getName(), client);

            // 从缓存获取服务器提供的工具列表
            List<Map<String, Object>> tools = getToolsFromCache(serverConfig.getName(), client);
            log.info("服务器 {} 提供了 {} 个工具", serverConfig.getName(), tools.size());

            // 为每个工具创建适配器
            for (Map<String, Object> toolInfo : tools) {
                String toolName = (String) toolInfo.get("name");
                String description = (String) toolInfo.get("description");

                McpToolAdapter adapter = new McpToolAdapter(
                    serverConfig.getName(),
                    toolName,
                    description,
                    client,
                    objectMapper
                );

                mcpTools.add(adapter);
                log.info("注册MCP工具: {}:{} - {}", serverConfig.getName(), toolName, description);
            }
        } else if ("sse".equals(serverConfig.getType())) {
            log.warn("SSE类型的MCP服务器暂不支持: {}", serverConfig.getName());
        }
    }

    /**
     * 获取所有MCP工具
     */
    public List<Tool> getMcpTools() {
        return Collections.unmodifiableList(mcpTools);
    }

    /**
     * 按服务器名称获取工具
     */
    public List<Tool> getToolsByServer(String serverName) {
        return mcpTools.stream()
            .filter(tool -> tool.getName().startsWith(serverName + ":"))
            .collect(Collectors.toList());
    }

    /**
     * 获取所有已连接的服务器名称
     */
    public Set<String> getConnectedServers() {
        return clients.keySet();
    }

    /**
     * 重新加载指定服务器
     */
    public void reloadServer(String serverName) {
        log.info("重新加载MCP服务器: {}", serverName);
        // 移除旧的工具
        mcpTools.removeIf(tool -> tool.getName().startsWith(serverName + ":"));

        // 清除缓存
        toolCache.invalidate(serverName);

        // 关闭旧连接
        McpClient oldClient = clients.remove(serverName);
        if (oldClient != null) {
            oldClient.close();
        }

        // 重新初始化
        Optional<McpConfig.McpServer> serverConfig = mcpConfig.getServers().stream()
            .filter(s -> s.getName().equals(serverName))
            .findFirst();

        if (serverConfig.isPresent() && serverConfig.get().isEnabled()) {
            try {
                initializeServer(serverConfig.get());
            } catch (Exception e) {
                log.error("重新加载MCP服务器失败: {}", serverName, e);
            }
        }
    }

    // ==================== 缓存相关方法 ====================

    /**
     * 从服务器加载工具列表（用于缓存加载）
     */
    private List<Map<String, Object>> loadToolsFromServer(String serverName) {
        McpClient client = clients.get(serverName);
        if (client == null) {
            log.warn("服务器 {} 未连接", serverName);
            return Collections.emptyList();
        }

        try {
            log.debug("从服务器 {} 加载工具列表", serverName);
            return client.listTools();
        } catch (Exception e) {
            log.error("加载工具列表失败: {}", serverName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 从缓存获取工具列表，如果缓存不存在则加载
     */
    private List<Map<String, Object>> getToolsFromCache(String serverName, McpClient client) {
        return toolCache.get(serverName);
    }

    /**
     * 刷新指定服务器的工具缓存
     */
    public void refreshToolCache(String serverName) {
        log.info("刷新服务器 {} 的工具缓存", serverName);
        toolCache.refresh(serverName);
    }

    /**
     * 清除所有工具缓存
     */
    public void clearToolCache() {
        log.info("清除所有工具缓存");
        toolCache.invalidateAll();
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("toolCache", Map.of(
            "size", toolCache.estimatedSize(),
            "hitRate", toolCache.stats().hitRate(),
            "missRate", toolCache.stats().missRate()
        ));
        return stats;
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭MCP管理器");
        mcpTools.clear();

        // 清理缓存
        toolCache.invalidateAll();

        for (McpClient client : clients.values()) {
            try {
                client.close();
            } catch (Exception e) {
                log.error("关闭MCP客户端时出错", e);
            }
        }

        clients.clear();
    }
}
