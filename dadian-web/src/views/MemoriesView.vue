<script setup lang="ts">
import { onMounted } from 'vue'
import { useMemoriesStore } from '@/stores/memories'

const store = useMemoriesStore()

onMounted(async () => { await store.fetchMemories() })
</script>

<template>
  <div class="memories-page min-h-screen p-6">
    <div class="flex items-center gap-4 mb-6">
      <button v-for="f in ['all','solo','team']" :key="f"
        class="text-xs px-3 py-1 rounded-full transition-colors"
        :class="store.filter===f ? 'bg-amber-500/20 text-amber-400' : 'text-stone-500 hover:text-stone-300'"
        @click="store.filter=f">
        {{ f==='all'?'全部':f==='solo'?'单人':'组队' }}
      </button>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
      <button v-for="m in store.memories" :key="(m as any).id"
        class="glass-card p-0 overflow-hidden text-left transition-all duration-300 hover:scale-[1.03] hover:border-amber-500/30 cursor-pointer"
        @click="$router.push('/hui-yi/'+(m as any).id)">
        <div class="h-40 bg-stone-800 flex items-center justify-center">
          <span class="text-3xl">{{ (m as any).style==='wangjiawei'?'🎬':(m as any).style==='cyberpunk'?'🤖':'💿' }}</span>
        </div>
        <div class="p-3">
          <div class="text-xs text-amber-400 mb-1">{{ (m as any).style }}</div>
          <div class="text-sm font-medium text-stone-200 truncate">{{ (m as any).title }}</div>
          <div class="text-[10px] text-stone-500 mt-1 truncate">{{ (m as any).summary }}</div>
        </div>
      </button>
    </div>

    <div v-if="!store.memories.length" class="text-center text-stone-600 py-20">
      还没有回忆录，完成一次出行来生成吧
    </div>
  </div>
</template>
