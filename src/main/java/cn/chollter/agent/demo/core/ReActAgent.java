package cn.chollter.agent.demo.core;

import cn.chollter.agent.demo.agent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReAct (Reasoning + Acting) Agent实现
 * 使用思考-行动-观察循环来完成任务
 */
@Component
public class ReActAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ReActAgent.class);
    private static final int MAX_STEPS = 10;

    private final ChatModel chatModel;
    private final List<Tool> tools;
    private final ObjectMapper objectMapper;

    private static final String REACT_PROMPT_TEMPLATE = """
        你是一个智能助手，可以使用工具来完成任务。请按照以下格式进行思考：

        Thought: 思考当前情况，分析需要做什么
        Action: 工具名称
        Action Input: 工具参数（JSON格式）

        当你获得足够信息时，使用以下格式给出最终答案：

        Thought: 我已经知道答案了
        Final Answer: 最终答案

        可用工具：
        %s

        用户问题：%s

        开始！
        """;

    public ReActAgent(ChatModel chatModel, List<Tool> tools, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.tools = tools;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentResponse execute(String task) {
        return execute(task, new ArrayList<>());
    }

    @Override
    public AgentResponse execute(String task, List<Message> conversationHistory) {
        AgentResponse response = new AgentResponse();

        try {
            // 构建工具描述
            String toolsDescription = buildToolsDescription();

            // 构建初始prompt
            String prompt = String.format(REACT_PROMPT_TEMPLATE, toolsDescription, task);

            int stepCount = 0;
            StringBuilder reasoning = new StringBuilder();

            while (stepCount < MAX_STEPS) {
                stepCount++;
                log.info("ReAct步骤 {}", stepCount);

                // 调用LLM进行推理
                String fullPrompt = prompt + "\n" + reasoning;
                String llmOutput = chatModel.call(new Prompt(new UserMessage(fullPrompt)))
                        .getResult()
                        .getOutput()
                        .getText();

                log.debug("LLM输出:\n{}", llmOutput);

                // 解析LLM输出
                if (llmOutput.contains("Final Answer:")) {
                    // 提取最终答案
                    String finalAnswer = extractFinalAnswer(llmOutput);
                    response.setFinalAnswer(finalAnswer);
                    response.setSuccess(true);
                    response.addThoughtStep(new ThoughtStep(ThoughtStep.StepType.THOUGHT,
                        "得出最终答案"));
                    break;
                }

                // 提取Thought
                String thought = extractSection(llmOutput, "Thought:");
                if (thought != null) {
                    response.addThoughtStep(new ThoughtStep(ThoughtStep.StepType.THOUGHT, thought));
                    reasoning.append("Thought: ").append(thought).append("\n");
                }

                // 提取Action
                String action = extractSection(llmOutput, "Action:");
                if (action == null) {
                    response.setSuccess(false);
                    response.setErrorMessage("无法解析Action");
                    break;
                }

                // 提取Action Input
                String actionInputStr = extractSection(llmOutput, "Action Input:");
                if (actionInputStr == null) {
                    response.setSuccess(false);
                    response.setErrorMessage("无法解析Action Input");
                    break;
                }

                // 执行工具
                Tool tool = findTool(action);
                if (tool == null) {
                    response.setSuccess(false);
                    response.setErrorMessage("未找到工具: " + action);
                    break;
                }

                response.addThoughtStep(new ThoughtStep(ThoughtStep.StepType.ACTION,
                    "使用工具: " + action));

                Map<String, Object> actionInput = parseJson(actionInputStr);
                String observation = tool.execute(actionInput);

                response.addThoughtStep(new ThoughtStep(ThoughtStep.StepType.OBSERVATION, observation));
                reasoning.append("Action: ").append(action).append("\n");
                reasoning.append("Action Input: ").append(actionInputStr).append("\n");
                reasoning.append("Observation: ").append(observation).append("\n\n");
            }

            if (stepCount >= MAX_STEPS) {
                response.setSuccess(false);
                response.setErrorMessage("超过最大步数限制");
            }

        } catch (Exception e) {
            log.error("Agent执行失败", e);
            response.setSuccess(false);
            response.setErrorMessage("执行失败: " + e.getMessage());
        }

        return response;
    }

    @Override
    public String getName() {
        return "ReAct Agent";
    }

    @Override
    public String getDescription() {
        return "使用ReAct推理模式的智能Agent";
    }

    @Override
    public List<Tool> getTools() {
        return tools;
    }

    private String buildToolsDescription() {
        StringBuilder sb = new StringBuilder();
        for (Tool tool : tools) {
            sb.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
        }
        return sb.toString();
    }

    private Tool findTool(String name) {
        return tools.stream()
            .filter(t -> t.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    private String extractSection(String text, String section) {
        Pattern pattern = Pattern.compile(section + "\\s*([\\s\\S]*?)(?=\\n(?:Thought|Action|Action Input|Final Answer)|$)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractFinalAnswer(String text) {
        Pattern pattern = Pattern.compile("Final Answer:\\s*([\\s\\S]*?)$");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "未找到答案";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("JSON解析失败: {}", json, e);
            throw new RuntimeException("无效的JSON格式: " + json);
        }
    }
}
