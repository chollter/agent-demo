package cn.chollter.agent.demo.agent;

import lombok.Getter;
import lombok.Setter;

/**
 * 思考步骤（用于ReAct模式）
 */
@Setter
@Getter
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

}
