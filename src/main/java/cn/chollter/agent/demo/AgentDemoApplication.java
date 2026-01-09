package cn.chollter.agent.demo;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentDemoApplication {

    public static void main(String[] args) {
        // 在 Spring Boot 启动前加载 .env 文件中的环境变量
        loadDotenv();
        SpringApplication.run(AgentDemoApplication.class, args);
    }

    private static void loadDotenv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            // 将 dotenv 中的变量设置为系统属性
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });

            System.out.println("Environment variables loaded from .env file");
        } catch (Exception e) {
            System.out.println("Warning: Failed to load .env file: " + e.getMessage());
        }
    }

}
