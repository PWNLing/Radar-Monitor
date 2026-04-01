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