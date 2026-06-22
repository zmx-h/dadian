<script setup lang="ts">
import type { Spot } from '@/stores/launch'

defineProps<{
  spot: Spot
}>()
</script>

<template>
  <div class="glass-card overflow-hidden w-40 h-60 flex flex-col transition-all duration-300 hover:border-amber-500/30 hover:-translate-y-1">
    <!-- image placeholder -->
    <div class="h-24 bg-stone-800 flex items-center justify-center">
      <span class="text-stone-600 text-xs">{{ spot.category }} · 图</span>
    </div>

    <!-- content -->
    <div class="flex flex-col gap-1.5 p-3 flex-1">
      <span class="text-sm font-medium text-stone-200 leading-tight line-clamp-2">
        {{ spot.name }}
      </span>

      <!-- category badge -->
      <span class="text-[10px] px-1.5 py-0.5 rounded bg-amber-500/10 text-amber-400 w-fit">
        {{ spot.category }}
      </span>

      <!-- rating stars -->
      <div class="flex items-center gap-1 mt-auto">
        <span class="text-xs text-amber-400">
          {{ '★'.repeat(Math.floor(spot.rating)) }}{{ '☆'.repeat(5 - Math.floor(spot.rating)) }}
        </span>
        <span class="text-[10px] text-stone-500">{{ spot.rating }}</span>
      </div>

      <!-- crowd level -->
      <div class="flex items-center gap-1.5">
        <span class="text-[10px] text-stone-500">热度</span>
        <div class="flex gap-0.5">
          <div
            class="w-3 h-1 rounded-full"
            :class="{
              'bg-green-500': spot.crowdLevel === 'low',
              'bg-amber-500': spot.crowdLevel === 'medium',
              'bg-red-500': spot.crowdLevel === 'high',
            }"
          />
          <div
            class="w-3 h-1 rounded-full"
            :class="{
              'bg-green-500': spot.crowdLevel === 'low',
              'bg-amber-500': spot.crowdLevel === 'medium',
              'bg-red-500': spot.crowdLevel === 'high',
            }"
          />
          <div
            class="w-3 h-1 rounded-full"
            :class="{
              'bg-stone-700': spot.crowdLevel === 'low',
              'bg-amber-500/30': spot.crowdLevel === 'medium',
              'bg-red-500': spot.crowdLevel === 'high',
            }"
          />
        </div>
      </div>
    </div>
  </div>
</template>
