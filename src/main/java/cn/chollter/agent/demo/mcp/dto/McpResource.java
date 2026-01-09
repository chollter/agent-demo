package cn.chollter.agent.demo.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP资源元数据
 * 对应MCP协议中的资源对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpResource {
    /**
     * 资源的唯一标识符(URI)
     */
    private String uri;

    /**
     * 资源名称
     */
    private String name;

    /**
     * 资源描述
     */
    private String description;

    /**
     * 资源的MIME类型
     */
    private String mimeType;

    /**
     * 资源的其他元数据
     */
    private Map<String, Object> metadata;
}
