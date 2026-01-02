package cn.chollter.agent.demo.agent;

/**
 * 思考步骤（用于ReAct模式）
 */
public class ThoughtStep {

    private StepType stepType;
    private String content;

    public ThoughtStep() {}

    public ThoughtStep(StepType stepType, String content) {
        this.stepType = stepType;
        this.content = content;
    }

    public enum StepType {
        THOUGHT,    // 思考
        ACTION,     // 行动
        OBSERVATION // 观察
    }

    public StepType getStepType() {
        return stepType;
    }

    public void setStepType(StepType stepType) {
        this.stepType = stepType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
