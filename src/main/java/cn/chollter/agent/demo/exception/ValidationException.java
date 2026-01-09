package cn.chollter.agent.demo.exception;

import lombok.Getter;

import java.util.List;

/**
 * 验证异常
 * 当输入验证失败时抛出
 *
 * @author Chollter
 * @since 1.0.0
 */
@Getter
public class ValidationException extends AgentException {

    private final List<String> validationErrors;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.validationErrors = List.of(message);
    }

    public ValidationException(String fieldName, String message) {
        super("VALIDATION_ERROR",
                String.format("字段 '%s' 验证失败: %s", fieldName, message),
                message);
        this.validationErrors = List.of(String.format("%s: %s", fieldName, message));
    }

    public ValidationException(List<String> validationErrors) {
        super("VALIDATION_ERROR",
                String.format("验证失败，共 %d 个错误", validationErrors.size()));
        this.validationErrors = validationErrors;
    }

}
