package cn.chollter.agent.demo.service;

import cn.chollter.agent.demo.agent.Agent;
import cn.chollter.agent.demo.agent.AgentResponse;
import cn.chollter.agent.demo.agent.Message;
import cn.chollter.agent.demo.agent.ThoughtStep;
import cn.chollter.agent.demo.entity.Conversation;
import cn.chollter.agent.demo.entity.Execution;
import cn.chollter.agent.demo.util.TokenEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    private final Agent agent;
    private final ExecutionService executionService;
    private final ConversationService conversationService;

    @Value("${agent.model.provider:openai}")
    private String modelProvider;

    // 对话历史配置
    @Value("${agent.history.enabled:true}")
    private boolean historyEnabled;

    @Value("${agent.history.max-messages:20}")
    private int maxHistoryMessages;

    @Value("${agent.history.max-tokens:4000}")
    private int maxHistoryTokens;

    @Value("${agent.history.recent-first:true}")
    private boolean recentHistoryFirst;

    public AgentService(
            @Qualifier("agent") Agent agent,
            ExecutionService executionService,
            ConversationService conversationService
    ) {
        this.agent = agent;
        this.executionService = executionService;
        this.conversationService = conversationService;
    }

    /**
     * 执行任务（不带会话ID，创建新会话）
     */
    public AgentResponse executeTask(String task) {
        return executeTask(null, task);
    }

    /**
     * 执行任务（带会话ID，用于多轮对话）
     * 返回 AgentResponse
     * 会通过 execution 获取实际的 conversationId
     */
    public AgentResponse executeTask(String conversationId, String task) {
        log.info("执行任务: {}, 会话ID: {}", task, conversationId);

        // 获取会话历史
        List<Message> history = new ArrayList<>();
        if (conversationId != null) {
            history = loadConversationHistory(conversationId);
            log.debug("加载了 {} 条历史消息", history.size());
        }

        // 创建执行记录
        Execution execution = executionService.createExecution(conversationId, task);
        String executionId = execution.getExecutionId();
        // 获取实际的 conversationId（可能是新创建的）
        String actualConversationId = execution.getConversation().getConversationId();

        long startTime = System.currentTimeMillis();

        try {
            // 执行任务（带历史）
            AgentResponse response = agent.execute(task, history);

            long duration = System.currentTimeMillis() - startTime;

            if (response.isSuccess()) {
                log.info("任务执行成功，答案: {}", response.getFinalAnswer());
            } else {
                log.error("任务执行失败: {}", response.getErrorMessage());
            }

            // 异步保存执行结果（不阻塞响应）
            saveExecutionResultAsync(executionId, response, duration);

            // 将 conversationId 设置到响应中
            response.setConversationId(actualConversationId);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("任务执行异常", e);

            // 异步保存异常信息
            AgentResponse errorResponse = new AgentResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage(e.getMessage());
            saveExecutionResultAsync(executionId, errorResponse, duration);

            throw e;
        }
    }

    /**
     * 加载会话历史
     * 从数据库获取指定会话的历史消息
     * 支持滑动窗口和 token 数量限制
     */
    private List<Message> loadConversationHistory(String conversationId) {
        // 检查是否启用对话历史
        if (!historyEnabled) {
            log.debug("对话历史功能已禁用");
            return new ArrayList<>();
        }

        return conversationService.getConversationByConversationIdWithExecutions(conversationId)
                .map(conversation -> {
                    if (conversation.getExecutions() == null || conversation.getExecutions().isEmpty()) {
                        log.debug("会话 {} 没有执行记录", conversationId);
                        return new ArrayList<Message>();
                    }

                    // 构建所有历史消息
                    List<Message> allMessages = conversation.getExecutions().stream()
                            .filter(e -> e.getSuccess() && e.getFinalAnswer() != null)
                            .<Message>flatMap(execution -> {
                                List<Message> msgs = new ArrayList<>();
                                msgs.add(new Message(Message.Role.USER, execution.getTask()));
                                msgs.add(new Message(Message.Role.ASSISTANT, execution.getFinalAnswer()));
                                return msgs.stream();
                            })
                            .collect(Collectors.toList());

                    int totalCount = allMessages.size();
                    log.debug("会话 {} 共有 {} 条历史消息", conversationId, totalCount);

                    // 应用滑动窗口策略：优先保留最近的消息
                    List<Message> selectedMessages = applySlidingWindow(allMessages);

                    // 应用 token 数量限制
                    selectedMessages = applyTokenLimit(selectedMessages);

                    log.info("对话历史: 共 {} 条，选择 {} 条，估算 {}",
                            totalCount,
                            selectedMessages.size(),
                            TokenEstimator.formatTokens(estimateTokensForMessages(selectedMessages)));

                    return selectedMessages;
                })
                .orElse(new ArrayList<Message>());
    }

    /**
     * 应用滑动窗口策略
     * 优先保留最近的消息
     */
    private List<Message> applySlidingWindow(List<Message> messages) {
        if (messages.size() <= maxHistoryMessages) {
            return messages;
        }

        // 优先保留最近的消息
        if (recentHistoryFirst) {
            return messages.subList(messages.size() - maxHistoryMessages, messages.size());
        } else {
            // 保留最早的消息
            return messages.subList(0, maxHistoryMessages);
        }
    }

    /**
     * 应用 token 数量限制
     * 从消息列表末尾开始，保留 token 数量不超过限制的消息
     */
    private List<Message> applyTokenLimit(List<Message> messages) {
        if (messages.isEmpty()) {
            return messages;
        }

        // 计算所有消息的 token 数量
        int totalTokens = estimateTokensForMessages(messages);

        if (totalTokens <= maxHistoryTokens) {
            return messages;
        }

        log.debug("历史消息 token 数量 {} 超过限制 {}，进行裁剪",
                TokenEstimator.formatTokens(totalTokens),
                TokenEstimator.formatTokens(maxHistoryTokens));

        // 从末尾开始保留消息，直到 token 数量超过限制
        List<Message> result = new ArrayList<>();
        int currentTokens = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            int msgTokens = TokenEstimator.estimateTokens(msg.getContent());

            if (currentTokens + msgTokens > maxHistoryTokens) {
                break;
            }

            result.add(0, msg);
            currentTokens += msgTokens;
        }

        log.info("Token 限制后保留 {} 条消息，估算 {}",
                result.size(),
                TokenEstimator.formatTokens(currentTokens));

        return result;
    }

    /**
     * 估算消息列表的总 token 数量
     */
    private int estimateTokensForMessages(List<Message> messages) {
        return messages.stream()
                .mapToInt(msg -> TokenEstimator.estimateTokens(msg.getContent()))
                .sum();
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

    /**
     * 异步保存执行结果
     * 不阻塞主线程，提升响应速度
     */
    @Async("taskExecutor")
    public void saveExecutionResultAsync(String executionId, AgentResponse response, long duration) {
        try {
            if (response.isSuccess()) {
                executionService.completeExecution(
                        executionId,
                        response.getFinalAnswer(),
                        response.getThoughtSteps(),
                        response.getThoughtSteps() != null ? response.getThoughtSteps().size() : 0,
                        duration
                );
            } else {
                executionService.failExecution(executionId, response.getErrorMessage(), duration);
            }
            log.debug("异步保存执行结果完成: {}", executionId);
        } catch (Exception e) {
            log.error("异步保存执行结果失败: {}", executionId, e);
        }
    }
}
