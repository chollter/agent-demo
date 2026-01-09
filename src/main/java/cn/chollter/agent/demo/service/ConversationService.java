package cn.chollter.agent.demo.service;

import cn.chollter.agent.demo.entity.Conversation;
import cn.chollter.agent.demo.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话服务
 *
 * @author Chollter
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    /**
     * 创建新会话
     * 清除列表缓存
     */
    @Transactional
    @CacheEvict(value = {"conversations", "activeConversations", "recentConversations"}, allEntries = true)
    public Conversation createConversation(String title, String modelProvider, String modelName) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(generateConversationId());
        conversation.setTitle(title != null && title.length() > 50 ? title.substring(0, 50) : title);
        conversation.setModelProvider(modelProvider);
        conversation.setModelName(modelName);
        conversation.setStatus(Conversation.ConversationStatus.ACTIVE);

        Conversation saved = conversationRepository.save(conversation);
        log.debug("创建新会话: {}", saved.getConversationId());
        return saved;
    }

    /**
     * 根据 ID 获取会话
     */
    @Cacheable(value = "conversations", key = "'id:' + #id")
    public Optional<Conversation> getConversationById(Long id) {
        return conversationRepository.findById(id);
    }

    /**
     * 根据 conversationId 获取会话
     * 使用 Spring Cache 缓存结果
     * 如果 conversationId 为 null，直接返回 Optional.empty()
     */
    // @Cacheable(value = "conversations", key = "#conversationId", unless = "#conversationId == null")
    public Optional<Conversation> getConversationByConversationId(String conversationId) {
        if (conversationId == null) {
            log.debug("conversationId 为 null，返回空 Optional");
            return Optional.empty();
        }
        log.debug("从数据库查询会话: {}", conversationId);
        return conversationRepository.findByConversationId(conversationId);
    }

    /**
     * 根据 conversationId 获取会话，并预加载 executions
     * 此方法不使用缓存，避免 LazyInitializationException
     * 用于需要访问 executions 集合的场景
     */
    @Transactional(readOnly = true)
    public Optional<Conversation> getConversationByConversationIdWithExecutions(String conversationId) {
        if (conversationId == null) {
            log.debug("conversationId 为 null，返回空 Optional");
            return Optional.empty();
        }
        log.debug("从数据库查询会话（含 executions）: {}", conversationId);
        return conversationRepository.findByConversationIdWithExecutions(conversationId);
    }

    /**
     * 获取所有活跃会话
     * 缓存 5 分钟
     */
    @Cacheable(value = "activeConversations", key = "'all'")
    public List<Conversation> getActiveConversations() {
        log.debug("从数据库查询活跃会话列表");
        return conversationRepository.findByStatus(Conversation.ConversationStatus.ACTIVE);
    }

    /**
     * 获取最近的会话
     * 缓存 5 分钟
     */
    @Cacheable(value = "recentConversations", key = "'all'")
    public List<Conversation> getRecentConversations() {
        log.debug("从数据库查询最近会话列表");
        return conversationRepository.findRecentConversations();
    }

    /**
     * 归档会话
     * 清除相关缓存
     */
    @Transactional
    @CacheEvict(value = {"conversations", "activeConversations", "recentConversations"}, key = "#conversationId")
    public void archiveConversation(String conversationId) {
        conversationRepository.findByConversationId(conversationId).ifPresent(conversation -> {
            conversation.setStatus(Conversation.ConversationStatus.ARCHIVED);
            conversationRepository.save(conversation);
            log.info("归档会话: {}", conversationId);
        });
    }

    /**
     * 删除会话
     * 清除相关缓存
     */
    @Transactional
    @CacheEvict(value = {"conversations", "activeConversations", "recentConversations"}, allEntries = true)
    public void deleteConversation(String conversationId) {
        conversationRepository.findByConversationId(conversationId).ifPresent(conversation -> {
            conversation.setStatus(Conversation.ConversationStatus.DELETED);
            conversationRepository.save(conversation);
            log.info("删除会话: {}", conversationId);
        });
    }

    /**
     * 统计活跃会话数量
     * 缓存统计数据 5 分钟
     */
    @Cacheable(value = "conversationStats", key = "'activeCount'")
    public long getActiveConversationCount() {
        log.debug("从数据库统计活跃会话数量");
        return conversationRepository.countByStatus(Conversation.ConversationStatus.ACTIVE);
    }

    /**
     * 生成会话 ID
     */
    private String generateConversationId() {
        return "conv-" + UUID.randomUUID().toString().replace("-", "");
    }
}
