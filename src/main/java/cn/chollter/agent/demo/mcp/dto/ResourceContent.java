package cn.chollter.agent.demo.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP资源内容
 * 对应MCP协议中的资源内容响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceContent {
    /**
     * 资源的URI
     */
    private String uri;

    /**
     * 资源的MIME类型
     */
    private String mimeType;

    /**
     * 文本内容
     */
    private String text;

    /**
     * 二进制内容(base64编码)
     */
    private String blob;

    /**
     * 内容项列表(用于复杂资源)
     */
    private List<ContentItem> contents;

    /**
     * 内容项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentItem {
        private String type; // "text" | "image" | "resource"
        private String text;
        private String data; // base64编码的数据
        private String uri;
    }
}
