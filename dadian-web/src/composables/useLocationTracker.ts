import { ref, onUnmounted } from 'vue'

export function useLocationTracker() {
  const currentPosition = ref<{lat: number; lng: number} | null>(null)
  const isTracking = ref(false)
  const error = ref<string | null>(null)
  let watchId: number | null = null

  function startTracking() {
    if (!navigator.geolocation) {
      error.value = '浏览器不支持定位'
      return
    }
    isTracking.value = true
    error.value = null
    watchId = navigator.geolocation.watchPosition(
      (pos) => { currentPosition.value = { lat: pos.coords.latitude, lng: pos.coords.longitude } },
      (err) => { error.value = err.message; isTracking.value = false },
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 10000 },
    )
  }

  function stopTracking() {
    if (watchId !== null) {
      navigator.geolocation.clearWatch(watchId)
      watchId = null
    }
    isTracking.value = false
  }

  onUnmounted(() => stopTracking())

  return { currentPosition, isTracking, error, startTracking, stopTracking }
}
