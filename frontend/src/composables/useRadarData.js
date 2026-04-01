import { ref, onMounted, onUnmounted } from 'vue'

export function useRadarData() {
  const systemStatus = ref({
    udpListening: false,
    listeningPort: null,
    radarOnline: false,
    fps: 0,
    lastReceiveTime: '-'
  })
  const radarData = ref(null)
  let ws = null

  const connectWs = () => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/radar`
    
    ws = new WebSocket(wsUrl)
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      if (data.systemStatus) systemStatus.value = data.systemStatus
      if (data.radarData) radarData.value = data.radarData
    }
    ws.onclose = () => {
      setTimeout(connectWs, 3000) // Auto reconnect
    }
  }

  onMounted(() => {
    connectWs()
  })

  onUnmounted(() => {
    if (ws) ws.close()
  })

  return { systemStatus, radarData }
}
