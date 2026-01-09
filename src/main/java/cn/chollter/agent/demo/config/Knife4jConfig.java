package cn.chollter.agent.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Knife4j 配置类
 * API 接口文档配置
 *
 * @author Chollter
 * @since 1.0.0
 */
@Configuration
public class Knife4jConfig {

    /**
     * 配置 OpenAPI 文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 接口文档基本信息
                .info(new Info()
                        .title("Agent Demo API 接口文档")
                        .version("1.0.0")
                        .description("""
                                ## Agent Demo 项目接口文档

                                本项目是一个基于 Spring AI 的 AI Agent 演示应用，支持多种 LLM 模型和 MCP 工具调用。

                                ### 主要功能
                                - **任务执行**：提交任务给 AI Agent 执行
                                - **会话管理**：管理和查看对话会话
                                - **执行记录**：查看任务执行历史和结果

                                ### 技术栈
                                - Spring Boot 3.5.9
                                - Spring AI 1.1.2
                                - PostgreSQL + Redis
                                - MCP (Model Context Protocol)
                                """)
                        .contact(new Contact()
                                .name("Chollter")
                                .email("chollter@example.com")
                                .url("https://github.com/chollter"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                // 服务器配置
                .servers(List.of(
                        new Server().url("http://localhost:8090").description("本地开发环境"),
                        new Server().url("https://api.example.com").description("生产环境")
                ));
    }

    /**
     * Agent 相关接口分组
     */
    @Bean
    public GroupedOpenApi agentApi() {
        return GroupedOpenApi.builder()
                .group("01-Agent模块")
                .pathsToMatch("/api/agent/**")
                .build();
    }

    /**
     * 会话相关接口分组
     */
    @Bean
    public GroupedOpenApi conversationApi() {
        return GroupedOpenApi.builder()
                .group("02-会话模块")
                .pathsToMatch("/api/conversations/**")
                .build();
    }

    /**
     * 执行记录相关接口分组
     */
    @Bean
    public GroupedOpenApi executionApi() {
        return GroupedOpenApi.builder()
                .group("03-执行记录模块")
                .pathsToMatch("/api/executions/**")
                .build();
    }

    /**
     * 健康检查接口分组
     */
    @Bean
    public GroupedOpenApi healthApi() {
        return GroupedOpenApi.builder()
                .group("04-系统模块")
                .pathsToMatch("/api/health/**", "/api/info/**")
                .build();
    }
}
