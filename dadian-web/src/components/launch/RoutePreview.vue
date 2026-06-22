<script setup lang="ts">
import type { RouteWaypoint } from '@/stores/launch'

defineProps<{
  waypoints: RouteWaypoint[]
  totalDistance: string
  estimatedTime: string
}>()

const typeBadge = (type: RouteWaypoint['type']) => {
  switch (type) {
    case 'start': return { label: '起点', cls: 'bg-green-500/10 text-green-400 border-green-500/30' }
    case 'waypoint': return { label: '途经', cls: 'bg-amber-500/10 text-amber-400 border-amber-500/30' }
    case 'end': return { label: '终点', cls: 'bg-orange-500/10 text-orange-400 border-orange-500/30' }
  }
}
</script>

<template>
  <div class="glass-card p-5">
    <!-- header -->
    <div class="flex items-center justify-between mb-5">
      <span class="text-sm font-semibold text-stone-200">路线预览</span>
      <div class="flex items-center gap-3 text-xs text-stone-500">
        <span>{{ totalDistance }}</span>
        <span>{{ estimatedTime }}</span>
      </div>
    </div>

    <!-- timeline -->
    <div class="relative pl-6">
      <template v-for="(wp, i) in waypoints" :key="wp.id">
        <!-- dot + line -->
        <div class="relative flex items-start gap-3 pb-5 last:pb-0">
          <!-- dot -->
          <div class="absolute left-0 top-0 -translate-x-1/2">
            <div
              class="w-3 h-3 rounded-full border-2"
              :class="{
                'bg-green-500 border-green-400': wp.type === 'start',
                'bg-amber-500 border-amber-400 amber-breathe': wp.type === 'waypoint',
                'bg-orange-500 border-orange-400': wp.type === 'end',
              }"
            />
          </div>

          <!-- vertical line (except last) -->
          <div
            v-if="i < waypoints.length - 1"
            class="absolute left-0 top-3 -translate-x-1/2 w-px h-full bg-stone-700"
          />

          <!-- content -->
          <div class="flex flex-col gap-1 ml-3">
            <span class="text-xs px-1.5 py-0.5 rounded border w-fit" :class="typeBadge(wp.type).cls">
              {{ typeBadge(wp.type).label }}
            </span>
            <span class="text-sm font-medium text-stone-200">{{ wp.name }}</span>
            <span class="text-xs text-stone-500">{{ wp.description }}</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
