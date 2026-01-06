package cn.chollter.agent.demo.mcp;

import cn.chollter.agent.demo.agent.Tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * MCP工具适配器
 * 将MCP服务器提供的工具适配为项目的Tool接口
 */
@Slf4j
@RequiredArgsConstructor
public class McpToolAdapter implements Tool {

    private final String serverName;
    private final String toolName;
    private final String description;
    private final McpClient mcpClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return serverName + ":" + toolName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            log.info("执行MCP工具: {} 参数: {}", getName(), parameters);
            String result = mcpClient.callTool(toolName, parameters);
            log.info("MCP工具执行成功: {} 结果: {}", getName(), result);
            return result;
        } catch (Exception e) {
            log.error("MCP工具执行失败: {}", getName(), e);
            return "Error: 工具执行失败 - " + e.getMessage();
        }
    }

    @Override
    public String getParameterSchema() {
        try {
            return mcpClient.getToolSchema(toolName);
        } catch (Exception e) {
            log.error("获取工具schema失败: {}", getName(), e);
            return "{}";
        }
    }
}
