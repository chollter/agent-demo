package cn.chollter.agent.demo.tools;

import cn.chollter.agent.demo.agent.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * 天气查询工具示例（模拟）
 */
@Component
public class WeatherTool implements Tool {

    private final Random random = new Random();

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getDescription() {
        return """
            查询指定城市的当前天气情况。
            参数：city (城市名称)
            返回：温度、天气状况、湿度等信息
            """;
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String city = getString(parameters);

        if (city == null || city.isEmpty()) {
            return "Error: city parameter is required";
        }

        // 模拟天气数据（实际应用中应该调用真实的天气API）
        int temperature = 15 + random.nextInt(20);
        String[] conditions = {"晴", "多云", "阴", "小雨", "大雨"};
        String condition = conditions[random.nextInt(conditions.length)];
        int humidity = 40 + random.nextInt(50);

        return String.format("%s的天气：温度 %d°C，天气 %s，湿度 %d%%",
            city, temperature, condition, humidity);
    }

    private String getString(Map<String, Object> params) {
        Object value = params.get("city");
        return value != null ? value.toString() : "";
    }
}
