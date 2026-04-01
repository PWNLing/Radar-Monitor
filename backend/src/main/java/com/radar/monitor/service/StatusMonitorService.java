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

/**
 * 状态监控与推送服务
 */
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

    /**
     * 更新 UDP 监听状态
     */
    public void updateUdpStatus(boolean isListening, Integer port) {
        systemStatus.setUdpListening(isListening);
        systemStatus.setListeningPort(port);
    }

    /**
     * 处理新接收到的雷达数据
     */
    public void processNewData(RadarData data) {
        latestRadarData = data;
        frameCounter.incrementAndGet();
        lastReceiveTimestamp = System.currentTimeMillis();
        systemStatus.setLastReceiveTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        systemStatus.setRadarOnline(true);
    }

    /**
     * 计算帧率并检查在线状态
     */
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

    /**
     * 定时向前端推送数据
     */
    @Scheduled(fixedRate = 100) // Push every 100ms (10fps max to frontend)
    public void pushDataToFrontend() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("systemStatus", systemStatus);
        payload.put("radarData", latestRadarData); // Can be null if no data yet
        webSocketHandler.broadcastData(payload);
    }
}
