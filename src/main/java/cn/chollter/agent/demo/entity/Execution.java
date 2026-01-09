package cn.chollter.agent.demo.entity;

import cn.chollter.agent.demo.agent.ThoughtStep;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务执行实体
 * 记录每次任务的执行过程
 *
 * @author Chollter
 * @since 1.0.0
 */
@Entity
@Table(name = "executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 执行唯一标识
     */
    @Column(unique = true, nullable = false, length = 64)
    private String executionId;

    /**
     * 关联的会话
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * 任务描述
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String task;

    /**
     * 最终答案
     */
    @Column(columnDefinition = "TEXT")
    private String finalAnswer;

    /**
     * 是否成功
     */
    @Column(nullable = false)
    private Boolean success;

    /**
     * 错误消息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 执行状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.IN_PROGRESS;

    /**
     * 执行的步骤数
     */
    @Column(nullable = false)
    private Integer steps = 0;

    /**
     * 总耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 使用的 Token 数量（如果有）
     */
    private Long totalTokens;

    /**
     * 思考步骤（JSON 格式存储）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<ThoughtStep> thoughtSteps = new ArrayList<>();

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    /**
     * 最后更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        IN_PROGRESS,  // 进行中
        COMPLETED,    // 已完成
        FAILED,       // 失败
        TIMEOUT       // 超时
    }
}
