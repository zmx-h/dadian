<script setup lang="ts">
import type { Teammate } from '@/stores/explore'
import { Circle } from '@lucide/vue'

defineProps<{ teammates: Teammate[] }>()
</script>

<template>
  <div class="glass-card p-4">
    <h4 class="text-xs text-stone-500 mb-3">队友状态</h4>
    <div v-if="!teammates.length" class="text-xs text-stone-600">
      <Circle :size="6" class="inline mr-1 text-amber-500 fill-amber-500 amber-breathe" /> 一个人出发
    </div>
    <div v-for="t in teammates" :key="t.id" class="flex items-center gap-2 py-1.5">
      <div class="w-7 h-7 rounded-full bg-stone-700 flex items-center justify-center text-xs text-stone-300">
        {{ t.displayName?.charAt(0) || '?' }}
      </div>
      <div class="flex-1 min-w-0">
        <div class="text-xs text-stone-300 truncate">{{ t.displayName }}</div>
        <div class="text-[10px] text-stone-500">{{ t.role }} · {{ t.socialEnergy }}%</div>
      </div>
      <Circle :size="6" class="text-amber-500 fill-amber-500 amber-breathe flex-shrink-0" />
    </div>
  </div>
</template>
