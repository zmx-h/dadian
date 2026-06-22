<script setup lang="ts">
import { ref, computed } from 'vue'
import { useCommunityStore, type AlmanacEntry } from '@/stores/community'
import { Camera, UtensilsCrossed, Coffee, Dice5, Clapperboard, Trophy, X, Award } from '@lucide/vue'

const store = useCommunityStore()

const almanacTypes = [
  { key: 'agent', icon: Camera, title: '特工之王', desc: '拍摄最多', color: 'text-amber-400' },
  { key: 'foodie', icon: UtensilsCrossed, title: '美食雷达', desc: '探店最多', color: 'text-amber-400' },
  { key: 'npc', icon: Coffee, title: '佛系大师', desc: '低电量出行', color: 'text-amber-400' },
  { key: 'escape', icon: Dice5, title: '逃跑大师', desc: '骰子逃跑', color: 'text-amber-400' },
  { key: 'wkw', icon: Clapperboard, title: '王家卫传人', desc: 'WKW记忆', color: 'text-amber-400' },
]

const selectedType = ref<string | null>(null)
const selectedRankings = ref<AlmanacEntry[]>([])
const modalOpen = ref(false)

async function openAlmanac(type: string) {
  selectedType.value = type
  const data = await store.fetchAlmanac(type, 5)
  selectedRankings.value = data
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  selectedType.value = null
}

const currentAlmanac = computed(() => {
  if (!selectedType.value) return null
  return almanacTypes.find(t => t.key === selectedType.value)
})
</script>

<template>
  <div class="almanac-entry">
    <div class="flex items-center gap-1.5 mb-3">
      <Trophy :size="16" class="text-amber-500" />
      <span class="text-xs font-medium text-stone-300 tracking-wide">城市年鉴</span>
    </div>
    <div class="space-y-1.5">
      <button
        v-for="item in almanacTypes"
        :key="item.key"
        class="almanac-card w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 hover:border-amber-500/40 bg-amber-950/20 border border-amber-800/20 text-left group"
        @click="openAlmanac(item.key)"
      >
        <component :is="item.icon" :size="16" :class="item.color + ' flex-shrink-0'" />
        <div class="flex-1 min-w-0">
          <div class="text-xs text-stone-300 font-medium">{{ item.title }}</div>
          <div class="text-[10px] text-stone-500">{{ item.desc }}</div>
        </div>
        <Award :size="12" class="text-amber-600/50 group-hover:text-amber-400 transition-colors flex-shrink-0" />
      </button>
    </div>
    <Teleport to="body">
      <div v-if="modalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="closeModal">
        <div class="absolute inset-0 glass-overlay" />
        <div class="relative glass-card p-6 w-full max-w-sm z-10">
          <button class="absolute top-3 right-3 p-1 rounded-full text-stone-500 hover:text-stone-300 transition-colors" @click="closeModal">
            <X :size="18" />
          </button>
          <div class="flex items-center gap-2 mb-4">
            <component v-if="currentAlmanac" :is="currentAlmanac.icon" :size="20" :class="(currentAlmanac as any).color" />
            <span class="text-lg font-semibold text-amber-400">{{ currentAlmanac?.title }}</span>
          </div>
          <div class="space-y-3">
            <div v-for="(entry, idx) in selectedRankings" :key="entry.userId" class="flex items-center gap-3 p-2 rounded-lg" :class="idx === 0 ? 'bg-amber-500/10 border border-amber-500/20' : ''">
              <span class="w-6 text-center text-sm font-bold" :class="idx === 0 ? 'text-amber-400' : idx === 1 ? 'text-stone-300' : idx === 2 ? 'text-amber-700/80' : 'text-stone-500'">{{ entry.rank }}</span>
              <div class="w-8 h-8 rounded-full bg-stone-800 flex items-center justify-center text-xs text-stone-400 flex-shrink-0">{{ entry.displayName[0] }}</div>
              <span class="text-sm text-stone-200 flex-1">{{ entry.displayName }}</span>
              <span class="text-xs text-amber-400/70">{{ entry.score }}</span>
            </div>
          </div>
          <div class="mt-4 pt-3 border-t border-stone-800 text-center text-[11px] text-stone-500">数据每日凌晨 2:00 更新</div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.almanac-card { cursor: pointer; }
</style>
