package cn.chollter.agent.demo.dto;

import cn.chollter.agent.demo.agent.ThoughtStep;

import java.util.List;

/**
 * 任务响应DTO
 */
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

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    public List<ThoughtStep> getThoughtSteps() {
        return thoughtSteps;
    }

    public void setThoughtSteps(List<ThoughtStep> thoughtSteps) {
        this.thoughtSteps = thoughtSteps;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
