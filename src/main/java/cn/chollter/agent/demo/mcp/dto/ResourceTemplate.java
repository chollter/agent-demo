package cn.chollter.agent.demo.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP资源模板
 * 用于描述可参数化的资源URI模板
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceTemplate {
    /**
     * 模板的唯一标识符(URI模板)
     */
    private String uriTemplate;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板的MIME类型
     */
    private String mimeType;

    /**
     * 模板参数定义
     */
    private Map<String, Object> parameters;
}
