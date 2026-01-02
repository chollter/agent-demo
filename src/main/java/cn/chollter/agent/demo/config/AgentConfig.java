package cn.chollter.agent.demo.config;

import cn.chollter.agent.demo.agent.Tool;
import cn.chollter.agent.demo.core.ReActAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

/**
 * Agent配置类
 * 使用阿里云通义千问作为 AI 模型
 */
@Configuration
public class AgentConfig {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.model:qwen-max}")
    private String model;

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    /**
     * 配置 ChatModel - 阿里云通义千问
     * 使用 OpenAiChatModel.Builder 直接构建
     */
    @Bean
    public ChatModel chatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build())
                .build();
    }

    /**
     * 配置ReActAgent Bean
     */
    @Bean
    public ReActAgent reactAgent(
            ChatModel chatModel,
            List<Tool> tools,
            ObjectMapper objectMapper) {
        return new ReActAgent(chatModel, tools, objectMapper);
    }

    /**
     * 配置ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
