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
                <th>序号</th>
                <th>航迹ID</th>
                <th>类别</th>
                <th>经度</th>
                <th>纬度</th>
                <th>速度(m/s)</th>
                <th>距离(m)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(track, index) in radarData?.tracks" :key="track.track_id">
                <td>{{ index + 1 }}</td>
                <td>{{ track.track_id }}</td>
                <td>{{ translateType(track.type) }}</td>
                <td>{{ track.position?.longitude?.toFixed(6) || '-' }}</td>
                <td>{{ track.position?.latitude?.toFixed(6) || '-' }}</td>
                <td>{{ track.velocity?.toFixed(2) || '-' }}</td>
                <td>{{ track.distance?.toFixed(2) || '-' }}</td>
              </tr>
              <tr v-if="!radarData?.tracks || radarData.tracks.length === 0">
                <td colspan="7" class="empty-text">暂无目标数据</td>
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