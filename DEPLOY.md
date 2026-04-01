# 雷达监控系统 (Radar Monitor) 部署文档

本文档提供雷达监控系统前后端分离项目的完整部署指南，包括运行环境准备、后端服务构建与运行、前端服务构建以及 Nginx 配置。

## 1. 环境准备 (Prerequisites)

在开始部署之前，请确保目标服务器已安装以下依赖：

- **Java 运行时环境 (JRE/JDK)**: Java 17 或以上版本
- **构建工具**: Maven 3.6+
- **Node.js**: v18.0.0 或以上版本 (前端构建依赖)
- **包管理工具**: npm 或 yarn
- **Web 服务器**: Nginx (用于前端静态资源托管及反向代理)

---

## 2. 后端服务部署 (Backend)

后端服务基于 Spring Boot 构建，使用 Maven 管理依赖。

### 2.1 编译与打包

1. 进入后端项目根目录：
   ```bash
   cd backend
   ```
2. 使用 Maven 进行打包（跳过单元测试）：
   ```bash
   mvn clean package -DskipTests
   ```
   *构建成功后，会在 `backend/target/` 目录下生成类似 `monitor-1.0.0-SNAPSHOT.jar` 的可执行 JAR 包。*

### 2.2 运行服务

可通过以下命令直接启动服务：

```bash
java -jar target/monitor-1.0.0-SNAPSHOT.jar
```

**后台运行建议**：
为了保证服务在后台持续运行且关闭终端不退出，建议使用 `nohup`：
```bash
nohup java -jar target/monitor-1.0.0-SNAPSHOT.jar > app.log 2>&1 &
```
> **注意**: 后端服务默认端口为 `8080`。如需修改，可在 `src/main/resources/application.yml` 或启动时通过参数指定（如 `java -jar app.jar --server.port=8081`）。

---

## 3. 前端服务部署 (Frontend)

前端项目基于 Vue 3 + Vite 构建，打包后为纯静态资源。

### 3.1 环境配置

1. 进入前端项目根目录：
   ```bash
   cd frontend
   ```
2. 环境变量配置：
   请根据生产环境的需要，创建或修改 `.env.production`（或对应的环境文件），配置高德地图等相关密钥：
   ```env
   VITE_AMAP_KEY=你的高德地图API Key
   VITE_AMAP_SECURITY_CODE=你的高德地图安全密钥
   ```

### 3.2 依赖安装与构建打包

1. 安装前端项目依赖：
   ```bash
   npm install
   ```
2. 执行生产环境构建：
   ```bash
   npm run build
   ```
   *构建成功后，前端产物会输出至 `frontend/dist/` 目录中。请记录该目录的绝对路径，稍后需配置到 Nginx 中。*

---

## 4. Nginx 部署与配置 (Nginx Configuration)

前端单页面应用（SPA）建议使用 Nginx 进行静态托管，并同时配置对后端的 API 及 WebSocket 代理。

### 4.1 Nginx 配置文件示例

在 `/etc/nginx/conf.d/` 目录下新建 `radar-monitor.conf`（或直接修改 `nginx.conf` 的 `server` 块），填入以下配置：

```nginx
server {
    # 监听端口
    listen 80;
    # 你的域名或服务器IP
    server_name your_domain_or_ip;

    # 1. 前端静态资源托管
    location / {
        # 替换为 frontend/dist 目录的绝对路径
        root /workspace/frontend/dist;
        index index.html index.htm;
        
        # 解决 Vue Router 的 History 模式刷新 404 问题
        try_files $uri $uri/ /index.html;
    }

    # 2. 后端 HTTP API 接口代理
    # 假设后端接口均以 /api 开头
    location /api/ {
        # 转发至后端服务实际地址
        proxy_pass http://127.0.0.1:8080/api/;
        
        # 传递真实客户端信息
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 3. 后端 WebSocket 代理
    # 假设后端 WebSocket 路径为 /ws
    location /ws/ {
        proxy_pass http://127.0.0.1:8080/ws/;
        
        # 支持 WebSocket 协议升级
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置（按需调整）
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 4.2 重启 Nginx

修改配置并保存后，验证 Nginx 配置语法是否正确并重新加载：

```bash
# 验证配置文件语法
sudo nginx -t

# 重新加载 Nginx 服务
sudo systemctl reload nginx
```

至此，雷达监控系统的前后端均已部署完毕。访问 `http://your_domain_or_ip` 即可打开前端页面，且前后端及 WebSocket 的通信将由 Nginx 正确转发。