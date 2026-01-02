package cn.chollter.agent.demo.controller;

import cn.chollter.agent.demo.dto.TaskRequest;
import cn.chollter.agent.demo.dto.TaskResponse;
import cn.chollter.agent.demo.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Agent REST API控制器
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 执行任务
     * POST /api/agent/execute
     *
     * @param request 任务请求
     * @return 任务响应
     */
    @PostMapping("/execute")
    public ResponseEntity<TaskResponse> execute(@RequestBody TaskRequest request) {
        log.info("收到任务请求: {}", request.getTask());

        try {
            var agentResponse = agentService.executeTask(request.getTask());
            return ResponseEntity.ok(TaskResponse.fromAgentResponse(agentResponse));
        } catch (Exception e) {
            log.error("任务执行失败", e);
            TaskResponse errorResponse = new TaskResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("任务执行失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 简化的执行接口（GET请求）
     * GET /api/agent/execute?task=xxx
     *
     * @param task 任务描述
     * @return 任务响应
     */
    @GetMapping("/execute")
    public ResponseEntity<TaskResponse> executeSimple(@RequestParam String task) {
        log.info("收到简化任务请求: {}", task);

        try {
            var agentResponse = agentService.executeTask(task);
            return ResponseEntity.ok(TaskResponse.fromAgentResponse(agentResponse));
        } catch (Exception e) {
            log.error("任务执行失败", e);
            TaskResponse errorResponse = new TaskResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("任务执行失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取Agent信息
     * GET /api/agent/info
     *
     * @return Agent信息
     */
    @GetMapping("/info")
    public ResponseEntity<String> getInfo() {
        return ResponseEntity.ok(agentService.getAgentInfo());
    }

    /**
     * 健康检查
     * GET /api/agent/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Agent is running!");
    }
}
