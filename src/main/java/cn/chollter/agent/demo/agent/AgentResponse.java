package cn.chollter.agent.demo.agent;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent响应
 */
@Setter
@Getter
public class AgentResponse {

    private String conversationId;
    private String finalAnswer;
    private List<ThoughtStep> thoughtSteps = new ArrayList<>();
    private boolean success;
    private String errorMessage;

    // Token 统计
    private int totalTokens;
    private int inputTokens;
    private int outputTokens;

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

    public void addThoughtStep(ThoughtStep step) {
        this.thoughtSteps.add(step);
    }

    /**
     * 增加 Token 统计
     */
    public void addTokens(int input, int output) {
        this.inputTokens += input;
        this.outputTokens += output;
        this.totalTokens = this.inputTokens + this.outputTokens;
    }

}
