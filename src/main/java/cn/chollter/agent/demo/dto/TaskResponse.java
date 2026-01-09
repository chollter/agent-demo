package cn.chollter.agent.demo.dto;

import cn.chollter.agent.demo.agent.ThoughtStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务响应DTO
 *
 * @author Chollter
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent 任务响应")
public class TaskResponse {

    /**
     * 会话ID
     */
    @Schema(description = "会话ID，用于多轮对话", example = "conv-a1b2c3d4e5f6")
    private String conversationId;

    /**
     * 最终答案
     */
    @Schema(description = "AI Agent 的最终答案", example = "根据查询结果，最近一周共有 50 条执行记录，其中成功 45 条，失败 5 条，成功率为 90%。")
    private String finalAnswer;

    /**
     * 思考步骤
     */
    @Schema(description = "AI Agent 的思考过程，包含每一步的推理和工具调用")
    private List<ThoughtStep> thoughtSteps;

    /**
     * 是否成功
     */
    @Schema(description = "任务是否成功执行", example = "true")
    private boolean success;

    /**
     * 错误消息
     */
    @Schema(description = "任务失败时的错误信息", example = "连接数据库超时")
    private String errorMessage;

    /**
     * 错误码
     */
    @Schema(description = "错误码，用于程序化处理错误", example = "DB_TIMEOUT")
    private String errorCode;

    /**
     * 关联ID（用于追踪请求）
     */
    @Schema(description = "请求追踪ID，用于日志关联和问题排查", example = "req-abc123xyz")
    private String correlationId;

    /**
     * 时间戳
     */
    @Schema(description = "响应时间戳", example = "2026-01-06T18:30:00")
    private LocalDateTime timestamp;

    public static TaskResponse fromAgentResponse(cn.chollter.agent.demo.agent.AgentResponse agentResponse) {
        return TaskResponse.builder()
                .conversationId(agentResponse.getConversationId())
                .finalAnswer(agentResponse.getFinalAnswer())
                .thoughtSteps(agentResponse.getThoughtSteps())
                .success(agentResponse.isSuccess())
                .errorMessage(agentResponse.getErrorMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

}
