package com.radar.monitor.udp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radar.monitor.model.RadarData;
import com.radar.monitor.service.StatusMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * UDP 动态监听器
 */
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

    /**
     * 启动 UDP 监听
     * @param port 监听端口
     * @return 是否启动成功
     */
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

    /**
     * 停止 UDP 监听
     */
    public synchronized void stopListening() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        statusMonitorService.updateUdpStatus(false, null);
    }
}
