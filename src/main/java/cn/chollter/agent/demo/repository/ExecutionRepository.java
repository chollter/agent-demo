package cn.chollter.agent.demo.repository;

import cn.chollter.agent.demo.entity.Execution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 任务执行 Repository
 *
 * @author Chollter
 * @since 1.0.0
 */
@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    /**
     * 根据 executionId 查找执行记录
     */
    Optional<Execution> findByExecutionId(String executionId);

    /**
     * 查找指定会话的所有执行记录
     */
    List<Execution> findByConversationConversationId(String conversationId);

    /**
     * 查找指定状态的执行记录
     */
    List<Execution> findByStatus(Execution.ExecutionStatus status);

    /**
     * 查找指定会话的执行记录（按创建时间降序）
     */
    @Query("SELECT e FROM Execution e WHERE e.conversation.conversationId = :conversationId ORDER BY e.createdAt DESC")
    List<Execution> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") String conversationId);

    /**
     * 查找指定时间范围内的执行记录
     */
    List<Execution> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计成功的执行次数
     */
    @Query("SELECT COUNT(e) FROM Execution e WHERE e.success = true")
    long countSuccessfulExecutions();

    /**
     * 统计失败的执行次数
     */
    @Query("SELECT COUNT(e) FROM Execution e WHERE e.success = false")
    long countFailedExecutions();

    /**
     * 查找最近的成功执行记录
     */
    @Query("SELECT e FROM Execution e WHERE e.success = true ORDER BY e.createdAt DESC")
    List<Execution> findRecentSuccessfulExecutions();

    /**
     * 计算平均执行时长（毫秒）
     */
    @Query("SELECT AVG(e.durationMs) FROM Execution e WHERE e.durationMs IS NOT NULL")
    Double findAverageDuration();

    /**
     * 查找指定会话的成功执行记录（带分页）
     * 在数据库层面直接过滤，避免内存处理
     *
     * @param conversationId 会话ID
     * @param pageable 分页参数
     * @return 分页的成功执行记录
     */
    @Query("""
        SELECT e FROM Execution e
        WHERE e.conversation.conversationId = :conversationId
        AND e.success = true
        AND e.finalAnswer IS NOT NULL
        ORDER BY e.createdAt DESC
        """)
    Page<Execution> findSuccessfulExecutionsByConversationId(
            @Param("conversationId") String conversationId,
            Pageable pageable
    );
}
