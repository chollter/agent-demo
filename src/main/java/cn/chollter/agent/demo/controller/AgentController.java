package cn.chollter.agent.demo.controller;

import cn.chollter.agent.demo.dto.TaskRequest;
import cn.chollter.agent.demo.dto.TaskResponse;
import cn.chollter.agent.demo.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
