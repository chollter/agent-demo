package cn.chollter.agent.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存配置类
 * 配置 Spring Cache 注解的 Redis 实现
 *
 * @author Chollter
 * @since 1.0.0
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 缓存管理器配置
     * 为 @Cacheable、@CacheEvict 等注解提供支持
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 创建配置了 JavaTimeModule 的 ObjectMapper 用于 Redis 序列化
        // 启用多态类型处理，确保反序列化时能正确还原对象类型
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("cn.chollter.agent.demo")
                .build();

        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 启用默认类型信息，确保反序列化时知道目标类型
        redisObjectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);

        // 默认缓存配置：30 分钟
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper)))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // 会话详情缓存（1小时）
                .withCacheConfiguration("conversations", defaultConfig.entryTtl(Duration.ofHours(1)))
                // 活跃会话列表缓存（5分钟）
                .withCacheConfiguration("activeConversations", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // 最近会话列表缓存（5分钟）
                .withCacheConfiguration("recentConversations", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // 会话统计缓存（5分钟）
                .withCacheConfiguration("conversationStats", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // 执行记录详情缓存（30分钟）
                .withCacheConfiguration("executions", defaultConfig.entryTtl(Duration.ofMinutes(30)))
                // 会话执行记录列表缓存（10分钟）
                .withCacheConfiguration("conversationExecutions", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                // 最近成功执行记录缓存（5分钟）
                .withCacheConfiguration("recentExecutions", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // 执行统计缓存（5分钟）
                .withCacheConfiguration("executionStats", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // Agent 信息缓存（1小时）
                .withCacheConfiguration("agentInfo", defaultConfig.entryTtl(Duration.ofHours(1)))
                .build();
    }
}
