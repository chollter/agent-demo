package cn.chollter.agent.demo.service;

import cn.chollter.agent.demo.agent.Agent;
import cn.chollter.agent.demo.agent.AgentResponse;
import cn.chollter.agent.demo.agent.Message;
import cn.chollter.agent.demo.agent.ThoughtStep;
import cn.chollter.agent.demo.entity.Conversation;
import cn.chollter.agent.demo.entity.Execution;
import cn.chollter.agent.demo.repository.ExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
    private final ExecutionRepository executionRepository;

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
            ConversationService conversationService,
            ExecutionRepository executionRepository
    ) {
        this.agent = agent;
        this.executionService = executionService;
        this.conversationService = conversationService;
        this.executionRepository = executionRepository;
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
     * 加载会话历史（带缓存）
     * 从数据库获取指定会话的历史消息
     * 支持滑动窗口和 token 数量限制
     * 优化：添加缓存避免重复查询，使用 public 方法以便 @Cacheable 生效
     */
    @Cacheable(value = "conversationHistory",
               key = "#conversationId",
               unless = "#result.isEmpty()")
    public List<Message> loadConversationHistory(String conversationId) {
        // 检查是否启用对话历史
        if (!historyEnabled) {
            log.debug("对话历史功能已禁用");
            return new ArrayList<>();
        }

        // 使用分页查询，在数据库层面直接过滤成功的执行记录
        // 只查询需要的数据量，避免加载所有记录
        Pageable pageable = PageRequest.of(0, maxHistoryMessages * 2);
        Page<Execution> executionPage = executionRepository.findSuccessfulExecutionsByConversationId(
                conversationId, pageable);

        if (executionPage.isEmpty()) {
            log.debug("会话 {} 没有成功的执行记录", conversationId);
            return new ArrayList<>();
        }

        // 构建历史消息
        List<Message> allMessages = executionPage.getContent().stream()
                .flatMap(execution -> {
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

        log.info("对话历史: 共 {} 条，选择 {} 条", totalCount, selectedMessages.size());

        return selectedMessages;
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

    /**
     * 流式执行任务（简单版本，不带会话历史）
     * 返回 Server-Sent Events 流
     *
     * @param task 用户任务
     * @return SSE 事件流
     */
    public Flux<ServerSentEvent<String>> executeTaskStream(String task) {
        return executeTaskStreamInternal(task, new ArrayList<>(), null);
    }

    /**
     * 流式执行任务（带会话历史）
     * 返回 Server-Sent Events 流
     *
     * @param conversationId 会话ID
     * @param task 用户任务
     * @return SSE 事件流
     */
    public Flux<ServerSentEvent<String>> executeTaskStream(String conversationId, String task) {
        List<Message> history = new ArrayList<>();
        if (conversationId != null) {
            history = loadConversationHistory(conversationId);
        }
        return executeTaskStreamInternal(task, history, conversationId);
    }

    /**
     * 流式执行任务内部实现
     * 使用真正的 SSE 流式响应
     */
    private Flux<ServerSentEvent<String>> executeTaskStreamInternal(
            String task,
            List<Message> history,
            String conversationId) {

        log.info("开始流式执行任务: {}, 会话ID: {}", task, conversationId);

        // 创建执行记录
        Execution execution = executionService.createExecution(conversationId, task);
        String actualConversationId = execution.getConversation().getConversationId();
        String executionId = execution.getExecutionId();

        long startTime = System.currentTimeMillis();
        StringBuilder fullAnswer = new StringBuilder();
        boolean[] hasError = {false};

        // 调用 Agent 的流式方法
        return agent.executeStream(task, history)
            .doOnNext(event -> {
                // 收集完整内容用于保存
                if ("content".equals(event.event())) {
                    fullAnswer.append(event.data());
                } else if ("error".equals(event.event())) {
                    hasError[0] = true;
                }
            })
            .doOnComplete(() -> {
                // 流结束后保存执行结果
                long duration = System.currentTimeMillis() - startTime;
                if (!hasError[0] && fullAnswer.length() > 0) {
                    AgentResponse response = new AgentResponse();
                    response.setSuccess(true);
                    response.setFinalAnswer(fullAnswer.toString());
                    saveExecutionResultAsync(executionId, response, duration);
                    log.info("流式执行完成，保存结果: {}", executionId);
                } else if (hasError[0]) {
                    AgentResponse response = new AgentResponse();
                    response.setSuccess(false);
                    response.setErrorMessage("流式执行过程中发生错误");
                    saveExecutionResultAsync(executionId, response, duration);
                }
            })
            .doOnError(e -> {
                log.error("流式执行任务失败", e);
                long duration = System.currentTimeMillis() - startTime;
                AgentResponse response = new AgentResponse();
                response.setSuccess(false);
                response.setErrorMessage(e.getMessage());
                saveExecutionResultAsync(executionId, response, duration);
            })
            .doOnCancel(() -> {
                // 客户端取消连接（点击停止按钮）
                log.info("客户端取消流式执行: {}", executionId);
                long duration = System.currentTimeMillis() - startTime;
                // 保存已生成的内容（部分结果）
                if (fullAnswer.length() > 0) {
                    AgentResponse response = new AgentResponse();
                    response.setSuccess(true); // 标记为成功，因为是用户主动停止
                    response.setFinalAnswer(fullAnswer.toString() + "\n\n[用户停止生成]");
                    saveExecutionResultAsync(executionId, response, duration);
                }
            })
            .map(event -> {
                // 在结束事件中添加 conversationId
                if ("end".equals(event.event())) {
                    return ServerSentEvent.<String>builder()
                        .event("end")
                        .data(actualConversationId)
                        .build();
                }
                return event;
            });
    }
}
