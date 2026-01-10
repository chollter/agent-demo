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
        log.info("MCP服务器 {} 初始化成功", serverName);

        // 发送initialized通知
        sendNotification("notifications/initialized", Map.of());
    }

    private void startResponseHandler() {
        Thread handler = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
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
                log.error("MCP响应处理器错误", e);
            }
        });
        handler.setDaemon(true);
        handler.setName("MCP-ResponseHandler-" + serverName);
        handler.start();
    }

    @SuppressWarnings("unchecked")
    private void handleResponse(Map<String, Object> message) {
        Object id = message.get("id");
        if (id instanceof Number) {
            long requestId = ((Number) id).longValue() & REQUEST_ID_MASK;
            RequestContext context = pendingRequests.remove(requestId);
            if (context != null) {
                synchronized (context.lock) {
                    if (message.containsKey("result")) {
                        context.result = String.valueOf(message.get("result"));
                    } else if (message.containsKey("error")) {
                        Map<String, Object> error = (Map<String, Object>) message.get("error");
                        context.result = "Error: " + error.get("message");
                    }
                    context.lock.notifyAll();
                }
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
            throw new IOException("请求超时");
        }

        try {
            return objectMapper.readValue(context.result, Map.class);
        } catch (Exception e) {
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
        Map<String, Object> response = sendRequest("tools/list", Map.of());
        Object result = response.get("result");
        if (result instanceof Map) {
            return (List<Map<String, Object>>) ((Map<?, ?>) result).get("tools");
        }
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
