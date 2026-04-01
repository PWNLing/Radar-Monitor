package com.radar.monitor.controller;

import com.radar.monitor.udp.UdpServerManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * UDP 控制 API
 */
@RestController
@RequestMapping("/api/radar")
public class UdpController {
    private final UdpServerManager udpServerManager;

    public UdpController(UdpServerManager udpServerManager) {
        this.udpServerManager = udpServerManager;
    }

    /**
     * 启动监听
     */
    @PostMapping("/listen/start")
    public Map<String, Object> startListening(@RequestBody Map<String, Integer> payload) {
        Integer port = payload.get("port");
        boolean success = udpServerManager.startListening(port != null ? port : 8888);
        Map<String, Object> response = new HashMap<>();
        response.put("code", success ? 200 : 500);
        response.put("message", success ? "success" : "failed to bind port");
        return response;
    }

    /**
     * 停止监听
     */
    @PostMapping("/listen/stop")
    public Map<String, Object> stopListening() {
        udpServerManager.stopListening();
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        return response;
    }
}
