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
