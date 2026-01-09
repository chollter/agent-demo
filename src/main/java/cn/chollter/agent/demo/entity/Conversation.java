package cn.chollter.agent.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话实体
 * 表示一次完整的对话会话
 *
 * @author Chollter
 * @since 1.0.0
 */
@Entity
@Table(name = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话唯一标识
     */
    @Column(unique = true, nullable = false, length = 64)
    private String conversationId;

    /**
     * 会话标题（第一个任务的前50个字符）
     */
    @Column(length = 255)
    private String title;

    /**
     * 模型提供商
     */
    @Column(length = 50)
    private String modelProvider;

    /**
     * 使用的模型名称
     */
    @Column(length = 100)
    private String modelName;

    /**
     * 会话状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 关联的任务执行记录
     * 使用 @JsonIgnore 避免 Redis 序列化时的懒加载异常
     */
    @JsonIgnore
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Execution> executions = new ArrayList<>();

    /**
     * 会话状态枚举
     */
    public enum ConversationStatus {
        ACTIVE,    // 活跃
        ARCHIVED,  // 已归档
        DELETED    // 已删除
    }
}
