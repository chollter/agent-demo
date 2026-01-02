package cn.chollter.agent.demo.agent;

import java.util.Map;

/**
 * Agent工具接口
 * 定义Agent可执行的工具/函数
 */
public interface Tool {

    /**
     * 工具名称
     */
    String getName();

    /**
     * 工具描述（用于让AI理解工具用途）
     */
    String getDescription();

    /**
     * 执行工具
     *
     * @param parameters 工具参数
     * @return 执行结果
     */
    String execute(Map<String, Object> parameters);

    /**
     * 获取参数schema（JSON格式）
     */
    default String getParameterSchema() {
        return "{}";
    }
}
