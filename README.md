# AI Agent Demo

基于Spring AI和阿里云通义千问的简单AI Agent框架实现。

## 项目简介

这是一个使用ReAct（Reasoning + Acting）模式的AI Agent实现。Agent能够：
- 使用工具执行任务
- 进行推理和决策
- 展示完整的思考过程
- 使用阿里云通义千问大模型

## 技术栈

- Java 21
- Spring Boot 3.5.9
- Spring AI 1.1.2
- 阿里云通义千问 (qwen-max)

## 可用工具

1. **calculator** - 数学计算工具
   - 支持加、减、乘、除、幂运算
   - 示例：计算2+3

2. **weather** - 天气查询工具（模拟）
   - 查询指定城市的天气
   - 示例：查询北京的天气

3. **search** - 搜索工具（模拟）
   - 搜索相关信息
   - 示例：搜索AI相关内容

4. **datetime** - 日期时间工具
   - 获取当前时间

## 快速开始

### 1. 获取阿里云 API Key

访问 [阿里云百炼平台](https://bailian.console.aliyun.com/) 获取API Key。

### 2. 配置 API Key

在 `application.yml` 中配置：
```yaml
spring:
  ai:
    openai:
      api-key: your-actual-api-key
```

或使用环境变量（推荐）：
```bash
export ALIYUN_API_KEY=your-actual-api-key
```

### 3. 编译并运行

```bash
mvn clean install
mvn spring-boot:run
```

或直接运行 JAR：
```bash
java -jar target/agent-demo-0.0.1-SNAPSHOT.jar
```

## API接口

应用启动后，访问 `http://localhost:8080`

### 1. 健康检查

```bash
GET http://localhost:8080/api/agent/health
```

### 2. 获取Agent信息

```bash
GET http://localhost:8080/api/agent/info
```

### 3. 执行任务（POST）

```bash
POST http://localhost:8080/api/agent/execute
Content-Type: application/json

{
  "task": "帮我计算25乘以36等于多少"
}
```

### 4. 执行任务（GET）

```bash
GET http://localhost:8080/api/agent/execute?task=帮我计算25乘以36等于多少
```

## 使用示例

### 示例1：数学计算

**请求：**
```json
{
  "task": "帮我计算25乘以36，再加上100"
}
```

**响应：**
```json
{
  "success": true,
  "finalAnswer": "25 × 36 = 900.00",
  "thoughtSteps": [
    {
      "stepType": "THOUGHT",
      "content": "用户需要进行数学计算，需要先计算25乘以36"
    },
    {
      "stepType": "ACTION",
      "content": "使用工具: calculator"
    },
    {
      "stepType": "OBSERVATION",
      "content": "25.00 multiply 36.00 = 900.0000"
    }
  ]
}
```

### 示例2：天气查询

**请求：**
```json
{
  "task": "北京今天天气怎么样？"
}
```

**响应：**
```json
{
  "success": true,
  "finalAnswer": "北京今天的天气温度适中，具体信息可以参考天气查询结果",
  "thoughtSteps": [
    {
      "stepType": "THOUGHT",
      "content": "用户想了解北京的天气，需要使用天气查询工具"
    },
    {
      "stepType": "ACTION",
      "content": "使用工具: weather"
    },
    {
      "stepType": "OBSERVATION",
      "content": "北京的天气：温度 22°C，天气 多云，湿度 65%"
    }
  ]
}
```

### 示例3：复合任务

**请求：**
```json
{
  "task": "告诉我现在的时间，然后帮我计算123除以3等于多少"
}
```

**响应：**
```json
{
  "success": true,
  "finalAnswer": "当前时间是2026-01-01 10:30:15，123除以3等于41.00",
  "thoughtSteps": [
    {
      "stepType": "ACTION",
      "content": "使用工具: datetime"
    },
    {
      "stepType": "OBSERVATION",
      "content": "当前时间: 2026-01-01 10:30:15"
    },
    {
      "stepType": "ACTION",
      "content": "使用工具: calculator"
    },
    {
      "stepType": "OBSERVATION",
      "content": "123.00 divide 3.00 = 41.0000"
    }
  ]
}
```

## 测试工具

### 使用cURL测试

```bash
# 健康检查
curl http://localhost:8080/api/agent/health

# 执行数学计算
curl "http://localhost:8080/api/agent/execute?task=计算100除以4等于多少"

# 查询天气
curl -X POST http://localhost:8080/api/agent/execute \
  -H "Content-Type: application/json" \
  -d '{"task": "上海的天气怎么样？"}'
```

### 使用Postman测试

导入以下请求到Postman：

1. **健康检查**
   - Method: GET
   - URL: http://localhost:8080/api/agent/health

2. **执行任务**
   - Method: POST
   - URL: http://localhost:8080/api/agent/execute
   - Headers: Content-Type: application/json
   - Body (raw JSON):
   ```json
   {
     "task": "你的任务内容"
   }
   ```

## 项目结构

```
src/main/java/cn/chollter/agent/demo/
├── agent/              # Agent核心接口和模型
│   ├── Agent.java
│   ├── AgentResponse.java
│   ├── Message.java
│   ├── ThoughtStep.java
│   └── Tool.java
├── config/             # 配置类
│   └── AgentConfig.java
├── controller/         # REST控制器
│   └── AgentController.java
├── core/               # Agent核心实现
│   └── ReActAgent.java
├── dto/                # 数据传输对象
│   ├── TaskRequest.java
│   └── TaskResponse.java
├── service/            # 服务层
│   └── AgentService.java
└── tools/              # 工具实现
    ├── CalculatorTool.java
    ├── DateTimeTool.java
    ├── SearchTool.java
    └── WeatherTool.java
```

## ReAct模式说明

ReAct (Reasoning + Acting) 是一种让Agent进行推理和行动的模式：

1. **Thought (思考)**: Agent分析当前情况，决定下一步行动
2. **Action (行动)**: Agent选择并执行一个工具
3. **Observation (观察)**: Agent观察工具执行的结果
4. **循环**: 重复上述步骤，直到能够给出最终答案

这种模式让Agent能够展示完整的推理过程，更加透明和可控。

## 扩展开发

### 添加新工具

1. 创建一个类实现 `Tool` 接口：

```java
@Component
public class MyCustomTool implements Tool {

    @Override
    public String getName() {
        return "myTool";
    }

    @Override
    public String getDescription() {
        return "工具描述";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        // 实现工具逻辑
        return "执行结果";
    }
}
```

2. 将类标注为 `@Component`，Spring会自动注册到工具列表中

### 自定义Agent

实现 `Agent` 接口创建自定义的Agent：

```java
@Component
public class MyCustomAgent implements Agent {
    // 实现Agent接口方法
}
```

## 模型配置

项目使用阿里云通义千问，支持以下模型：

- **qwen-max** (默认) - 最强模型，适合复杂任务
- **qwen-plus** - 性价比高
- **qwen-turbo** - 速度快，成本低

在 `application.yml` 中修改：
```yaml
spring:
  ai:
    openai:
      chat:
        model: qwen-max  # 可选: qwen-plus, qwen-turbo
```

## 注意事项

1. 需要有效的阿里云API Key
2. API调用会产生费用，请注意控制成本
3. 确保网络连接稳定
4. 工具执行需要考虑异常处理和超时
5. 复杂任务可能需要较长时间，注意设置合理的超时时间

## 许可证

MIT License
