# 雷达上位机显控平台 (V1.0) 设计文档

## 1. 系统架构

本系统采用前后端分离的单体架构，专注于实时接收并展示雷达目标数据。

- **后端**：基于 Spring Boot，负责 UDP 数据接收、解析、状态计算及 WebSocket 实时推送。
- **前端**：基于 Vue 3 (Composition API) 开发单页应用（SPA），集成高德地图 API。

### 数据流向

1. 服务端通过 UDP 向本系统推送 JSON 数据。
2. 后端 `UdpListener` 监听配置端口（支持动态指定），解析并提取 `fusion_geodetic_data` 类型消息。
3. 解析成功后，后端更新内存中的系统状态（最新接收时间、计算帧率等）。
4. 后端通过 WebSocket (`/ws/radar`) 将目标数据和系统状态实时广播给前端。
5. 前端接收到 WebSocket 消息后，更新状态，触发高德地图点位刷新和目标航迹表格重绘。

## 2. 前端设计 (Vue 3)

### 2.1 页面布局

采用**全屏地图 + 悬浮面板**布局：
- **地图层**：高德地图铺满全屏，提供沉浸式监控体验。
- **顶部状态栏**：页面顶部居中或横跨悬浮一个半透明状态栏，展示系统运行状态。
- **右侧面板**：固定在屏幕右侧的半透明面板，包含设置区域和目标航迹表格。支持点击边缘按钮收起/展开，不遮挡地图视野。

### 2.2 核心组件拆分

1. `RadarMap.vue`
   - 职责：初始化高德地图，绘制雷达自身位置（图标），绘制目标位置（不同类型不同图标/颜色），处理地图点位实时刷新（避免闪烁）。
2. `StatusHeader.vue`
   - 职责：展示 UDP 监听状态、雷达在线状态（基于最近接收时间判断）、当前帧率、最新接收时间。根据状态使用颜色区分（绿/黄/红）。
3. `TargetTablePanel.vue`
   - 职责：右侧悬浮面板。顶部包含 UDP 端口配置输入框和连接/断开按钮。下方展示目标航迹数据表格。
   - 功能：展示实时目标列表，支持目标类型筛选，按时间/速度排序，支持虚拟滚动或分页（每页 10 条）。点击某行数据时，通知地图组件定位到该目标。
4. `WebSocketService.js` (或组合式函数 `useRadarData.js`)
   - 职责：管理与后端的 WebSocket 连接，接收数据并分发给各组件。

### 2.3 动态端口监听交互

为了支持用户指定监听端口：
- 前端在 `TargetTablePanel` 顶部提供一个“监听设置”表单，包含端口输入框和“启动监听/停止监听”按钮。
- 用户点击“启动”时，前端通过 HTTP 接口或 WebSocket 消息通知后端开始监听指定端口。
- 只有在监听成功后，后端才会开始推送数据。

## 3. 后端设计 (Spring Boot)

### 3.1 模块划分

```text
com.radar.monitor
├── controller     # 提供HTTP接口，如启动/停止指定端口监听的接口
├── udp            # UDP Socket 监听服务 (UdpListener, UdpServerManager)
├── service        # 核心业务逻辑：JSON 解析、数据校验、状态计算 (DataProcessorService, StatusMonitorService)
├── websocket      # WebSocket 服务端配置与消息推送器 (WebSocketConfig, WebSocketHandler)
├── model          # 数据模型：DTO, VO (RadarData, TargetTrack, SystemStatus)
└── config         # 应用配置：Jackson 配置，跨域配置等
```

### 3.2 核心逻辑

- **UDP 动态监听 (`UdpServerManager`)**：
  - 维护一个或多个 `DatagramSocket` 实例。
  - 提供 `startListening(int port)` 和 `stopListening(int port)` 方法。
  - 启动后，在一个独立线程中循环调用 `socket.receive()`。
- **数据解析与容错 (`DataProcessorService`)**：
  - 使用 Jackson (ObjectMapper) 反序列化 JSON。
  - 捕获 `JsonProcessingException` 等异常，记录日志，保证服务不崩溃。
  - 提取并转换核心字段。将目标类型（如 `PEDESTRIAN`, `CAR`）的英文字符串直接透传或转换为前端易读格式。
- **系统状态计算 (`StatusMonitorService`)**：
  - 记录“监听是否启动”。
  - 每次收到合法包，更新“最后接收时间戳”。
  - 维护一个滑动窗口或简单计数器计算“当前帧率”（例如每秒统计收到的包数）。
  - 定时（如每 1 秒）将合并后的系统状态与最新目标数据通过 WebSocket 推送。

## 4. 接口设计

### 4.1 控制接口 (HTTP)

- **启动 UDP 监听**
  - `POST /api/radar/listen/start`
  - Body: `{ "port": 8888 }`
  - Response: `{ "code": 200, "message": "success", "data": null }`
- **停止 UDP 监听**
  - `POST /api/radar/listen/stop`
  - Body: `{ "port": 8888 }`
  - Response: `{ "code": 200, "message": "success", "data": null }`

### 4.2 数据推送接口 (WebSocket)

- **端点**：`ws://<host>:<port>/ws/radar`
- **推送消息格式 (JSON)**：

```json
{
  "systemStatus": {
    "udpListening": true,
    "listeningPort": 8888,
    "radarOnline": true,
    "fps": 12,
    "lastReceiveTime": "2026-04-01 10:00:00.123"
  },
  "radarData": {
    "timestamp": 1773824375.675,
    "sequence": 14760,
    "egoPosition": {
      "latitude": 25.3100844,
      "longitude": 110.4111849,
      "altitude": 159.72
    },
    "targetCount": 2,
    "tracks": [
      {
        "trackId": 2219,
        "type": "PEDESTRIAN", // 前端将显示为中文：行人
        "position": {
          "latitude": 25.3097296,
          "longitude": 110.4157190,
          "altitude": 159.72
        },
        "distance": 5.000,
        "velocity": 6.57
      }
    ]
  }
}
```

## 5. 异常处理与日志

- **UDP 丢包/超时**：通过 `sequence` 判断丢包（如果差值 > 1），记录警告日志。如果超过设定时间（如 3 秒）未收到数据，将 `radarOnline` 状态置为 `false`。
- **非法 JSON**：捕获解析异常，记录错误日志，不影响下一次接收。
- **地图渲染卡顿**：前端采用节流 (throttle) 或 `requestAnimationFrame` 优化高频更新，确保动画流畅不闪烁。

## 6. 测试与验证策略

- **模拟发送器**：开发一个简单的 Python 或 Java 脚本，循环向指定端口发送测试 JSON 数据，模拟雷达推送。
- **前端测试**：使用 Mock 数据验证表格分页、排序、筛选以及地图点位的点击联动。