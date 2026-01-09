package cn.chollter.agent.demo.exception;

import cn.chollter.agent.demo.dto.TaskResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中的所有异常，返回标准化的错误响应
 *
 * @author Chollter
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理方法参数验证异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<TaskResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        String correlationId = UUID.randomUUID().toString();
        String requestPath = getPath(request);

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("[{}] 验证失败 - 路径: {}, 错误: {}",
                correlationId, requestPath, errors);

        TaskResponse response = TaskResponse.builder()
                .success(false)
                .errorMessage(errors)
                .errorCode("VALIDATION_ERROR")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 处理约束违反异常（@RequestParam 等）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<TaskResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request
    ) {
        String correlationId = UUID.randomUUID().toString();
        String requestPath = getPath(request);

        String errors = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.warn("[{}] 验证失败 - 路径: {}, 错误: {}",
                correlationId, requestPath, errors);

        TaskResponse response = TaskResponse.builder()
                .success(false)
                .errorMessage(errors)
                .errorCode("VALIDATION_ERROR")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 处理 AgentException 及其子类
     */
    @ExceptionHandler(AgentException.class)
    public ResponseEntity<TaskResponse> handleAgentException(
            AgentException ex,
            WebRequest request
    ) {
        String correlationId = UUID.randomUUID().toString();
        String requestPath = getPath(request);

        log.error("[{}] Agent 错误 - 错误码: {}, 路径: {}, 详情: {}",
                correlationId, ex.getErrorCode(), requestPath, ex.getDetail(), ex);

        TaskResponse response = TaskResponse.builder()
                .success(false)
                .errorMessage(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * 处理验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<TaskResponse> handleValidationException(
            ValidationException ex,
            WebRequest request
    ) {
        String correlationId = UUID.randomUUID().toString();
        String requestPath = getPath(request);

        log.warn("[{}] 验证失败 - 路径: {}, 错误: {}",
                correlationId, requestPath, String.join("; ", ex.getValidationErrors()));

        TaskResponse response = TaskResponse.builder()
                .success(false)
                .errorMessage(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 处理 LLM 连接异常
     */
    @ExceptionHandler(LlmConnectionException.class)
    public ResponseEntity<TaskResponse> handleLlmConnectionException(
            LlmConnectionException ex,
            WebRequest request
    ) {
        String correlationId = UUID.randomUUID().toString();
        String requestPath = getPath(request);

        log.error("[{}] LLM 连接失败 - 路径: {}",
                correlationId, requestPath, ex);

        TaskResponse response = TaskResponse.builder()
                .success(false)
                .errorMessage("AI 服务暂时不可用，请稍后重试")
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    /**
     * 处理工具执行异常
     */
    @ExceptionHandler(ToolExecutionException.class)
    public ResponseEntity<TaskResponse> handleToolExecutionException(
            ToolExecutionException ex,
            WebRequest request
    ) {
        String correlationId = UUID.randomUUID().toString();
        String requestPath = getPath(request);

        log.error("[{}] 工具执行失败 - 路径: {}",
                correlationId, requestPath, ex);

        TaskResponse response = TaskResponse.builder()
                .success(false)
                .errorMessage(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * 处理未预期的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<TaskResponse> handleUnhandledException(
            Exception ex,
            WebRequest request
    ) {
        String correlationId = UUID.randomUUID().toString();
        String requestPath = getPath(request);

        log.error("[{}] 未预期的错误 - 路径: {}",
                correlationId, requestPath, ex);

        TaskResponse response = TaskResponse.builder()
                .success(false)
                .errorMessage("服务器内部错误，请联系管理员")
                .errorCode("INTERNAL_ERROR")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
