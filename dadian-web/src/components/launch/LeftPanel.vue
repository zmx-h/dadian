<script setup lang="ts">
import { CloudSun, Lightbulb, Users } from '@lucide/vue'

// Mock weather
const weather = {
  city: '上海',
  temp: 26,
  condition: '多云',
  humidity: 68,
}

// Mock inspired routes (collected scripts)
const inspiredRoutes = [
  { id: '1', name: '武康路咖啡漫游', savedAt: '2天前', spots: 4 },
  { id: '2', name: '外滩夜行路线', savedAt: '5天前', spots: 3 },
]

// Mock friend activity
const friendActivity = [
  { id: '1', name: '小明', action: '打卡了 Manner 武康路', time: '30分钟前' },
  { id: '2', name: '小红', action: '创建了新路线「巨鹿路探店」', time: '2小时前' },
]
</script>

<template>
  <aside class="flex flex-col gap-4 w-56 shrink-0">
    <!-- Weather card -->
    <div class="glass-card p-4 flex items-center gap-3">
      <CloudSun :size="28" class="text-amber-400" />
      <div>
        <div class="text-sm font-semibold text-stone-200">{{ weather.city }}</div>
        <div class="text-lg font-bold text-amber-300">{{ weather.temp }}°C</div>
        <div class="text-[10px] text-stone-500">{{ weather.condition }} · 湿度 {{ weather.humidity }}%</div>
      </div>
    </div>

    <!-- Inspired routes -->
    <div class="glass-card p-4 flex flex-col gap-3">
      <div class="flex items-center gap-2 text-xs text-stone-400">
        <Lightbulb :size="14" class="text-amber-400" />
        <span class="font-medium">灵感路线</span>
      </div>
      <div v-for="r in inspiredRoutes" :key="r.id" class="flex flex-col gap-0.5">
        <span class="text-sm text-stone-300">{{ r.name }}</span>
        <span class="text-[10px] text-stone-600">{{ r.spots }} 个地点 · {{ r.savedAt }}</span>
      </div>
    </div>

    <!-- Friend activity -->
    <div class="glass-card p-4 flex flex-col gap-3">
      <div class="flex items-center gap-2 text-xs text-stone-400">
        <Users :size="14" class="text-amber-400" />
        <span class="font-medium">好友动态</span>
      </div>
      <div v-for="a in friendActivity" :key="a.id" class="flex flex-col gap-0.5">
        <span class="text-sm text-stone-300">{{ a.name }}</span>
        <span class="text-xs text-stone-500">{{ a.action }}</span>
        <span class="text-[10px] text-stone-600">{{ a.time }}</span>
      </div>
    </div>
  </aside>
</template>
