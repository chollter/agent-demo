package cn.chollter.agent.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务请求DTO
 *
 * @author Chollter
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent 任务请求")
public class TaskRequest {

    /**
     * 任务描述
     */
    @Schema(
            description = "任务描述内容",
            example = "请帮我查询最近一周的执行记录，并统计成功和失败的数量",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 2000
    )
    @NotBlank(message = "任务内容不能为空")
    @Size(min = 1, max = 2000, message = "任务内容长度必须在 1-2000 字符之间")
    private String task;

    /**
     * 会话ID（可选，用于多轮对话）
     */
    @Schema(
            description = "会话ID，用于多轮对话上下文",
            example = "conv-a1b2c3d4e5f6",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String conversationId;

}
