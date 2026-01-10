package cn.chollter.agent.demo.controller;

import cn.chollter.agent.demo.dto.TaskRequest;
import cn.chollter.agent.demo.dto.TaskResponse;
import cn.chollter.agent.demo.security.RateLimit;
import cn.chollter.agent.demo.security.RateLimitType;
import cn.chollter.agent.demo.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Agent REST API控制器
 * 提供 AI Agent 任务执行接口
 *
 * @author Chollter
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/agent")
@AllArgsConstructor
@Tag(name = "Agent管理", description = "AI Agent 任务执行和管理接口")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;

    /**
     * 执行任务
     * POST /api/agent/execute
     *
     * @param request 任务请求
     * @return 任务响应
     */
    @PostMapping("/execute")
    @RateLimit(capacity = 20, refillTokens = 20, period = 60, type = RateLimitType.API_KEY)
    @Operation(summary = "执行Agent任务", description = """
            提交任务给 AI Agent 执行。

            支持的任务类型：
            - 文件操作：读取、写入、搜索文件
            - 数据查询：数据库查询和统计
            - GitHub操作：查询仓库、Issue等（需配置Token）
            - 网络搜索：Brave搜索（需配置API Key）

            注意事项：
            - 复杂任务可能需要较长时间执行
            - 建议设置合理的超时时间
            - 执行过程会被记录到数据库
            """)
    public ResponseEntity<TaskResponse> execute(
            @Parameter(description = "任务请求对象", required = true)
            @Valid @RequestBody TaskRequest request) {
        log.info("收到任务请求: {}, 会话ID: {}", request.getTask(), request.getConversationId());

        var agentResponse = agentService.executeTask(request.getConversationId(), request.getTask());
        return ResponseEntity.ok(TaskResponse.fromAgentResponse(agentResponse));
    }

    /**
     * 简化的执行接口（GET请求）
     * GET /api/agent/execute?task=xxx
     *
     * @param task 任务描述
     * @return 任务响应
     */
    @GetMapping("/execute")
    @RateLimit(capacity = 10, refillTokens = 10, period = 60, type = RateLimitType.IP)
    @Operation(summary = "简化任务执行接口", description = """
            通过 GET 请求快速执行任务。

            适用场景：
            - 简单查询任务
            - 快速测试
            - 浏览器直接访问

            限制：
            - 不支持会话ID
            - 参数长度受限
            """)
    public ResponseEntity<TaskResponse> executeSimple(
            @Parameter(description = "任务描述", required = true, example = "今天天气怎么样？")
            @RequestParam String task) {
        log.info("收到简化任务请求: {}", task);

        var agentResponse = agentService.executeTask(task);
        return ResponseEntity.ok(TaskResponse.fromAgentResponse(agentResponse));
    }

    /**
     * 获取Agent信息
     * GET /api/agent/info
     *
     * @return Agent信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取Agent信息", description = """
            返回当前 Agent 的配置信息，包括：
            - Agent 名称和描述
            - 使用的模型提供商（OpenAI/Ollama）
            - 可用工具数量和类型
            """)
    public ResponseEntity<String> getInfo() {
        return ResponseEntity.ok(agentService.getAgentInfo());
    }

    /**
     * 健康检查
     * GET /api/agent/health
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查 Agent 服务是否正常运行")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Agent is running!");
    }

    /**
     * 流式执行任务
     * POST /api/agent/stream
     *
     * @param request 任务请求
     * @return SSE 事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式执行Agent任务", description = """
            使用 Server-Sent Events (SSE) 流式返回 AI Agent 的执行结果。

            优势：
            - 实时返回生成内容，无需等待完整响应
            - 更好的用户体验，特别适合长文本生成
            - 支持超时处理和断线重连

            事件类型：
            - content: 内容片段
            - end: 响应结束（包含 conversationId）
            - error: 错误信息

            注意事项：
            - 流式响应不包含思考步骤（thoughtSteps）
            - 需要客户端支持 SSE 事件处理
            """)
    public Flux<ServerSentEvent<String>> executeStream(
            @Parameter(description = "任务请求对象", required = true)
            @Valid @RequestBody TaskRequest request) {
        log.info("收到流式任务请求: {}, 会话ID: {}", request.getTask(), request.getConversationId());

        if (request.getConversationId() != null) {
            return agentService.executeTaskStream(request.getConversationId(), request.getTask());
        } else {
            return agentService.executeTaskStream(request.getTask());
        }
    }

    /**
     * 流式执行任务（简化版，GET请求）
     * GET /api/agent/stream?task=xxx
     *
     * @param task 任务描述
     * @return SSE 事件流
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式任务执行接口（简化版）", description = """
            通过 GET 请求进行流式任务执行。

            适用场景：
            - 简单查询的流式响应
            - 快速测试流式功能
            - 浏览器直接访问

            限制：
            - 不支持会话ID
            """)
    public Flux<ServerSentEvent<String>> executeStreamSimple(
            @Parameter(description = "任务描述", required = true, example = "今天天气怎么样？")
            @RequestParam String task) {
        log.info("收到简化流式任务请求: {}", task);
        return agentService.executeTaskStream(task);
    }
}
