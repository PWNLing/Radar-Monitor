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
