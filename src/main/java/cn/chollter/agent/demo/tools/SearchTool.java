package cn.chollter.agent.demo.tools;

import cn.chollter.agent.demo.agent.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 搜索工具示例（模拟）
 */
@Component
public class SearchTool implements Tool {

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getDescription() {
        return """
            搜索相关信息。
            参数：query (搜索查询)
            返回：搜索结果摘要
            """;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String query = getString(parameters, "query");

        if (query == null || query.isEmpty()) {
            return "Error: query parameter is required";
        }

        // 模拟搜索结果（实际应用中应该调用真实的搜索API）
        return String.format("[搜索结果] 关于'%s'的相关信息：这是模拟的搜索结果。" +
            "在实际应用中，这里应该连接到真实的搜索引擎或知识库。", query);
    }

    private String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : "";
    }
}
