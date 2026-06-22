import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export interface Spot {
  id: string; name: string; category: string; lat: number; lng: number
  address: string; city: string; crowdLevel: string; rating: number
  tags: string[]; highlight: string; imageUrl: string
}
export interface RouteWaypoint { id: string; name: string; type: string; description: string }
export interface GeneratedRoute { waypoints: RouteWaypoint[]; totalDistance: string; estimatedTime: string }
export interface Mission {
  id: string; type: string; title: string; description: string; reward: string
  status: string; participantStatus: string; completedAt: string
}
export interface Teammate { id: string; userId: string; displayName: string; role: string; socialEnergy: number }

export const useExploreStore = defineStore('explore', () => {
  const outing = ref<any>(null)
  const route = ref<any>(null)
  const missions = ref<Mission[]>([])
  const teammates = ref<Teammate[]>([])
  const currentLocation = ref<{lat:number;lng:number}|null>(null)
  const isTracking = ref(false)
  const isPaused = ref(false)

  const completedMissions = computed(() => missions.value.filter(m => m.participantStatus === 'completed'))
  const availableMissions = computed(() => missions.value.filter(m => m.participantStatus === 'available' || m.participantStatus === 'active'))
  const progressPercent = computed(() => {
    if (!missions.value.length) return 0
    return Math.round((completedMissions.value.length / missions.value.length) * 100)
  })

  async function createOuting(spotId: string) {
    const { data } = await api.post('/outings', { spotId, lat: 31.21, lng: 121.45, energy: 50, role: 'agent' })
    if (data.code === 0) outing.value = data.data
    return data
  }
  function requireOuting() { if (!outing.value?.id) throw new Error('No active outing') }

  async function startOuting() {
    requireOuting()
    const { data } = await api.post(`/outings/${outing.value.id}/start`)
    if (data.code === 0) outing.value = data.data
    return data
  }
  async function pauseOuting() {
    requireOuting()
    await api.post(`/outings/${outing.value.id}/pause`)
    isPaused.value = true
  }
  async function resumeOuting() {
    requireOuting()
    await api.post(`/outings/${outing.value.id}/resume`)
    isPaused.value = false
  }
  async function completeOuting() {
    requireOuting()
    await api.post(`/outings/${outing.value.id}/complete`)
    outing.value = null; missions.value = []; isTracking.value = false
  }
  async function fetchRoute() {
    requireOuting()
    const { data } = await api.get(`/outings/${outing.value.id}/route`)
    if (data.code === 0) route.value = data.data
  }
  async function fetchMissions() {
    requireOuting()
    const { data } = await api.get(`/outings/${outing.value.id}/missions`)
    if (data.code === 0) missions.value = data.data
  }
  async function acceptMission(id: string) {
    requireOuting()
    await api.post(`/outings/${outing.value.id}/missions/${id}/accept`)
    await fetchMissions()
  }
  async function skipMission(id: string) {
    requireOuting()
    await api.post(`/outings/${outing.value.id}/missions/${id}/skip`)
    await fetchMissions()
  }
  async function completeMission(id: string, photoUrl?: string) {
    requireOuting()
    await api.post(`/outings/${outing.value.id}/missions/${id}/complete`, { proofPhotoUrl: photoUrl })
    await fetchMissions()
  }
  async function checkin(spotId: string, photoUrl?: string, comment?: string) {
    requireOuting()
    const { data } = await api.post('/footprints', { outingId: outing.value.id, spotId, lat: 31.21, lng: 121.45, photoUrl, comment })
    return data
  }
  function updateLocation(lat: number, lng: number) { currentLocation.value = { lat, lng } }

  return {
    outing, route, missions, teammates, currentLocation, isTracking, isPaused,
    completedMissions, availableMissions, progressPercent,
    createOuting, startOuting, pauseOuting, resumeOuting, completeOuting,
    fetchRoute, fetchMissions, acceptMission, skipMission, completeMission,
    checkin, updateLocation,
  }
})
