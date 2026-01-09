package cn.chollter.agent.demo.mcp;

import cn.chollter.agent.demo.agent.Tool;
import cn.chollter.agent.demo.config.McpConfig;
import cn.chollter.agent.demo.mcp.dto.McpResource;
import cn.chollter.agent.demo.mcp.dto.ResourceContent;
import cn.chollter.agent.demo.mcp.dto.ResourceTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MCP管理器
 * 管理所有MCP服务器连接、工具和资源
 * 支持工具列表缓存和资源访问
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
    // 资源列表缓存，5分钟过期，3分钟刷新
    private final LoadingCache<String, List<McpResource>> resourceCache;

    public McpManager(McpConfig mcpConfig, ObjectMapper objectMapper) {
        this.mcpConfig = mcpConfig;
        this.objectMapper = objectMapper;

        // 初始化工具列表缓存
        this.toolCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .refreshAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100)
            .build(this::loadToolsFromServer);

        // 初始化资源列表缓存
        this.resourceCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .refreshAfterWrite(3, TimeUnit.MINUTES)
            .maximumSize(100)
            .build(this::loadResourcesFromServer);

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
        resourceCache.invalidate(serverName);

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
     * 从服务器加载资源列表（用于缓存加载）
     */
    private List<McpResource> loadResourcesFromServer(String serverName) {
        McpClient client = clients.get(serverName);
        if (client == null) {
            log.warn("服务器 {} 未连接", serverName);
            return Collections.emptyList();
        }

        try {
            log.debug("从服务器 {} 加载资源列表", serverName);
            return client.listResources();
        } catch (Exception e) {
            log.error("加载资源列表失败: {}", serverName, e);
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
        stats.put("resourceCache", Map.of(
            "size", resourceCache.estimatedSize(),
            "hitRate", resourceCache.stats().hitRate(),
            "missRate", resourceCache.stats().missRate()
        ));
        return stats;
    }

    // ==================== 资源访问相关方法 ====================

    /**
     * 获取指定服务器的所有资源
     * 使用缓存提高性能
     */
    public List<McpResource> getResources(String serverName) {
        McpClient client = clients.get(serverName);
        if (client == null) {
            log.warn("服务器 {} 未连接", serverName);
            return Collections.emptyList();
        }

        return resourceCache.get(serverName);
    }

    /**
     * 获取所有服务器的资源
     */
    public Map<String, List<McpResource>> getAllResources() {
        Map<String, List<McpResource>> allResources = new HashMap<>();
        for (String serverName : clients.keySet()) {
            allResources.put(serverName, getResources(serverName));
        }
        return allResources;
    }

    /**
     * 读取指定服务器的资源内容
     */
    public ResourceContent readResource(String serverName, String uri) {
        McpClient client = clients.get(serverName);
        if (client == null) {
            throw new IllegalArgumentException("服务器未连接: " + serverName);
        }

        try {
            log.debug("读取资源: {} from {}", uri, serverName);
            return client.readResource(uri);
        } catch (Exception e) {
            log.error("读取资源失败: {} from {}", uri, serverName, e);
            throw new RuntimeException("读取资源失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取指定服务器的资源模板
     */
    public List<ResourceTemplate> getResourceTemplates(String serverName) {
        McpClient client = clients.get(serverName);
        if (client == null) {
            log.warn("服务器 {} 未连接", serverName);
            return Collections.emptyList();
        }

        try {
            return client.listResourceTemplates();
        } catch (Exception e) {
            log.error("获取资源模板失败: {}", serverName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 使用资源模板读取资源
     */
    public ResourceContent readResourceTemplate(String serverName, String uriTemplate, Map<String, Object> arguments) {
        McpClient client = clients.get(serverName);
        if (client == null) {
            throw new IllegalArgumentException("服务器未连接: " + serverName);
        }

        try {
            log.debug("使用模板读取资源: {} from {}", uriTemplate, serverName);
            return client.readResourceTemplate(uriTemplate, arguments);
        } catch (Exception e) {
            log.error("使用模板读取资源失败: {} from {}", uriTemplate, serverName, e);
            throw new RuntimeException("读取资源失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新指定服务器的资源缓存
     */
    public void refreshResourceCache(String serverName) {
        log.info("刷新服务器 {} 的资源缓存", serverName);
        resourceCache.refresh(serverName);
    }

    /**
     * 清除所有资源缓存
     */
    public void clearResourceCache() {
        log.info("清除所有资源缓存");
        resourceCache.invalidateAll();
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭MCP管理器");
        mcpTools.clear();

        // 清理缓存
        toolCache.invalidateAll();
        resourceCache.invalidateAll();

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
