package cn.chollter.agent.demo.agent;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI Agent接口
 * 定义Agent的基本行为
 */
public interface Agent {

    /**
     * 执行任务
     *
     * @param task 用户任务描述
     * @return 执行结果
     */
    AgentResponse execute(String task);

    /**
     * 执行任务（带历史上下文）
     *
     * @param task        用户任务描述
     * @param conversationHistory 对话历史
     * @return 执行结果
     */
    AgentResponse execute(String task, List<Message> conversationHistory);

    /**
     * 流式执行任务（不带历史）
     *
     * @param task 用户任务描述
     * @return SSE 事件流
     */
    Flux<ServerSentEvent<String>> executeStream(String task);

    /**
     * 流式执行任务（带历史上下文）
     *
     * @param task 用户任务描述
     * @param conversationHistory 对话历史
     * @return SSE 事件流
     */
    Flux<ServerSentEvent<String>> executeStream(String task, List<Message> conversationHistory);

    /**
     * 获取Agent名称
     */
    String getName();

    /**
     * 获取Agent描述
     */
    String getDescription();

    /**
     * 获取Agent支持的工具
     */
    List<Tool> getTools();
}
