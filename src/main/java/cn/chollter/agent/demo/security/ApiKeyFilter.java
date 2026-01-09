package cn.chollter.agent.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * API Key 认证过滤器
 * 验证请求头中的 API Key
 *
 * @author Chollter
 * @since 1.0.0
 */
@Slf4j
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${agent.security.api-key:#{null}}")
    private String configuredApiKey;

    @Value("${agent.security.enabled:true}")
    private boolean securityEnabled;

    /**
     * 不需要认证的路径
     */
    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/agent/health",
            "/actuator",
            "/error"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 如果安全功能未启用，直接放行
        if (!securityEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 检查是否是排除路径
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取请求头中的 API Key
        String requestApiKey = request.getHeader(API_KEY_HEADER);

        // 验证 API Key
        if (isValidApiKey(requestApiKey)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("API Key 验证失败 - 路径: {}, IP: {}", path, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Missing or invalid API key\", \"errorCode\": \"INVALID_API_KEY\"}");
        }
    }

    /**
     * 验证 API Key
     */
    private boolean isValidApiKey(String apiKey) {
        // 如果没有配置 API Key，则拒绝所有请求（强制要求配置）
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            log.error("API Key 未配置，请设置 agent.security.api-key 配置项");
            return false;
        }

        // 验证请求的 API Key
        return configuredApiKey.equals(apiKey);
    }

    /**
     * 检查是否是排除路径
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
    }
}
