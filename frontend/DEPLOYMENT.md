# 前端部署指南

## 开发环境

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问：http://localhost:3000

## 生产构建

### 构建

```bash
npm run build
```

构建产物输出到 `dist/` 目录

### 预览构建

```bash
npm run preview
```

## Nginx 部署

### 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /var/www/agent-demo/frontend/dist;
    index index.html;

    # SPA 路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api {
        proxy_pass http://localhost:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
```

### 部署步骤

```bash
# 1. 构建前端
npm run build

# 2. 上传到服务器
scp -r dist/* user@server:/var/www/agent-demo/frontend/

# 3. 重启 Nginx
sudo nginx -s reload
```

## Docker 部署

### Dockerfile

```dockerfile
# 构建阶段
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 生产阶段
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 构建镜像

```bash
docker build -t agent-demo-frontend:latest .
```

### 运行容器

```bash
docker run -d \
  --name agent-frontend \
  -p 80:80 \
  agent-demo-frontend:latest
```

## Vercel 部署

### 安装 Vercel CLI

```bash
npm install -g vercel
```

### 部署

```bash
vercel
```

### 环境变量

在 Vercel 控制台设置：

```
VITE_API_BASE_URL=https://your-backend-api.com
```

## Netlify 部署

### netlify.toml

```toml
[build]
  command = "npm run build"
  publish = "dist"

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200

[build.environment]
  NODE_VERSION = "18"
```

### 部署

```bash
netlify deploy --prod
```

## 性能优化

### 1. 代码分割

```tsx
import { lazy } from 'react'

const HeavyComponent = lazy(() => import('./HeavyComponent'))
```

### 2. 图片优化

使用 WebP 格式，添加响应式图片

### 3. 缓存策略

- 静态资源：1 年
- HTML：不缓存
- API 响应：根据内容动态设置

## 监控

### 添加分析

```tsx
// 在 App.tsx 中添加
useEffect(() => {
  if (import.meta.env.PROD) {
    // Google Analytics
    // 或其他分析工具
  }
}, [])
```

## 故障排查

### 构建失败

```bash
# 清除缓存
rm -rf node_modules
rm package-lock.json
npm install

# 重新构建
npm run build
```

### API 调用失败

1. 检查 `.env` 配置
2. 检查后端服务是否运行
3. 检查 CORS 配置
4. 查看浏览器控制台错误信息

### 路由问题

确保服务器配置 SPA 路由：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```
