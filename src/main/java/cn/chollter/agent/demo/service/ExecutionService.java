package cn.chollter.agent.demo.service;

import cn.chollter.agent.demo.agent.ThoughtStep;
import cn.chollter.agent.demo.entity.Conversation;
import cn.chollter.agent.demo.entity.Execution;
import cn.chollter.agent.demo.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 任务执行服务
 *
 * @author Chollter
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final ConversationService conversationService;

    /**
     * 创建新的任务执行记录
     */
    @Transactional
    public Execution createExecution(String conversationId, String task) {
        // 获取或创建会话
        Conversation conversation;
        if (conversationId != null) {
            // 如果 conversationId 不为空，先尝试查找
            conversation = conversationService
                    .getConversationByConversationId(conversationId)
                    .orElseGet(() -> conversationService.createConversation(
                            task.length() > 50 ? task.substring(0, 50) : task,
                            "ollama", // 默认模型，可以从配置读取
                            "qwen2.5:7b"
                    ));
        } else {
            // 如果 conversationId 为空，直接创建新会话
            conversation = conversationService.createConversation(
                    task.length() > 50 ? task.substring(0, 50) : task,
                    "ollama", // 默认模型，可以从配置读取
                    "qwen2.5:7b"
            );
        }

        Execution execution = new Execution();
        execution.setExecutionId(generateExecutionId());
        execution.setConversation(conversation);
        execution.setTask(task);
        execution.setStatus(Execution.ExecutionStatus.IN_PROGRESS);
        execution.setSuccess(false);
        execution.setSteps(0);

        Execution saved = executionRepository.save(execution);
        log.debug("创建执行记录: {} for conversation: {}", saved.getExecutionId(), conversationId);
        return saved;
    }

    /**
     * 更新执行记录为成功状态
     * 清除相关缓存
     */
    @Transactional
    @CacheEvict(value = {"executions", "conversationExecutions", "executionStats"}, allEntries = true)
    public void completeExecution(String executionId, String finalAnswer,
                                   List<ThoughtStep> thoughtSteps, int steps, Long durationMs) {
        executionRepository.findByExecutionId(executionId).ifPresent(execution -> {
            execution.setFinalAnswer(finalAnswer);
            execution.setThoughtSteps(thoughtSteps);
            execution.setSteps(steps);
            execution.setDurationMs(durationMs);
            execution.setStatus(Execution.ExecutionStatus.COMPLETED);
            execution.setSuccess(true);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            log.info("执行完成: {}, 耗时: {} ms", executionId, durationMs);
        });
    }

    /**
     * 更新执行记录为失败状态
     * 清除相关缓存
     */
    @Transactional
    @CacheEvict(value = {"executions", "conversationExecutions", "executionStats"}, allEntries = true)
    public void failExecution(String executionId, String errorMessage, Long durationMs) {
        executionRepository.findByExecutionId(executionId).ifPresent(execution -> {
            execution.setErrorMessage(errorMessage);
            execution.setDurationMs(durationMs);
            execution.setStatus(Execution.ExecutionStatus.FAILED);
            execution.setSuccess(false);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            log.warn("执行失败: {}", executionId);
        });
    }

    /**
     * 根据 ID 获取执行记录
     */
    @Cacheable(value = "executions", key = "'id:' + #id")
    public Optional<Execution> getExecutionById(Long id) {
        return executionRepository.findById(id);
    }

    /**
     * 根据 executionId 获取执行记录
     */
    @Cacheable(value = "executions", key = "#executionId")
    public Optional<Execution> getExecutionByExecutionId(String executionId) {
        log.debug("从数据库查询执行记录: {}", executionId);
        return executionRepository.findByExecutionId(executionId);
    }

    /**
     * 获取指定会话的所有执行记录
     * 缓存 10 分钟
     */
    @Cacheable(value = "conversationExecutions", key = "#conversationId")
    public List<Execution> getExecutionsByConversationId(String conversationId) {
        log.debug("从数据库查询会话执行记录: {}", conversationId);
        return executionRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
    }

    /**
     * 获取最近的成功执行记录
     * 缓存 5 分钟
     */
    @Cacheable(value = "recentExecutions", key = "'successful'")
    public List<Execution> getRecentSuccessfulExecutions() {
        log.debug("从数据库查询最近成功执行记录");
        return executionRepository.findRecentSuccessfulExecutions();
    }

    /**
     * 统计成功的执行次数
     * 缓存统计数据 5 分钟
     */
    @Cacheable(value = "executionStats", key = "'successCount'")
    public long getSuccessfulExecutionCount() {
        log.debug("从数据库统计成功执行次数");
        return executionRepository.countSuccessfulExecutions();
    }

    /**
     * 统计失败的执行次数
     * 缓存统计数据 5 分钟
     */
    @Cacheable(value = "executionStats", key = "'failedCount'")
    public long getFailedExecutionCount() {
        log.debug("从数据库统计失败执行次数");
        return executionRepository.countFailedExecutions();
    }

    /**
     * 计算平均执行时长
     * 缓存统计数据 5 分钟
     */
    @Cacheable(value = "executionStats", key = "'avgDuration'")
    public Double getAverageDuration() {
        log.debug("从数据库计算平均执行时长");
        return executionRepository.findAverageDuration();
    }

    /**
     * 生成执行 ID
     */
    private String generateExecutionId() {
        return "exec-" + UUID.randomUUID().toString().replace("-", "");
    }
}
