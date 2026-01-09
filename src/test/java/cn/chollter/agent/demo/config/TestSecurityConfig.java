package cn.chollter.agent.demo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 测试配置
 * 禁用安全功能以便测试
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityConfig testSecurityConfig() {
        // 返回一个空的配置，不注册过滤器
        return new SecurityConfig(null);
    }
}
