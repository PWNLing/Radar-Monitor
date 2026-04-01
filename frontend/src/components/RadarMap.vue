<template>
  <div id="map-container"></div>
</template>

<script setup>
import { onMounted, watch, ref, shallowRef } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const props = defineProps({
  radarData: Object,
  focusPosition: Object,
  selectedTrackId: Number
})

const emit = defineEmits(['selectTarget', 'focusTarget'])

const map = shallowRef(null)
const markers = ref(new Map()) // trackId -> AMap.Marker
const egoMarker = shallowRef(null)

const createMarkerContent = (type, isSelected) => {
  const typeColors = {
    'PEDESTRIAN': '#10b981', // green
    'CAR': '#3b82f6', // blue
    'BICYCLE': '#f59e0b', // yellow
    'TRUCK': '#8b5cf6', // purple
  }
  const color = typeColors[type] || '#94a3b8'
  const size = isSelected ? 20 : 12
  const border = isSelected ? '3px solid white' : '2px solid white'
  const shadow = isSelected ? `0 0 10px ${color}, 0 0 20px ${color}` : '0 2px 4px rgba(0,0,0,0.3)'
  const zIndex = isSelected ? 100 : 1
  
  return `<div style="
    width: ${size}px; 
    height: ${size}px; 
    background-color: ${color}; 
    border-radius: 50%; 
    border: ${border}; 
    box-shadow: ${shadow};
    transition: all 0.2s;
    position: relative;
    z-index: ${zIndex};
  "></div>`
}

onMounted(() => {
  if (import.meta.env.VITE_AMAP_SECURITY_CODE) {
    window._AMapSecurityConfig = {
      securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE,
    }
  }
  
  AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY || '1bf5b821a81e3a1f81a7006d6bb15e1f', // 默认使用测试Key
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar'] // 可选的插件
  }).then((AMap) => {
    map.value = new AMap.Map('map-container', {
      zoom: 17,
      center: [xxx, xxx], // 默认地点
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
      const isSelected = props.selectedTrackId === track.track_id
      const content = createMarkerContent(track.type, isSelected)
      
      if (markers.value.has(track.track_id)) {
        // 存在则移动
        const marker = markers.value.get(track.track_id)
        marker.setPosition(position)
        marker.setContent(content)
        marker.setExtData({ type: track.type })
      } else {
        // 新增目标
        const marker = new AMap.Marker({
          position: position,
          content: content,
          title: `ID: ${track.track_id} | ${track.type}`,
          anchor: 'center',
          extData: { type: track.type }
        })
        marker.on('click', () => {
          emit('selectTarget', track.track_id)
          emit('focusTarget', track.position)
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

watch(() => props.selectedTrackId, (newId, oldId) => {
  if (oldId && markers.value.has(oldId)) {
    const marker = markers.value.get(oldId)
    const type = marker.getExtData().type
    marker.setContent(createMarkerContent(type, false))
  }
  if (newId && markers.value.has(newId)) {
    const marker = markers.value.get(newId)
    const type = marker.getExtData().type
    marker.setContent(createMarkerContent(type, true))
  }
})

watch(() => props.focusPosition, (pos) => {
  if (!map.value || !pos) return
  // 定位到目标位置，并稍微放大层级
  map.value.setZoomAndCenter(19, [pos.longitude, pos.latitude])
})
</script>

<style scoped>
#map-container {
  width: 100%;
  height: 100%;
}
</style>
