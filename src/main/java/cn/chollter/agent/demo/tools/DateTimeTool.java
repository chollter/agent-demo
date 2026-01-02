package cn.chollter.agent.demo.tools;

import cn.chollter.agent.demo.agent.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 日期时间工具
 */
@Component
public class DateTimeTool implements Tool {

    @Override
    public String getName() {
        return "datetime";
    }

    @Override
    public String getDescription() {
        return """
            获取当前日期和时间信息。
            不需要任何参数。
            """;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "当前时间: " + now.format(formatter);
    }
}
