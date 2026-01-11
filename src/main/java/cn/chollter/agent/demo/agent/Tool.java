package cn.chollter.agent.demo.agent;

import java.util.Map;
import java.time.Duration;

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

    /**
     * 是否可以缓存此工具的结果
     * 对于查询类工具（如天气、搜索），返回 true 可以避免重复调用
     *
     * @return true 表示结果可缓存
     */
    default boolean isCacheable() {
        return false;
    }

    /**
     * 获取缓存过期时间
     * 只在 isCacheable() 返回 true 时生效
     *
     * @return 缓存过期时间，默认 5 分钟
     */
    default Duration getCacheTtl() {
        return Duration.ofMinutes(5);
    }

    /**
     * 获取工具执行超时时间
     * 工具执行超过此时间将被中断并抛出 ToolTimeoutException
     *
     * @return 超时时间，默认 null 表示使用全局配置
     */
    default Duration getTimeout() {
        return null;  // 使用全局配置
    }
}
