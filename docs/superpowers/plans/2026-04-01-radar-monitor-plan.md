# 雷达上位机显控平台 (V1.0) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 开发一套雷达上位机显控平台，包含后端 UDP 数据接收与 WebSocket 推送，以及前端高德地图点位展示和目标航迹表格（带动态端口配置功能）。

**Architecture:** 单体架构，Spring Boot 负责 UDP 监听与 WebSocket 推送；Vue 3 + Vite 负责前端展示（全屏地图+顶部状态栏+右侧悬浮面板）。

**Tech Stack:**
- 后端：Java 17, Spring Boot 3.x, Jackson, WebSocket
- 前端：Vue 3 (Composition API), Vite, TailwindCSS (或基础 CSS), @amap/amap-jsapi-loader, vueuse (可选)

---

### Task 1: 项目初始化与基础配置

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/radar/monitor/RadarMonitorApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `frontend/.env.development`
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`

- [ ] **Step 1: 初始化 Spring Boot 后端项目结构**
在 `backend/pom.xml` 中添加必要的依赖：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.4</version>
    </parent>
    <groupId>com.radar</groupId>
    <artifactId>monitor</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建后端启动类和配置文件**
在 `backend/src/main/java/com/radar/monitor/RadarMonitorApplication.java` 中：
```java
package com.radar.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RadarMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(RadarMonitorApplication.class, args);
    }
}
```
在 `backend/src/main/resources/application.yml` 中：
```yaml
spring:
  profiles:
    active: dev
```
在 `backend/src/main/resources/application-dev.yml` 中：
```yaml
server:
  port: 8080
spring:
  application:
    name: radar-monitor
```

- [ ] **Step 3: 初始化 Vue 3 前端项目与环境变量**
在 `frontend/.env.development` 中添加高德地图配置：
```env
VITE_AMAP_KEY=1bf5b821a81e3a1f81a7006d6bb15e1f
VITE_AMAP_SECURITY_CODE=您的安全密钥(如需要)
```

在 `frontend/package.json` 中：
```json
{
  "name": "radar-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.21",
    "@amap/amap-jsapi-loader": "^1.0.1"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.4",
    "vite": "^5.2.8"
  }
}
```
在 `frontend/vite.config.js` 中：
```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true
      }
    }
  }
})
```

- [ ] **Step 4: 创建前端入口文件**
在 `frontend/index.html` 中：
```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>雷达上位机显控平台</title>
    <style>
      body, html { margin: 0; padding: 0; height: 100%; width: 100%; overflow: hidden; font-family: sans-serif; }
      #app { height: 100%; width: 100%; }
    </style>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```
在 `frontend/src/main.js` 中：
```javascript
import { createApp } from 'vue'
import App from './App.vue'

createApp(App).mount('#app')
```

- [ ] **Step 5: 提交代码**
```bash
git add backend/pom.xml backend/src/ frontend/package.json frontend/vite.config.js frontend/index.html frontend/src/ frontend/.env.development
git commit -m "chore: init spring boot and vue 3 projects"
```

---

### Task 2: 后端数据模型与 WebSocket 配置

**Files:**
- Create: `backend/src/main/java/com/radar/monitor/model/RadarData.java`
- Create: `backend/src/main/java/com/radar/monitor/model/SystemStatus.java`
- Create: `backend/src/main/java/com/radar/monitor/websocket/WebSocketConfig.java`
- Create: `backend/src/main/java/com/radar/monitor/websocket/RadarWebSocketHandler.java`

- [x] **Step 1: 定义数据模型**
在 `backend/src/main/java/com/radar/monitor/model/RadarData.java` 中：
```java
package com.radar.monitor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RadarData {
    private String type;
    private Double timestamp;
    private Header header;
    @JsonProperty("ego_position")
    private Position egoPosition;
    @JsonProperty("target_count")
    private Integer targetCount;
    private List<Track> tracks;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private Long sequence;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Position {
        private Double latitude;
        private Double longitude;
        private Double altitude;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Track {
        @JsonProperty("track_id")
        private Integer trackId;
        private String type;
        private Position position;
        private Double distance;
        private Double velocity;
    }
}
```

在 `backend/src/main/java/com/radar/monitor/model/SystemStatus.java` 中：
```java
package com.radar.monitor.model;

import lombok.Data;

@Data
public class SystemStatus {
    private boolean udpListening = false;
    private Integer listeningPort = null;
    private boolean radarOnline = false;
    private int fps = 0;
    private String lastReceiveTime = "-";
}
```

- [x] **Step 2: 配置 WebSocket Handler**
在 `backend/src/main/java/com/radar/monitor/websocket/RadarWebSocketHandler.java` 中：
```java
package com.radar.monitor.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RadarWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    public void broadcastData(Object data) {
        if (sessions.isEmpty()) return;
        try {
            String json = objectMapper.writeValueAsString(data);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

- [x] **Step 3: 注册 WebSocket**
在 `backend/src/main/java/com/radar/monitor/websocket/WebSocketConfig.java` 中：
```java
package com.radar.monitor.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final RadarWebSocketHandler radarWebSocketHandler;

    public WebSocketConfig(RadarWebSocketHandler radarWebSocketHandler) {
        this.radarWebSocketHandler = radarWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(radarWebSocketHandler, "/ws/radar").setAllowedOrigins("*");
    }
}
```

- [x] **Step 4: 提交代码**
```bash
git add backend/src/main/java/com/radar/monitor/model/ backend/src/main/java/com/radar/monitor/websocket/
git commit -m "feat: add backend models and websocket config"
```

---

### Task 3: 后端 UDP 监听与数据处理

**Files:**
- Create: `backend/src/main/java/com/radar/monitor/service/StatusMonitorService.java`
- Create: `backend/src/main/java/com/radar/monitor/udp/UdpServerManager.java`
- Create: `backend/src/main/java/com/radar/monitor/controller/UdpController.java`

- [x] **Step 1: 实现状态监控与推送服务**
在 `backend/src/main/java/com/radar/monitor/service/StatusMonitorService.java` 中：
```java
package com.radar.monitor.service;

import com.radar.monitor.model.RadarData;
import com.radar.monitor.model.SystemStatus;
import com.radar.monitor.websocket.RadarWebSocketHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StatusMonitorService {
    private final RadarWebSocketHandler webSocketHandler;
    private final SystemStatus systemStatus = new SystemStatus();
    private volatile RadarData latestRadarData = null;
    
    private final AtomicInteger frameCounter = new AtomicInteger(0);
    private long lastReceiveTimestamp = 0;

    public StatusMonitorService(RadarWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public void updateUdpStatus(boolean isListening, Integer port) {
        systemStatus.setUdpListening(isListening);
        systemStatus.setListeningPort(port);
    }

    public void processNewData(RadarData data) {
        latestRadarData = data;
        frameCounter.incrementAndGet();
        lastReceiveTimestamp = System.currentTimeMillis();
        systemStatus.setLastReceiveTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        systemStatus.setRadarOnline(true);
    }

    @Scheduled(fixedRate = 1000)
    public void calculateFpsAndCheckOnline() {
        // Calculate FPS
        systemStatus.setFps(frameCounter.getAndSet(0));
        
        // Check Online Status (offline if no data for 3 seconds)
        if (System.currentTimeMillis() - lastReceiveTimestamp > 3000) {
            systemStatus.setRadarOnline(false);
            systemStatus.setFps(0);
        }
    }

    @Scheduled(fixedRate = 100) // Push every 100ms (10fps max to frontend)
    public void pushDataToFrontend() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("systemStatus", systemStatus);
        payload.put("radarData", latestRadarData); // Can be null if no data yet
        webSocketHandler.broadcastData(payload);
    }
}
```

- [x] **Step 2: 实现 UDP 动态监听器**
在 `backend/src/main/java/com/radar/monitor/udp/UdpServerManager.java` 中：
```java
package com.radar.monitor.udp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radar.monitor.model.RadarData;
import com.radar.monitor.service.StatusMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Component
public class UdpServerManager {
    private static final Logger log = LoggerFactory.getLogger(UdpServerManager.class);
    private final StatusMonitorService statusMonitorService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private DatagramSocket socket;
    private Thread listeningThread;
    private volatile boolean isRunning = false;

    public UdpServerManager(StatusMonitorService statusMonitorService) {
        this.statusMonitorService = statusMonitorService;
    }

    public synchronized boolean startListening(int port) {
        if (isRunning) {
            stopListening();
        }
        try {
            socket = new DatagramSocket(port);
            isRunning = true;
            statusMonitorService.updateUdpStatus(true, port);
            
            listeningThread = new Thread(() -> {
                byte[] buffer = new byte[65535];
                while (isRunning && !socket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String json = new String(packet.getData(), 0, packet.getLength());
                        
                        RadarData data = objectMapper.readValue(json, RadarData.class);
                        if ("fusion_geodetic_data".equals(data.getType())) {
                            statusMonitorService.processNewData(data);
                        }
                    } catch (Exception e) {
                        if (isRunning) {
                            log.error("Error processing UDP packet: {}", e.getMessage());
                        }
                    }
                }
            });
            listeningThread.start();
            return true;
        } catch (Exception e) {
            log.error("Failed to start UDP listener on port {}", port, e);
            return false;
        }
    }

    public synchronized void stopListening() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        statusMonitorService.updateUdpStatus(false, null);
    }
}
```

- [x] **Step 3: 提供控制 API**
在 `backend/src/main/java/com/radar/monitor/controller/UdpController.java` 中：
```java
package com.radar.monitor.controller;

import com.radar.monitor.udp.UdpServerManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/radar")
public class UdpController {
    private final UdpServerManager udpServerManager;

    public UdpController(UdpServerManager udpServerManager) {
        this.udpServerManager = udpServerManager;
    }

    @PostMapping("/listen/start")
    public Map<String, Object> startListening(@RequestBody Map<String, Integer> payload) {
        Integer port = payload.get("port");
        boolean success = udpServerManager.startListening(port != null ? port : 8888);
        Map<String, Object> response = new HashMap<>();
        response.put("code", success ? 200 : 500);
        response.put("message", success ? "success" : "failed to bind port");
        return response;
    }

    @PostMapping("/listen/stop")
    public Map<String, Object> stopListening() {
        udpServerManager.stopListening();
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        return response;
    }
}
```

- [x] **Step 4: 提交代码**
```bash
git add backend/src/main/java/com/radar/monitor/service/ backend/src/main/java/com/radar/monitor/udp/ backend/src/main/java/com/radar/monitor/controller/
git commit -m "feat: add UDP listener and status processing logic"
```

---

### Task 4: 前端核心状态与 App 布局结构

**Files:**
- Create: `frontend/src/composables/useRadarData.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/components/StatusHeader.vue`
- Create: `frontend/src/components/TargetTablePanel.vue`
- Create: `frontend/src/components/RadarMap.vue`

- [x] **Step 1: 创建 WebSocket 状态管理 Hook**
在 `frontend/src/composables/useRadarData.js` 中：
```javascript
import { ref, onMounted, onUnmounted } from 'vue'

export function useRadarData() {
  const systemStatus = ref({
    udpListening: false,
    listeningPort: null,
    radarOnline: false,
    fps: 0,
    lastReceiveTime: '-'
  })
  const radarData = ref(null)
  let ws = null

  const connectWs = () => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/radar`
    
    ws = new WebSocket(wsUrl)
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      if (data.systemStatus) systemStatus.value = data.systemStatus
      if (data.radarData) radarData.value = data.radarData
    }
    ws.onclose = () => {
      setTimeout(connectWs, 3000) // Auto reconnect
    }
  }

  onMounted(() => {
    connectWs()
  })

  onUnmounted(() => {
    if (ws) ws.close()
  })

  return { systemStatus, radarData }
}
```

- [x] **Step 2: 创建主布局 App.vue**
在 `frontend/src/App.vue` 中：
```vue
<template>
  <div class="app-container">
    <RadarMap :radarData="radarData" class="map-layer" />
    <StatusHeader :status="systemStatus" class="status-layer" />
    <TargetTablePanel :status="systemStatus" :radarData="radarData" class="panel-layer" />
  </div>
</template>

<script setup>
import { useRadarData } from './composables/useRadarData.js'
import RadarMap from './components/RadarMap.vue'
import StatusHeader from './components/StatusHeader.vue'
import TargetTablePanel from './components/TargetTablePanel.vue'

const { systemStatus, radarData } = useRadarData()
</script>

<style scoped>
.app-container {
  position: relative;
  width: 100vw;
  height: 100vh;
}
.map-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}
.status-layer {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}
.panel-layer {
  position: absolute;
  top: 20px;
  right: 20px;
  bottom: 20px;
  width: 400px;
  z-index: 10;
}
</style>
```

- [x] **Step 3: 提交代码**
```bash
git add frontend/src/composables/ frontend/src/App.vue
git commit -m "feat: add frontend websocket hook and layout structure"
```

---

### Task 5: 前端系统状态与表格面板组件

**Files:**
- Update: `frontend/src/components/StatusHeader.vue`
- Update: `frontend/src/components/TargetTablePanel.vue`

- [ ] **Step 1: 实现 StatusHeader 组件**
在 `frontend/src/components/StatusHeader.vue` 中：
```vue
<template>
  <div class="status-header">
    <div class="status-item">
      <span>UDP监听:</span>
      <span :class="status.udpListening ? 'text-green' : 'text-red'">
        {{ status.udpListening ? `已启动 (${status.listeningPort})` : '未启动' }}
      </span>
    </div>
    <div class="status-item">
      <span>雷达状态:</span>
      <span :class="status.radarOnline ? 'text-green' : 'text-red'">
        {{ status.radarOnline ? '在线' : '离线' }}
      </span>
    </div>
    <div class="status-item">
      <span>帧率:</span>
      <span :class="status.fps >= 10 ? 'text-green' : (status.fps > 0 ? 'text-yellow' : 'text-red')">
        {{ status.fps }} fps
      </span>
    </div>
    <div class="status-item">
      <span>最新数据:</span>
      <span>{{ status.lastReceiveTime }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  status: { type: Object, required: true }
})
</script>

<style scoped>
.status-header {
  display: flex;
  gap: 20px;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(8px);
  color: white;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  border: 1px solid rgba(255,255,255,0.1);
}
.status-item { display: flex; gap: 8px; }
.text-green { color: #4ade80; }
.text-yellow { color: #facc15; }
.text-red { color: #f87171; }
</style>
```

- [ ] **Step 2: 实现 TargetTablePanel 组件 (带控制面板)**
在 `frontend/src/components/TargetTablePanel.vue` 中：
```vue
<template>
  <div class="panel-container" :class="{ 'collapsed': isCollapsed }">
    <div class="toggle-btn" @click="isCollapsed = !isCollapsed">
      {{ isCollapsed ? '◀' : '▶' }}
    </div>
    
    <div class="panel-content" v-show="!isCollapsed">
      <!-- 控制区 -->
      <div class="control-section">
        <h3>设置</h3>
        <div class="input-group">
          <input type="number" v-model="portInput" placeholder="UDP端口 (如 8888)" :disabled="status.udpListening" />
          <button v-if="!status.udpListening" @click="startListen" class="btn btn-primary">启动监听</button>
          <button v-else @click="stopListen" class="btn btn-danger">停止监听</button>
        </div>
      </div>

      <!-- 表格区 -->
      <div class="table-section">
        <h3>目标列表 ({{ radarData?.tracks?.length || 0 }})</h3>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>类别</th>
                <th>速度(m/s)</th>
                <th>距离(m)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="track in radarData?.tracks" :key="track.track_id">
                <td>{{ track.track_id }}</td>
                <td>{{ translateType(track.type) }}</td>
                <td>{{ track.velocity.toFixed(2) }}</td>
                <td>{{ track.distance.toFixed(2) }}</td>
              </tr>
              <tr v-if="!radarData?.tracks || radarData.tracks.length === 0">
                <td colspan="4" class="empty-text">暂无目标数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  status: Object,
  radarData: Object
})

const isCollapsed = ref(false)
const portInput = ref(8888)

const translateType = (type) => {
  const map = { 'PEDESTRIAN': '行人', 'CAR': '汽车', 'BICYCLE': '自行车', 'TRUCK': '卡车' }
  return map[type] || type || '未知'
}

const startListen = async () => {
  if (!portInput.value) return
  await fetch('/api/radar/listen/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ port: portInput.value })
  })
}

const stopListen = async () => {
  await fetch('/api/radar/listen/stop', { method: 'POST' })
}
</script>

<style scoped>
.panel-container {
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  color: white;
  display: flex;
  flex-direction: row;
  transition: transform 0.3s;
}
.panel-container.collapsed {
  transform: translateX(calc(100% - 30px));
}
.toggle-btn {
  width: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: rgba(255,255,255,0.1);
  border-radius: 8px 0 0 8px;
}
.panel-content {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow: hidden;
}
h3 { margin: 0 0 10px 0; font-size: 16px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 8px;}
.input-group { display: flex; gap: 8px; }
input { flex: 1; padding: 6px; background: rgba(0,0,0,0.5); border: 1px solid #444; color: white; border-radius: 4px;}
.btn { padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; color: white; }
.btn-primary { background: #3b82f6; }
.btn-danger { background: #ef4444; }
.table-wrapper { overflow-y: auto; flex: 1; max-height: calc(100vh - 200px); }
table { width: 100%; border-collapse: collapse; font-size: 14px; text-align: left;}
th, td { padding: 8px; border-bottom: 1px solid rgba(255,255,255,0.05); }
th { color: #94a3b8; font-weight: normal; }
.empty-text { text-align: center; color: #64748b; padding: 20px;}
</style>
```

- [ ] **Step 3: 提交代码**
```bash
git add frontend/src/components/StatusHeader.vue frontend/src/components/TargetTablePanel.vue
git commit -m "feat: add frontend status and table components"
```

---

### Task 6: 前端高德地图组件集成

**Files:**
- Update: `frontend/src/components/RadarMap.vue`

- [ ] **Step 1: 实现 RadarMap 组件**
*注意：这里需要一个免费的高德地图 Key。由于是测试台，代码里提供一个占位符，执行时替换或先使用默认测试 key。*

在 `frontend/src/components/RadarMap.vue` 中：
```vue
<template>
  <div id="map-container"></div>
</template>

<script setup>
import { onMounted, watch, ref, shallowRef } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const props = defineProps({
  radarData: Object
})

const map = shallowRef(null)
const markers = ref(new Map()) // trackId -> AMap.Marker
const egoMarker = shallowRef(null)

onMounted(() => {
  window._AMapSecurityConfig = {
    securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE,
  }
  AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY,
    version: '2.0',
  }).then((AMap) => {
    map.value = new AMap.Map('map-container', {
      zoom: 18,
      mapStyle: 'amap://styles/darkblue'
    })
  }).catch(e => console.error(e))
})

watch(() => props.radarData, (newData) => {
  if (!map.value || !newData) return
  const AMap = window.AMap

  // 1. 更新雷达位置 (Ego)
  if (newData.ego_position && !egoMarker.value) {
    egoMarker.value = new AMap.Marker({
      position: [newData.ego_position.longitude, newData.ego_position.latitude],
      icon: new AMap.Icon({
        size: new AMap.Size(32, 32),
        image: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-red.png'
      }),
      title: '雷达设备'
    })
    map.value.add(egoMarker.value)
    map.value.setCenter([newData.ego_position.longitude, newData.ego_position.latitude])
  }

  // 2. 更新目标点位
  const currentIds = new Set()
  if (newData.tracks) {
    newData.tracks.forEach(track => {
      currentIds.add(track.track_id)
      const position = [track.position.longitude, track.position.latitude]
      
      if (markers.value.has(track.track_id)) {
        // 存在则移动
        markers.value.get(track.track_id).setPosition(position)
      } else {
        // 新增目标
        const marker = new AMap.Marker({
          position: position,
          title: `ID: ${track.track_id} | ${track.type}`,
          // 这里可以使用不同颜色区分不同类型，简单起见用默认标记
        })
        map.value.add(marker)
        markers.value.set(track.track_id, marker)
      }
    })
  }

  // 3. 清理消失的目标
  for (let [id, marker] of markers.value.entries()) {
    if (!currentIds.has(id)) {
      map.value.remove(marker)
      markers.value.delete(id)
    }
  }
}, { deep: true })
</script>

<style scoped>
#map-container {
  width: 100%;
  height: 100%;
}
</style>
```

- [ ] **Step 2: 提交代码**
```bash
git add frontend/src/components/RadarMap.vue
git commit -m "feat: add amap integration and real-time markers"
```
