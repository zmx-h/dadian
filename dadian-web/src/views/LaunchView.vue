<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useLaunchStore } from '@/stores/launch'
import type { GeneratedRoute } from '@/stores/launch'
import StepProgressBar from '@/components/launch/StepProgressBar.vue'
import LeftPanel from '@/components/launch/LeftPanel.vue'
import RightPanel from '@/components/launch/RightPanel.vue'
import RegionSelector from '@/components/launch/RegionSelector.vue'
import SpotFlow from '@/components/launch/SpotFlow.vue'
import RoutePreview from '@/components/launch/RoutePreview.vue'
import { Rocket } from '@lucide/vue'

const launch = useLaunchStore()
const { currentStep, selectedRegion, selectedSpot } = storeToRefs(launch)

// ── Mock route generator ──
const mockRoute = computed<GeneratedRoute>(() => {
  if (!selectedSpot.value) {
    return { waypoints: [], totalDistance: '', estimatedTime: '' }
  }
  return {
    waypoints: [
      {
        id: 'wp-1',
        name: '你的位置',
        type: 'start',
        description: '从当前位置出发',
      },
      {
        id: 'wp-2',
        name: selectedSpot.value.name,
        type: 'end',
        description: '目标地点，预计停留 1.5 小时',
      },
    ],
    totalDistance: '3.2 km',
    estimatedTime: '约 25 分钟',
  }
})

// ── Step title ──
const stepTitle = computed(() => {
  switch (currentStep.value) {
    case 1: return '选择一个区域'
    case 2: return '选择一个地点'
    case 3: return '确认路线'
    default: return ''
  }
})
</script>

<template>
  <div class="launch-page min-h-screen p-6 flex flex-col gap-6">
    <!-- Step progress -->
    <StepProgressBar :current-step="currentStep" />

    <!-- Three-column desk layout (>=1280px) -->
    <div class="flex-1 flex gap-6 justify-center">
      <!-- Left panel -->
      <div class="hidden xl:block">
        <LeftPanel />
      </div>

      <!-- Main: map / POI area -->
      <div class="flex-1 max-w-2xl flex flex-col gap-5">
        <h2 class="text-lg font-semibold text-stone-200">{{ stepTitle }}</h2>

        <!-- Step 1: Region -->
        <div v-if="currentStep === 1" class="flex-1">
          <RegionSelector
            :model-value="selectedRegion"
            @update:model-value="launch.selectRegion"
          />
        </div>

        <!-- Step 2: Spot -->
        <div v-else-if="currentStep === 2" class="flex-1">
          <SpotFlow
            :region="selectedRegion"
            @select="launch.selectSpot"
          />

          <!-- Back to region -->
          <button
            class="mt-4 text-xs text-stone-500 hover:text-amber-400 transition-colors duration-200"
            @click="launch.setStep(1)"
          >
            返回重新选择区域
          </button>
        </div>

        <!-- Step 3: Route -->
        <div v-else-if="currentStep === 3" class="flex-1 flex flex-col gap-4">
          <!-- Selected spot summary -->
          <div v-if="selectedSpot" class="glass-card p-4 flex items-center justify-between">
            <div>
              <span class="text-sm text-stone-400">已选地点</span>
              <div class="text-base font-semibold text-amber-300 mt-0.5">
                {{ selectedSpot.name }}
              </div>
            </div>
            <button
              class="text-xs text-stone-500 hover:text-amber-400 transition-colors"
              @click="launch.setStep(2)"
            >
              更换
            </button>
          </div>

          <!-- Route preview -->
          <RoutePreview
            :waypoints="mockRoute.waypoints"
            :total-distance="mockRoute.totalDistance"
            :estimated-time="mockRoute.estimatedTime"
          />

          <!-- Region context -->
          <p class="text-xs text-stone-500">
            区域：{{ selectedRegion }} · 当前电量 {{ launch.socialEnergy }}%
          </p>
        </div>
      </div>

      <!-- Right panel -->
      <div class="hidden xl:block">
        <RightPanel />
      </div>
    </div>

    <!-- Launch button (bottom) -->
    <div class="flex justify-center pb-6">
      <button
        class="glass-button px-10 py-3 flex items-center gap-3 text-base font-semibold transition-all duration-300 hover:bg-amber-500/20 hover:shadow-[0_0_24px_rgba(245,158,11,0.4)] disabled:opacity-30 disabled:cursor-not-allowed"
        :disabled="currentStep < 3 && !selectedSpot"
        @click="launch.confirmLaunch()"
      >
        <Rocket :size="20" class="text-amber-400" />
        <span class="text-amber-300">出发</span>
      </button>
    </div>

    <!-- Mobile/tablet: show panels as collapsible sections -->
    <div class="xl:hidden grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
      <LeftPanel />
      <RightPanel />
    </div>
  </div>
</template>
