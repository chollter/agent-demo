package cn.chollter.agent.demo.tools;

import cn.chollter.agent.demo.agent.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 计算器工具示例
 */
@Component
public class CalculatorTool implements Tool {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return """
            执行基本的数学计算。
            支持的operation类型：add(加法), subtract(减法), multiply(乘法), divide(除法), power(幂运算)
            参数：a (第一个数字), b (第二个数字), operation (操作类型)
            示例：计算2+3 -> {a: 2, b: 3, operation: "add"}
            """;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            double a = getDouble(parameters, "a");
            double b = getDouble(parameters, "b");
            String operation = getString(parameters, "operation");

            double result = switch (operation.toLowerCase()) {
                case "add" -> a + b;
                case "subtract" -> a - b;
                case "multiply" -> a * b;
                case "divide" -> {
                    if (b == 0) {
                        yield Double.NaN;
                    }
                    yield a / b;
                }
                case "power" -> Math.pow(a, b);
                default -> throw new IllegalArgumentException("Unknown operation: " + operation);
            };

            return String.format("%.2f %s %.2f = %.4f", a, operation, b, result);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private double getDouble(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : "";
    }
}
