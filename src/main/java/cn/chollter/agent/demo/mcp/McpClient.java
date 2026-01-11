package cn.chollter.agent.demo.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP客户端
 * 通过stdio协议与MCP服务器通信
 */
@Slf4j
public class McpClient implements AutoCloseable {

    private final String serverName;
    private final Process process;
    private final BufferedReader reader;
    private final OutputStreamWriter writer;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestId = new AtomicLong(0);
    private final Map<Long, RequestContext> pendingRequests = new ConcurrentHashMap<>();

    private static final int TIMEOUT_MS = 30000;
    private static final long REQUEST_ID_MASK = 0xFFFFFFFFL;
    private volatile boolean running = true;

    private static class RequestContext {
        final long requestId;
        final long timestamp;
        String result;
        final Object lock = new Object();

        RequestContext(long requestId) {
            this.requestId = requestId;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public McpClient(String serverName, List<String> command, List<String> env) throws IOException {
        this.serverName = serverName;
        this.objectMapper = new ObjectMapper();

        log.info("启动MCP服务器: {} 命令: {}", serverName, String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        log.debug("准备启动进程，命令: {}", String.join(" ", command));
        if (env != null && !env.isEmpty()) {
            Map<String, String> environment = pb.environment();
            for (String e : env) {
                String[] parts = e.split("=", 2);
                if (parts.length == 2) {
                    environment.put(parts[0], parts[1]);
                }
            }
        }
        pb.redirectErrorStream(true);

        this.process = pb.start();
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.writer = new OutputStreamWriter(process.getOutputStream());

        // 启动响应处理线程
        startResponseHandler();

        // 初始化连接
        initialize();
    }

    private void initialize() throws IOException {
        // 发送initialize请求
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", Map.of(
            "roots", Map.of(),
            "sampling", Map.of()
        ));
        params.put("clientInfo", Map.of(
            "name", "agent-demo",
            "version", "1.0.0"
        ));

        Map<String, Object> response = sendRequest("initialize", params);
        log.info("MCP服务器 {} 初始化成功: {}", serverName, response);

        // 发送initialized通知
        sendNotification("notifications/initialized", Map.of());

        // 等待服务器处理initialized通知
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.debug("服务器 {} 已完成初始化握手", serverName);
    }

    private void startResponseHandler() {
        Thread handler = new Thread(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    String trimmedLine = line.trim();
                    if (!trimmedLine.startsWith("{")) {
                        log.info("跳过 MCP 服务端非 JSON 输出: {}", line);
                        continue;
                    }
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = objectMapper.readValue(line, Map.class);
                        handleResponse(message);
                    } catch (Exception e) {
                        log.error("处理MCP响应失败: {}", line, e);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    log.error("MCP响应处理器错误", e);
                } else {
                    log.debug("MCP响应处理器正常关闭");
                }
            }
        });
        handler.setDaemon(true);
        handler.setName("MCP-ResponseHandler-" + serverName);
        handler.start();
    }

    @SuppressWarnings("unchecked")
    private void handleResponse(Map<String, Object> message) {
        Object id = message.get("id");
        log.debug("收到消息: id={}, keys={}", id, message.keySet());

        if (id instanceof Number) {
            long requestId = ((Number) id).longValue() & REQUEST_ID_MASK;
            RequestContext context = pendingRequests.remove(requestId);
            if (context != null) {
                synchronized (context.lock) {
                    if (message.containsKey("result")) {
                        try {
                            Object resultObj = message.get("result");
                            log.debug("序列化结果: type={}, value={}", resultObj.getClass(), resultObj);
                            context.result = objectMapper.writeValueAsString(resultObj);
                        } catch (Exception e) {
                            log.error("序列化结果失败", e);
                            context.result = "{\"error\": \"Failed to serialize result\"}";
                        }
                    } else if (message.containsKey("error")) {
                        Map<String, Object> error = (Map<String, Object>) message.get("error");
                        context.result = "{\"error\": \"" + error.get("message") + "\"}";
                    }
                    context.lock.notifyAll();
                }
            } else {
                log.warn("未找到对应的请求上下文: id={}", requestId);
            }
        }
    }

    private Map<String, Object> sendRequest(String method, Map<String, Object> params) throws IOException {
        long id = requestId.incrementAndGet() & REQUEST_ID_MASK;
        RequestContext context = new RequestContext(id);
        pendingRequests.put(id, context);

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        if (params != null) {
            request.put("params", params);
        }

        String json = objectMapper.writeValueAsString(request);
        log.debug("发送请求: {}", json);
        synchronized (writer) {
            writer.write(json);
            writer.write("\n");
            writer.flush();
        }

        // 等待响应
        synchronized (context.lock) {
            try {
                context.lock.wait(TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("请求被中断", e);
            }
        }

        if (context.result == null) {
            log.error("请求超时: method={}, id={}", method, id);
            throw new IOException("请求超时");
        }

        log.debug("收到响应: method={}, result={}", method, context.result);

        try {
            Map<String, Object> fullResponse = objectMapper.readValue(context.result, Map.class);
            log.debug("解析后的响应: method={}, response={}", method, fullResponse);

            // 返回 result 字段，如果不存在则返回整个响应
            Object result = fullResponse.get("result");
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }

            // 如果没有 result 字段或不是 Map 类型，返回整个响应
            return fullResponse;
        } catch (Exception e) {
            log.error("解析响应失败: method={}, result={}, error={}", method, context.result, e.getMessage());
            return Map.of("result", context.result);
        }
    }

    private void sendNotification(String method, Map<String, Object> params) throws IOException {
        Map<String, Object> notification = new HashMap<>();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);
        if (params != null) {
            notification.put("params", params);
        }

        String json = objectMapper.writeValueAsString(notification);
        synchronized (writer) {
            writer.write(json);
            writer.write("\n");
            writer.flush();
        }
    }

    public List<Map<String, Object>> listTools() throws IOException {
        // sendRequest 现在直接返回 result 字段，包含 tools 键
        Map<String, Object> response = sendRequest("tools/list", Map.of());
        log.debug("tools/list 响应: {}", response);

        Object tools = response.get("tools");
        if (tools instanceof List) {
            List<Map<String, Object>> toolList = (List<Map<String, Object>>) tools;
            log.info("获取到 {} 个工具", toolList.size());
            return toolList;
        }

        log.warn("无法解析工具列表，响应: {}", response);
        return Collections.emptyList();
    }

    public String callTool(String toolName, Map<String, Object> arguments) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);

        Map<String, Object> response = sendRequest("tools/call", params);
        Object result = response.get("result");

        if (result instanceof Map) {
            Map<?, ?> resultMap = (Map<?, ?>) result;
            Object content = resultMap.get("content");
            if (content instanceof List) {
                StringBuilder sb = new StringBuilder();
                for (Object item : (List<?>) content) {
                    if (item instanceof Map) {
                        Object text = ((Map<?, ?>) item).get("text");
                        if (text != null) {
                            if (!sb.isEmpty()) sb.append("\n");
                            sb.append(text);
                        }
                    }
                }
                return sb.toString();
            }
            return String.valueOf(content);
        }

        return String.valueOf(result);
    }

    public String getToolSchema(String toolName) throws IOException {
        List<Map<String, Object>> tools = listTools();
        for (Map<String, Object> tool : tools) {
            if (toolName.equals(tool.get("name"))) {
                return objectMapper.writeValueAsString(tool);
            }
        }
        return "{}";
    }

    @Override
    public void close() {
        // 先设置停止标志，让响应处理器线程优雅退出
        running = false;

        try {
            sendNotification("shutdown", Map.of());
            sendNotification("notifications/exit", Map.of());
        } catch (Exception e) {
            log.error("关闭MCP连接时出错", e);
        }

        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (process != null) process.destroy();
        } catch (IOException e) {
            log.error("关闭MCP进程时出错", e);
        }

        log.info("MCP服务器 {} 已关闭", serverName);
    }
}
