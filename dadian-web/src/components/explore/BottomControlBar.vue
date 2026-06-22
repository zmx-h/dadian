<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { Mission } from '@/stores/explore'
import { Pause, Play, Camera, RefreshCw, Users, Radio } from '@lucide/vue'

const props = defineProps<{ missions: Mission[]; isPaused: boolean; teammateCount?: number; isRemote?: boolean }>()
const emit = defineEmits<{ pause: []; resume: []; complete: []; checkin: []; transfer: []; toggleTeammates: [] }>()

const elapsed = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => { timer = setInterval(() => elapsed.value++, 1000) })
onUnmounted(() => { if (timer) clearInterval(timer) })

function fmt(t:number) {
  const h = Math.floor(t/3600), m = Math.floor((t%3600)/60), s = t % 60
  return `${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}`
}
</script>

<template>
  <div class="control-bar sticky bottom-0 glass-card border-t border-stone-800 px-4 py-3 flex items-center justify-between gap-3 z-20">
    <div class="flex items-center gap-2">
      <button class="glass-button px-3 py-1.5 text-xs flex items-center gap-1" @click="isPaused ? emit('resume') : emit('pause')">
        <component :is="isPaused?Play:Pause" :size="14" />
        {{ isPaused ? '继续' : '暂停' }}
      </button>
      <button class="glass-button px-3 py-1.5 text-xs flex items-center gap-1" @click="emit('checkin')">
        <Camera :size="14" /> 打卡
      </button>
      <button class="glass-button px-3 py-1.5 text-xs flex items-center gap-1" @click="emit('transfer')">
        <RefreshCw :size="14" /> 转职
      </button>
    </div>
    <div class="flex items-center gap-4 text-xs text-stone-500">
      <button
        v-if="teammateCount || isRemote"
        class="flex items-center gap-1 text-xs hover:text-stone-300 transition-colors"
        @click="emit('toggleTeammates')"
      >
        <component :is="isRemote ? Radio : Users" :size="14" :class="isRemote ? 'text-violet-400' : 'text-amber-400'" />
        <span v-if="isRemote" class="text-violet-400">远程</span>
        <span v-else>{{ teammateCount }}人</span>
      </button>
      <span>{{ fmt(elapsed) }}</span>
      <span>{{ missions.filter(m => m.participantStatus === 'completed').length }}/{{ missions.length }} 任务</span>
      <button class="text-red-400/60 hover:text-red-400 text-[10px]" @click="emit('complete')">结束出行</button>
    </div>
  </div>
</template>
