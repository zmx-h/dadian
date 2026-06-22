<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useExploreStore, type Spot } from '@/stores/explore'
import api from '@/api'
import MapArea from '@/components/explore/MapArea.vue'
import SpotDiscovery from '@/components/explore/SpotDiscovery.vue'
import TeammatePanel from '@/components/explore/TeammatePanel.vue'
import MissionHandbook from '@/components/explore/MissionHandbook.vue'
import BottomControlBar from '@/components/explore/BottomControlBar.vue'
import CheckInModal from '@/components/explore/CheckInModal.vue'
import TransferModal from '@/components/explore/TransferModal.vue'
import { useLocationTracker } from '@/composables/useLocationTracker'

const explore = useExploreStore()
const { outing, route, missions, teammates, isPaused } = storeToRefs(explore)
const { currentPosition, startTracking } = useLocationTracker()

const showCheckIn = ref(false)
const showTransfer = ref(false)
const spots = ref<Spot[]>([])

const hasActiveOuting = computed(() => outing.value && outing.value.status !== 'completed')

onMounted(async () => {
  const { data } = await api.get('/spots?limit=12')
  if (data.code === 0) spots.value = data.data
})

async function handleCreateOuting(spotId: string) {
  await explore.createOuting(spotId)
  await explore.startOuting()
  await explore.fetchRoute()
  await explore.fetchMissions()
  startTracking()
}

async function handlePause() { await explore.pauseOuting() }
async function handleResume() { await explore.resumeOuting() }
async function handleComplete() { await explore.completeOuting() }
</script>

<template>
  <div class="explore-page min-h-screen flex flex-col">
    <div class="flex-1 flex flex-col xl:flex-row gap-4 p-4 xl:p-6">
      <div class="flex-1 min-h-[400px]">
        <MapArea v-if="hasActiveOuting" :route="route" :missions="missions" :current-position="currentPosition" />
        <SpotDiscovery v-else :spots="spots" @select="handleCreateOuting" />
      </div>
      <div class="xl:w-72 flex flex-col gap-4">
        <TeammatePanel :teammates="teammates" />
        <MissionHandbook v-if="hasActiveOuting" :missions="missions" />
      </div>
    </div>
    <BottomControlBar
      v-if="hasActiveOuting"
      :missions="missions"
      :is-paused="isPaused"
      @pause="handlePause"
      @resume="handleResume"
      @complete="handleComplete"
      @checkin="showCheckIn = true"
      @transfer="showTransfer = true"
    />
    <CheckInModal v-if="showCheckIn" @close="showCheckIn = false" @confirm="(c) => { explore.checkin(c.spotId, c.photoUrl, c.comment); showCheckIn = false }" />
    <TransferModal v-if="showTransfer" @close="showTransfer = false" />
  </div>
</template>
