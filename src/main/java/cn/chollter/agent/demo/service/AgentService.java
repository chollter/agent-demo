package cn.chollter.agent.demo.service;

import cn.chollter.agent.demo.agent.AgentResponse;
import cn.chollter.agent.demo.agent.Message;
import cn.chollter.agent.demo.core.ReActAgent;
import cn.chollter.agent.demo.entity.Execution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 服务
 * 提供任务执行功能，并集成持久化
 *
 * @author Chollter
 * @since 1.0.0
 */
@Slf4j
@Service
public class AgentService {

    private final ReActAgent agent;
    private final ExecutionService executionService;

    @Value("${agent.model.provider:openai}")
    private String modelProvider;

    public AgentService(
            @Qualifier("reActAgent") ReActAgent agent,
            ExecutionService executionService
    ) {
        this.agent = agent;
        this.executionService = executionService;
    }

    /**
     * 执行任务（不带会话ID，创建新会话）
     */
    public AgentResponse executeTask(String task) {
        return executeTask(null, task);
    }

    /**
     * 执行任务（带会话ID，用于多轮对话）
     */
    public AgentResponse executeTask(String conversationId, String task) {
        log.info("执行任务: {}, 会话ID: {}", task, conversationId);

        // 创建执行记录
        Execution execution = executionService.createExecution(conversationId, task);
        String executionId = execution.getExecutionId();

        long startTime = System.currentTimeMillis();

        try {
            // 执行任务
            AgentResponse response = agent.execute(task);

            long duration = System.currentTimeMillis() - startTime;

            if (response.isSuccess()) {
                log.info("任务执行成功，答案: {}", response.getFinalAnswer());

                // 保存成功结果
                executionService.completeExecution(
                        executionId,
                        response.getFinalAnswer(),
                        response.getThoughtSteps(),
                        response.getThoughtSteps() != null ? response.getThoughtSteps().size() : 0,
                        duration
                );
            } else {
                log.error("任务执行失败: {}", response.getErrorMessage());

                // 保存失败结果
                executionService.failExecution(executionId, response.getErrorMessage(), duration);
            }

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("任务执行异常", e);

            // 保存异常信息
            executionService.failExecution(executionId, e.getMessage(), duration);

            throw e;
        }
    }

    /**
     * 执行任务（带对话历史）
     */
    public AgentResponse executeTaskWithHistory(String task, List<Message> history) {
        log.info("执行任务(带历史): {}", task);
        return agent.execute(task, history);
    }

    /**
     * 获取Agent信息
     * 缓存 1 小时（配置信息很少变化）
     */
    @Cacheable(value = "agentInfo", key = "'config'")
    public String getAgentInfo() {
        log.debug("构建 Agent 信息");

        String providerName = switch (modelProvider.toLowerCase()) {
            case "ollama" -> "本地Ollama";
            case "openai" -> "阿里云通义千问";
            default -> "未知";
        };

        return String.format("Agent名称: %s\n描述: %s\n模型提供商: %s\n可用工具: %d个",
            agent.getName(),
            agent.getDescription(),
            providerName,
            agent.getTools().size());
    }
}
