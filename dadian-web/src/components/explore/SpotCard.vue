<script setup lang="ts">
import type { Spot } from '@/stores/explore'
import { Star, MapPin } from '@lucide/vue'

defineProps<{ spot: Spot }>()
</script>

<template>
  <button class="spot-card glass-card p-0 overflow-hidden text-left w-full transition-all duration-300 hover:border-amber-500/40 hover:scale-[1.02] cursor-pointer">
    <div class="h-24 bg-stone-800 flex items-center justify-center">
      <MapPin :size="28" class="text-stone-600" />
    </div>
    <div class="p-3">
      <div class="flex items-center gap-1 text-amber-400 text-xs mb-1">
        <Star :size="10" fill="currentColor" />
        <span>{{ spot.rating }}</span>
      </div>
      <div class="text-sm font-medium text-stone-200 truncate">{{ spot.name }}</div>
      <div class="flex gap-1 mt-1.5 flex-wrap">
        <span class="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20">
          {{ spot.category }}
        </span>
        <span v-if="spot.crowdLevel" class="text-[10px] px-1.5 py-0.5 rounded-full bg-stone-800 text-stone-400">
          {{ spot.crowdLevel === 'low' ? '安静' : spot.crowdLevel === 'high' ? '热闹' : '适中' }}
        </span>
      </div>
    </div>
  </button>
</template>
