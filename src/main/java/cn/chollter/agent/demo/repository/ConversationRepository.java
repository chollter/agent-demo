package cn.chollter.agent.demo.repository;

import cn.chollter.agent.demo.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话 Repository
 *
 * @author Chollter
 * @since 1.0.0
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * 根据 conversationId 查找会话
     */
    Optional<Conversation> findByConversationId(String conversationId);

    /**
     * 查找活跃的会话
     */
    List<Conversation> findByStatus(Conversation.ConversationStatus status);

    /**
     * 根据模型提供商查找会话
     */
    List<Conversation> findByModelProvider(String modelProvider);

    /**
     * 查找指定时间范围内创建的会话
     */
    List<Conversation> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计活跃会话数量
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.status = :status")
    long countByStatus(@Param("status") Conversation.ConversationStatus status);

    /**
     * 查找最近创建的会话
     */
    @Query("SELECT c FROM Conversation c ORDER BY c.createdAt DESC")
    List<Conversation> findRecentConversations();
}
