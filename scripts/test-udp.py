import socket
import json
import time
import math
import random

# 配置部分
# TARGET_IP = "192.168.10.200"
TARGET_IP = "127.0.0.1"
TARGET_PORT = 10000

# 中心点：桂电花江校区
CENTER_LON = 110.416819
CENTER_LAT = 25.311724
BASE_ALT = 180.0

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# 定义目标类型
ALL_TYPES = ["CAR", "TRUCK", "PEDESTRIAN", "BICYCLE", "UNKNOWN", "TEST_GHOST"]

# 配置目标
targets_config = [
    # --- CAR (3个) ---
    {"id": 101, "type": "CAR",       "lat_off": 0.00010, "lon_off": 0.00010, "speed_base": 12.0, "move_type": "linear_east"},
    {"id": 102, "type": "CAR",       "lat_off": -0.00015, "lon_off": 0.00005, "speed_base": 8.0,  "move_type": "linear_west"},
    {"id": 103, "type": "CAR",       "lat_off": 0.00020, "lon_off": -0.00010, "speed_base": 15.0, "move_type": "linear_north"},
    
    # --- TRUCK (2个) ---
    {"id": 201, "type": "TRUCK",     "lat_off": 0.00030, "lon_off": 0.00030, "speed_base": 6.0,  "move_type": "slow_linear"},
    {"id": 202, "type": "TRUCK",     "lat_off": -0.00025, "lon_off": -0.00020, "speed_base": 5.0,  "move_type": "slow_linear"},
    
    # --- PEDESTRIAN (2个) ---
    {"id": 301, "type": "PEDESTRIAN", "lat_off": 0.00005, "lon_off": -0.00005, "speed_base": 1.4, "move_type": "wander"},
    {"id": 302, "type": "PEDESTRIAN", "lat_off": -0.00005, "lon_off": 0.00005, "speed_base": 1.2, "move_type": "wander"},
    
    # --- BICYCLE (2个) ---
    {"id": 401, "type": "BICYCLE",   "lat_off": 0.00012, "lon_off": 0.00000, "speed_base": 4.5, "move_type": "circle"},
    {"id": 402, "type": "BICYCLE",   "lat_off": 0.00000, "lon_off": 0.00012, "speed_base": 5.0, "move_type": "circle"},
    
    # --- UNKNOWN (1个) ---
    {"id": 501, "type": "UNKNOWN",   "lat_off": 0.00040, "lon_off": 0.00000, "speed_base": 0.0, "move_type": "jitter"},
]

sequence = 0
start_time = time.time()

print(f"=== Simulation with 'distance' field ===")
print(f"Center: Lat {CENTER_LAT}, Lon {CENTER_LON}")
print(f"Sending to: {TARGET_IP}:{TARGET_PORT}\n")

try:
    while True:
        current_timestamp = time.time()
        elapsed_time = current_timestamp - start_time
        sequence += 1
        
        tracks = []
        
        for cfg in targets_config:
            d_lat = cfg["lat_off"]
            d_lon = cfg["lon_off"]
            
            # --- 运动逻辑 ---
            if cfg["move_type"] == "linear_east":
                d_lon += elapsed_time * 0.000001 * cfg["speed_base"]
            elif cfg["move_type"] == "linear_west":
                d_lon -= elapsed_time * 0.000001 * cfg["speed_base"]
            elif cfg["move_type"] == "linear_north":
                d_lat += elapsed_time * 0.000001 * cfg["speed_base"]
            elif cfg["move_type"] == "slow_linear":
                d_lon += elapsed_time * 0.0000005 * cfg["speed_base"]
                d_lat += elapsed_time * 0.0000002 * cfg["speed_base"]
            elif cfg["move_type"] == "wander":
                d_lat += math.sin(elapsed_time * 0.8 + cfg["id"]) * 0.000002
                d_lon += math.cos(elapsed_time * 0.6 + cfg["id"]) * 0.000002
            elif cfg["move_type"] == "circle":
                radius = 0.00008
                angle = elapsed_time * 0.5 + cfg["id"]
                d_lat += math.sin(angle) * radius
                d_lon += math.cos(angle) * radius
            elif cfg["move_type"] == "jitter":
                d_lat += random.uniform(-0.000001, 0.000001)
                d_lon += random.uniform(-0.000001, 0.000001)

            # 计算当前速度
            current_speed = max(0, cfg["speed_base"] + random.uniform(-0.3, 0.3))
            
            # 计算最终经纬度
            final_lat = CENTER_LAT + d_lat
            final_lon = CENTER_LON + d_lon
            
            # --- 新增：计算距离 (Distance) ---
            # 使用简化公式计算与中心点的直线距离 (米)
            # 纬度差 1度 ≈ 111139 米
            # 经度差 1度 ≈ 111139 * cos(纬度) 米
            lat_diff = final_lat - CENTER_LAT
            lon_diff = final_lon - CENTER_LON
            
            meters_per_deg_lat = 111139
            meters_per_deg_lon = 111139 * math.cos(math.radians(CENTER_LAT))
            
            distance_x = lon_diff * meters_per_deg_lon
            distance_y = lat_diff * meters_per_deg_lat
            
            # 欧几里得距离
            distance_meters = math.sqrt(distance_x**2 + distance_y**2)

            # 构建目标数据
            track_data = {
                "track_id": cfg["id"],
                "type": cfg["type"],
                "position": {
                    "latitude": round(final_lat, 6),
                    "longitude": round(final_lon, 6),
                    "altitude": round(BASE_ALT + random.uniform(-0.5, 0.5), 2)
                },
                "distance": round(distance_meters, 3),  # 【新增字段】单位：米，保留3位小数
                "velocity": round(current_speed, 3)
            }
            tracks.append(track_data)

        # 构建 Payload
        payload = {
            "type": "fusion_geodetic_data",
            "version": "1.0",
            "timestamp": current_timestamp,
            "source": "imu_rtk_fusion",
            "header": {
                "frame_id": "indoor_test", # 对应你的示例
                "stamp": {
                    "sec": int(current_timestamp)
                },
                "sequence": sequence
            },
            "target_count": len(tracks),
            "tracks": tracks
        }

        data = json.dumps(payload).encode("utf-8")
        sock.sendto(data, (TARGET_IP, TARGET_PORT))
        
        # 打印逻辑 (仅打印前3帧完整JSON)
        if sequence <= 3:
            print(f"--- [Frame {sequence}] JSON ---")
            print(json.dumps(payload, indent=2))
            print("--- [End] ---\n")
        elif sequence % 30 == 0:
            print(f"[Seq {sequence}] Sent {len(tracks)} targets.")
        
        time.sleep(0.1)

except KeyboardInterrupt:
    print("\nSimulation stopped.")
finally:
    sock.close()