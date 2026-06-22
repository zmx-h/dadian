<script setup lang="ts">

import { useRouter } from 'vue-router'
import CollectButton from './CollectButton.vue'
import { Zap, Camera } from '@lucide/vue'

const props = defineProps<{
  memories: any[]
  loading?: boolean
}>()

const emit = defineEmits<{ collect: [id: string]; 'load-more': [] }>()

const router = useRouter()

const styleEmoji: Record<string, string> = {
  wangjiawei: '🎬',
  cyberpunk: '🤖',
  documentary: '💿',
  retro: '📷',
  dream: '🌙',
}

const styleLabel: Record<string, string> = {
  wangjiawei: '王家卫',
  cyberpunk: '赛博朋克',
  documentary: '纪录片',
  retro: '复古',
  dream: '梦幻',
}

const styleColor: Record<string, string> = {
  wangjiawei: 'bg-violet-500/20 text-violet-300 border-violet-500/30',
  cyberpunk: 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30',
  documentary: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30',
  retro: 'bg-orange-500/20 text-orange-300 border-orange-500/30',
  dream: 'bg-pink-500/20 text-pink-300 border-pink-500/30',
}

function timeAgo(dateStr: string): string {
  const now = Date.now()
  const diff = now - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return new Date(dateStr).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function viewMemory(id: string) {
  router.push(`/hui-yi/${id}`)
}

function onCollect(memoryId: string) {
  emit('collect', memoryId)
}
</script>

<template>
  <div class="memory-feed">
    <!-- Grid -->
    <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
      <button
        v-for="m in memories"
        :key="m.id"
        class="memory-card glass-card p-0 overflow-hidden text-left transition-all duration-300 hover:scale-[1.02] hover:border-amber-500/30 cursor-pointer group"
        @click="viewMemory(m.id)"
      >
        <!-- Cover area -->
        <div class="h-36 sm:h-44 bg-stone-800 flex flex-col items-center justify-center relative overflow-hidden">
          <span class="text-4xl sm:text-5xl transition-transform duration-300 group-hover:scale-110">
            {{ styleEmoji[m.style] || '💿' }}
          </span>
          <div class="absolute inset-0 bg-gradient-to-t from-stone-950/60 via-transparent to-transparent pointer-events-none" />
          <!-- Charge count badge -->
          <div v-if="m.chargeCount" class="absolute top-2 right-2 flex items-center gap-1 px-2 py-0.5 rounded-full bg-stone-950/60 backdrop-blur text-[10px] text-amber-400">
            <Zap :size="10" />
            {{ m.chargeCount }}
          </div>
        </div>

        <!-- Card body -->
        <div class="p-3 space-y-2">
          <!-- Style badge -->
          <div class="flex items-center justify-between">
            <span
              class="inline-block text-[10px] px-2 py-0.5 rounded-full border"
              :class="styleColor[m.style] || styleColor.documentary"
            >
              {{ styleLabel[m.style] || m.style || '纪录片' }}
            </span>
          </div>

          <!-- Title -->
          <div class="text-sm font-medium text-stone-200 truncate leading-snug">
            {{ m.title }}
          </div>

          <!-- User + time + energy -->
          <div class="flex items-center justify-between gap-2">
            <div class="flex items-center gap-1.5 min-w-0">
              <div class="w-5 h-5 rounded-full bg-stone-700 flex-shrink-0 flex items-center justify-center text-[10px] text-stone-400">
                {{ (m.user?.displayName || '?')[0] }}
              </div>
              <span class="text-[11px] text-stone-400 truncate">{{ m.user?.displayName || '匿名' }}</span>
            </div>
            <span class="text-[10px] text-stone-500 flex-shrink-0">{{ timeAgo(m.createdAt) }}</span>
          </div>

          <!-- Collect button -->
          <div class="pt-1 border-t border-stone-800/50" @click.stop>
            <CollectButton
              :memory-id="m.id"
              :collected="m.collected"
              @collect="onCollect"
            />
          </div>
        </div>
      </button>
    </div>

    <!-- Loading indicator -->
    <div v-if="loading" class="flex justify-center py-8">
      <div class="flex items-center gap-2 text-stone-500 text-sm">
        <span class="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
        <span class="w-2 h-2 rounded-full bg-amber-500 animate-pulse" style="animation-delay: 0.2s" />
        <span class="w-2 h-2 rounded-full bg-amber-500 animate-pulse" style="animation-delay: 0.4s" />
      </div>
    </div>

    <!-- Load more -->
    <div v-if="!loading && memories.length > 0" class="flex justify-center py-8">
      <button
        class="glass-button px-6 py-2 text-sm text-amber-400/80 hover:text-amber-300 transition-colors"
        @click="emit('load-more')"
      >
        加载更多记忆...
      </button>
    </div>

    <!-- Empty state -->
    <div v-if="!loading && memories.length === 0" class="text-center py-20">
      <Camera :size="40" class="mx-auto mb-4 text-stone-700" />
      <p class="text-stone-500">社区还在沉睡，期待第一条公开记忆</p>
    </div>
  </div>
</template>
