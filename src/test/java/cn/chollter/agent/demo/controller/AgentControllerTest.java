package cn.chollter.agent.demo.controller;

import cn.chollter.agent.demo.agent.AgentResponse;
import cn.chollter.agent.demo.dto.TaskRequest;
import cn.chollter.agent.demo.dto.TaskResponse;
import cn.chollter.agent.demo.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AgentController 测试类
 *
 * @author Chollter
 * @since 1.0.0
 */
@WebMvcTest(AgentController.class)
@ActiveProfiles("test")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentService agentService;

    @Test
    void testExecuteTask_Success() throws Exception {
        // 准备测试数据
        AgentResponse agentResponse = new AgentResponse();
        agentResponse.setFinalAnswer("42");
        agentResponse.setThoughtSteps(List.of());
        agentResponse.setSuccess(true);

        when(agentService.executeTask(any(String.class))).thenReturn(agentResponse);

        // 执行测试
        mockMvc.perform(post("/api/agent/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                TaskRequest.builder().task("What is 1 + 1?").build()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.finalAnswer").value("42"));
    }

    @Test
    void testExecuteTask_ValidationError() throws Exception {
        mockMvc.perform(post("/api/agent/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                TaskRequest.builder().task("").build()
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testExecuteSimple_Success() throws Exception {
        AgentResponse agentResponse = new AgentResponse();
        agentResponse.setFinalAnswer("Hello World");
        agentResponse.setSuccess(true);

        when(agentService.executeTask(eq("Say hello"))).thenReturn(agentResponse);

        mockMvc.perform(get("/api/agent/execute")
                        .param("task", "Say hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.finalAnswer").value("Hello World"));
    }

    @Test
    void testGetInfo() throws Exception {
        when(agentService.getAgentInfo())
                .thenReturn("Agent名称: Test Agent\n描述: Test Description");

        mockMvc.perform(get("/api/agent/info"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test Agent")));
    }

    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/api/agent/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Agent is running!"));
    }
}
