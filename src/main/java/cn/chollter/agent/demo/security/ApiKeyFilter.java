package cn.chollter.agent.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API Key 认证过滤器
 * 验证请求头中的 API Key，增强版安全特性：
 * - 强制要求配置 API Key（无默认值）
 * - 支持 API Key 轮换（多个有效 key）
 * - API Key 格式验证（前缀检查）
 * - 失败尝试记录和临时封禁
 *
 * @author Chollter
 * @since 1.0.0
 */
@Slf4j
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_PREFIX = "sk-";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    @Value("${agent.security.api-key:#{null}}")
    private String configuredApiKey;

    @Value("${agent.security.api-key-secondary:#{null}}")
    private String secondaryApiKey;

    @Value("${agent.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${agent.security.require-prefix:true}")
    private boolean requirePrefix;

    private final RedisTemplate<String, Object> redisTemplate;

    public ApiKeyFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 不需要认证的路径
     */
    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/agent/health",
            "/actuator/health",
            "/actuator/prometheus",
            "/doc.html",
            "/error"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 如果安全功能未启用，直接放行（仅用于开发环境）
        if (!securityEnabled) {
            log.warn("安全功能已禁用，生产环境请启用！");
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        // 检查是否是排除路径
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 检查 IP 是否被临时封禁
        if (isIpBlocked(clientIp)) {
            log.warn("IP 被封禁 - IP: {}, 路径: {}", clientIp, path);
            response.setStatus(429);  // 429 Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Too many failed attempts, please try again later\", \"errorCode\": \"IP_BLOCKED\"}");
            return;
        }

        // 获取请求头中的 API Key
        String requestApiKey = request.getHeader(API_KEY_HEADER);

        // 验证 API Key
        if (isValidApiKey(requestApiKey)) {
            // 验证成功，清除失败计数
            clearFailedAttempts(clientIp);
            filterChain.doFilter(request, response);
        } else {
            // 验证失败，记录并检查是否需要封禁
            handleFailedAuthentication(clientIp, path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Missing or invalid API key\", \"errorCode\": \"INVALID_API_KEY\"}");
        }
    }

    /**
     * 验证 API Key（增强版）
     */
    private boolean isValidApiKey(String apiKey) {
        // 检查是否为空
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("API Key 为空");
            return false;
        }

        // 检查前缀（如果启用）
        if (requirePrefix && !apiKey.startsWith(API_KEY_PREFIX)) {
            log.warn("API Key 前缀错误: {}", apiKey.substring(0, Math.min(10, apiKey.length())));
            return false;
        }

        // 检查是否配置了主 API Key
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            log.error("API Key 未配置！请通过环境变量或配置文件设置 agent.security.api-key");
            return false;
        }

        // 验证主 API Key 或备用 API Key（支持轮换）
        boolean isValid = configuredApiKey.equals(apiKey) ||
                (secondaryApiKey != null && !secondaryApiKey.isBlank() && secondaryApiKey.equals(apiKey));

        if (!isValid) {
            log.warn("API Key 验证失败: {}", apiKey.substring(0, Math.min(10, apiKey.length())));
        }

        return isValid;
    }

    /**
     * 处理认证失败
     */
    private void handleFailedAuthentication(String clientIp, String path) {
        String key = "auth:failed:" + clientIp;
        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts == 1) {
            // 第一次失败，设置过期时间
            redisTemplate.expire(key, BLOCK_DURATION.toSeconds(), java.util.concurrent.TimeUnit.SECONDS);
        }

        log.warn("API Key 验证失败 - 路径: {}, IP: {}, 失败次数: {}", path, clientIp, attempts);

        // 如果超过最大失败次数，封禁 IP
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            blockIp(clientIp);
        }
    }

    /**
     * 封禁 IP
     */
    private void blockIp(String clientIp) {
        String blockKey = "auth:blocked:" + clientIp;
        redisTemplate.opsForValue().set(blockKey, "true", BLOCK_DURATION);
        log.error("IP 已被封禁 - IP: {}, 封禁时长: {} 分钟", clientIp, BLOCK_DURATION.toMinutes());
    }

    /**
     * 检查 IP 是否被封禁
     */
    private boolean isIpBlocked(String clientIp) {
        String blockKey = "auth:blocked:" + clientIp;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
    }

    /**
     * 清除失败尝试计数
     */
    private void clearFailedAttempts(String clientIp) {
        String key = "auth:failed:" + clientIp;
        redisTemplate.delete(key);
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个 IP 的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 检查是否是排除路径
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
    }
}
