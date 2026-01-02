package cn.chollter.agent.demo.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent响应
 */
public class AgentResponse {

    private String finalAnswer;
    private List<ThoughtStep> thoughtSteps = new ArrayList<>();
    private boolean success;
    private String errorMessage;

    public AgentResponse() {}

    public AgentResponse(String finalAnswer, boolean success) {
        this.finalAnswer = finalAnswer;
        this.success = success;
    }

    public static AgentResponse success(String answer) {
        return new AgentResponse(answer, true);
    }

    public static AgentResponse error(String error) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(false);
        response.setErrorMessage(error);
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

    public void addThoughtStep(ThoughtStep step) {
        this.thoughtSteps.add(step);
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
