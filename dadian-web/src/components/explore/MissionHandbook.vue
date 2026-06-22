<script setup lang="ts">
import type { Mission } from '@/stores/explore'
import { CheckCircle, Circle, Play, SkipForward } from '@lucide/vue'

defineProps<{ missions: Mission[] }>()
const emit = defineEmits<{ accept: [id: string]; skip: [id: string]; complete: [id: string] }>()
</script>

<template>
  <div class="glass-card p-4">
    <h4 class="text-xs text-stone-500 mb-3">任务手账</h4>
    <div v-if="!missions.length" class="text-xs text-stone-600">暂无任务</div>
    <div v-for="m in missions" :key="m.id" class="py-2 border-b border-stone-800 last:border-b-0">
      <div class="flex items-center gap-2">
        <CheckCircle v-if="m.participantStatus === 'completed'" :size="14" class="text-green-400 flex-shrink-0" />
        <Circle v-else-if="m.participantStatus === 'active'" :size="14" class="text-amber-400 amber-breathe flex-shrink-0" />
        <Circle v-else :size="14" class="text-stone-600 flex-shrink-0" />
        <div class="flex-1 min-w-0">
          <div class="text-xs text-stone-300 truncate">{{ m.title }}</div>
          <div class="text-[10px] text-stone-500 truncate">{{ m.description }}</div>
        </div>
      </div>
      <div v-if="m.participantStatus === 'available'" class="flex gap-2 mt-2 ml-6">
        <button class="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 flex items-center gap-1" @click="emit('accept', m.id)">
          <Play :size="10" /> 接受
        </button>
        <button class="text-[10px] px-2 py-0.5 rounded-full text-stone-500 flex items-center gap-1" @click="emit('skip', m.id)">
          <SkipForward :size="10" /> 跳过
        </button>
      </div>
      <div v-else-if="m.participantStatus === 'active'" class="ml-6 mt-1">
        <button class="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20" @click="emit('complete', m.id)">
          完成打卡
        </button>
      </div>
    </div>
  </div>
</template>
