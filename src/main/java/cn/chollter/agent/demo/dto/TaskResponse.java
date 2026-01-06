package cn.chollter.agent.demo.dto;

import cn.chollter.agent.demo.agent.ThoughtStep;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 任务响应DTO
 */
@Setter
@Getter
public class TaskResponse {

    private String finalAnswer;
    private List<ThoughtStep> thoughtSteps;
    private boolean success;
    private String errorMessage;

    public TaskResponse() {}

    public static TaskResponse fromAgentResponse(cn.chollter.agent.demo.agent.AgentResponse agentResponse) {
        TaskResponse response = new TaskResponse();
        response.setFinalAnswer(agentResponse.getFinalAnswer());
        response.setThoughtSteps(agentResponse.getThoughtSteps());
        response.setSuccess(agentResponse.isSuccess());
        response.setErrorMessage(agentResponse.getErrorMessage());
        return response;
    }

}
