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