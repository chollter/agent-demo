# AI Agent Demo

一个基于 ReAct 模式的 AI Agent 演示项目，包含完整的后端服务和现代化前端界面。

## 项目结构

```
agent-demo/
├── backend/                # 后端服务（Spring Boot）
│   ├── src/
│   ├── pom.xml
│   └── ...
├── frontend/               # 前端应用（React + Vite）
│   ├── src/
│   ├── package.json
│   └── ...
└── README.md
```

## 快速开始

### 后端服务

```bash
# 进入后端目录
cd backend

# 启动后端服务（默认端口 8090）
mvn spring-boot:run
```

### 前端应用

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器（默认端口 3000）
npm run dev
```

### 访问应用

打开浏览器访问：http://localhost:3000

## 功能特性

### 后端（Spring Boot）

- ✅ ReAct Agent 实现
- ✅ 多模型支持（阿里云通义千问 + 本地 Ollama）
- ✅ 可插拔工具系统
- ✅ MCP 协议支持
- ✅ 对话持久化（H2 + JPA）
- ✅ API 认证（API Key）
- ✅ 输入验证
- ✅ 异常处理
- ✅ 重试机制

详细文档：[后端文档](./backend/README.md)

### 前端（React）

- 💬 实时对话界面
- 📝 对话历史管理
- 💭 思考过程展示
- 🎨 Markdown 渲染
- 🌗 代码高亮
- 🔐 API Key 管理
- 📱 响应式设计

详细文档：[前端文档](./frontend/README.md)

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5.9
- Spring AI 1.1.2
- H2 Database / PostgreSQL
- Maven

### 前端

- React 18 + TypeScript
- Vite 5
- Ant Design 5
- react-markdown
- axios

## 环境配置

### 后端配置

创建 `backend/src/main/resources/application-local.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: ${AI_API_KEY}  # 从环境变量读取
```

设置环境变量：
```bash
export AI_API_KEY=your-api-key
```

### 前端配置

创建 `frontend/.env`：

```bash
VITE_API_BASE_URL=http://localhost:8090
```

## 使用示例

### 1. 数学计算

```
用户: 帮我计算 25 × 36 等于多少
Agent: 25 × 36 = 900
```

### 2. 天气查询

```
用户: 北京今天天气怎么样？
Agent: 北京今天的天气温度适中，具体信息可以参考天气查询结果
```

### 3. 复合任务

```
用户: 告诉我现在的时间，然后帮我计算 123 除以 3
Agent: 当前时间是 2026-01-06 12:00:00，123 除以 3 等于 41
```

## 开发指南

### 添加新工具（后端）

```java
@Component
public class MyTool implements Tool {
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
        return "结果";
    }
}
```

### 添加新页面（前端）

```tsx
// src/components/MyPage.tsx
export default function MyPage() {
  return <div>My Page</div>
}
```

## 测试

### 后端测试

```bash
cd backend
mvn test
```

### 前端测试

```bash
cd frontend
npm run test
```

## 部署

### Docker 部署

```bash
# 构建并启动所有服务
docker-compose up -d
```

### 手动部署

详见部署文档：[PROJECT_GUIDE.md](./PROJECT_GUIDE.md)

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 联系方式

- **项目维护者**: Chollter
- **GitHub**: https://github.com/your-org/agent-demo
