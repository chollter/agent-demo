package cn.chollter.agent.demo.config;

import cn.chollter.agent.demo.agent.Agent;
import cn.chollter.agent.demo.agent.Tool;
import cn.chollter.agent.demo.core.FunctionCallingAgent;
import cn.chollter.agent.demo.mcp.McpManager;
import cn.chollter.agent.demo.service.ToolCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Agent配置类
 * 支持多种AI模型：阿里云通义千问、本地Ollama等
 */
@Slf4j
@Configuration
public class AgentConfig {

    @Value("${agent.model.provider:openai}")
    private String modelProvider;

    // OpenAI 配置（阿里云通义千问）
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.model:qwen-max}")
    private String model;

    // Ollama 配置
    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.model:qwen2.5:7b}")
    private String ollamaModel;

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public OllamaApi ollamaApi() {
        return OllamaApi.builder()
                .baseUrl(ollamaBaseUrl)
                .build();
    }

    /**
     * 配置 ChatModel - 根据配置选择模型提供商
     */
    @Bean
    public ChatModel chatModel(OpenAiApi openAiApi, OllamaApi ollamaApi) {
        return switch (modelProvider.toLowerCase()) {
            case "ollama" -> {
                // 本地Ollama模型
                yield OllamaChatModel.builder()
                        .ollamaApi(ollamaApi)
                        .defaultOptions(OllamaChatOptions.builder()
                                .model(ollamaModel)
                                .temperature(0.7)
                                .build())
                        .build();
            }
            case "openai", "default" -> {
                // 阿里云通义千问（OpenAI兼容）
                yield OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .defaultOptions(OpenAiChatOptions.builder()
                                .model(model)
                                .temperature(0.7)
                                .maxTokens(2000)
                                .build())
                        .build();
            }
            default -> throw new IllegalArgumentException("不支持的模型提供商: " + modelProvider);
        };
    }

    /**
     * 配置 Agent Bean
     * 使用 Function Calling 机制调用 MCP 工具
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public Agent agent(
            ChatModel chatModel,
            List<Tool> localTools,
            McpManager mcpManager,
            ObjectMapper objectMapper,
            ToolCacheService toolCacheService) {
        log.info("使用 Function Calling Agent (MCP工具调用)");
        return new FunctionCallingAgent(chatModel, localTools, objectMapper, toolCacheService, mcpManager);
    }
}
