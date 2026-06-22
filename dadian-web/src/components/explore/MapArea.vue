<script setup lang="ts">
import type { RouteWaypoint, Teammate } from '@/stores/explore'

const props = defineProps<{
  route: any
  missions: any[]
  currentPosition: {lat:number;lng:number} | null
  teammates?: Teammate[]
  isRemote?: boolean
}>()

function xPos(i: number) { return 200 + i * 200 }
function yPos(i: number) { return 250 - i * 60 + (i % 2) * 30 }
function polylineStr(wps: RouteWaypoint[]) {
  return wps.map((_, i: number) => `${xPos(i)},${yPos(i)}`).join(' ')
}
function dotFill(i: number, total: number) {
  if (i === 0) return '#10b981'
  if (i === total - 1) return '#f97316'
  return '#f59e0b'
}
function asNum(v: unknown): number { return Number(v) }
</script>

<template>
  <div class="map-area glass-card h-full min-h-[400px] relative overflow-hidden flex items-center justify-center">
    <div class="absolute inset-0" style="background-image: linear-gradient(rgba(245,158,11,0.05) 1px,transparent 1px), linear-gradient(90deg, rgba(245,158,11,0.05) 1px,transparent 1px); background-size: 40px 40px;" />

    <svg v-if="route?.waypoints?.length" class="absolute inset-0 w-full h-full" viewBox="0 0 800 500" preserveAspectRatio="xMidYMid meet">
      <polyline
        :points="polylineStr(route.waypoints)"
        fill="none"
        :stroke="route.neonColor === 'violet' ? '#8b5cf6' : route.neonColor === 'orange' ? '#f97316' : '#f59e0b'"
        stroke-width="3" stroke-linecap="round" stroke-dasharray="8 4" class="opacity-70"
      />
      <circle v-for="(wp, i) in route.waypoints" :key="wp.id"
        :cx="xPos(asNum(i))" :cy="yPos(asNum(i))" r="8"
        :fill="dotFill(asNum(i), route.waypoints.length)"
        class="amber-breathe"
      />
      <text v-for="(wp, i) in route.waypoints" :key="'l'+wp.id"
        :x="xPos(asNum(i))" :y="yPos(asNum(i)) - 15"
        text-anchor="middle" fill="#a8a29e" font-size="11">
        {{ wp.name }}
      </text>

      <!-- Teammate markers -->
      <template v-if="teammates?.length">
        <g v-for="(t, i) in teammates" :key="'tm'+t.id">
          <circle
            :cx="100 + i * 150"
            :cy="300 + (i % 2) * 50"
            r="7"
            fill="#8b5cf6"
            fill-opacity="0.9"
            stroke="#7c3aed"
            stroke-width="2"
          />
          <circle
            :cx="100 + i * 150"
            :cy="300 + (i % 2) * 50"
            r="12"
            fill="none"
            stroke="#8b5cf6"
            stroke-width="1"
            stroke-opacity="0.3"
          />
          <text
            :x="100 + i * 150"
            :y="300 + (i % 2) * 50 + 22"
            text-anchor="middle"
            fill="#a78bfa"
            font-size="10"
          >
            {{ t.displayName }}
          </text>
        </g>
      </template>
    </svg>

    <div v-if="currentPosition" class="absolute w-4 h-4 rounded-full bg-amber-500 amber-breathe z-10" style="left:50%; top:45%;" />

    <div v-if="!route?.waypoints?.length" class="relative z-10 text-stone-600 text-sm">📍 路线加载中...</div>
  </div>
</template>
