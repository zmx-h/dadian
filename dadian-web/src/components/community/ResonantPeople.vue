<script setup lang="ts">
import { Sparkles, RefreshCw, MessageCircle, UserPlus, UserMinus } from '@lucide/vue'
import { useCommunityStore } from '@/stores/community'

const store = useCommunityStore()

defineProps<{
  users: Array<{
    id: string; displayName: string; avatarUrl: string
    similarityScore: number; commonTags: string[]
  }>
}>()

const emit = defineEmits<{ refresh: []; message: [id: string] }>()
</script>

<template>
  <div class="resonant-people">
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-1.5">
        <Sparkles :size="16" class="text-amber-400" />
        <span class="text-xs font-medium text-stone-300 tracking-wide">同频共振</span>
      </div>
      <button class="p-1 rounded-full text-stone-500 hover:text-amber-400 transition-colors" @click="emit('refresh')">
        <RefreshCw :size="14" />
      </button>
    </div>

    <div class="space-y-2">
      <div v-for="u in users" :key="u.id" class="glass-card p-3 flex items-center gap-3 group">
        <div class="w-10 h-10 rounded-full bg-stone-700 flex items-center justify-center text-sm text-stone-300 flex-shrink-0">
          {{ u.displayName[0] }}
        </div>
        <div class="flex-1 min-w-0">
          <div class="text-xs text-stone-300 font-medium truncate">{{ u.displayName }}</div>
          <div class="text-[10px] flex items-center gap-1 mt-0.5">
            <span class="font-mono" :class="u.similarityScore>=90?'text-amber-400 amber-breathe':u.similarityScore>=70?'text-amber-400/70':'text-amber-400/40'">
              共振 {{ u.similarityScore }}%
            </span>
          </div>
          <div class="flex flex-wrap gap-1 mt-1">
            <span v-for="tag in u.commonTags?.slice(0,3)" :key="tag" class="text-[9px] px-1 py-0.5 rounded bg-stone-800 text-stone-500">{{ tag }}</span>
          </div>
        </div>
        <div class="flex gap-1">
          <button class="p-1.5 rounded-full text-stone-600 hover:text-amber-400 transition-colors" @click="emit('message', u.id)">
            <MessageCircle :size="14" />
          </button>
          <button
            class="p-1.5 rounded-full transition-colors"
            :class="store.isFollowing(u.id) ? 'text-amber-400' : 'text-stone-600 hover:text-amber-400'"
            @click="store.isFollowing(u.id) ? store.unfollow(u.id) : store.follow(u.id)"
          >
            <component :is="store.isFollowing(u.id) ? UserMinus : UserPlus" :size="14" />
          </button>
        </div>
      </div>
    </div>

    <button class="w-full mt-2 text-[10px] text-stone-600 hover:text-stone-400 transition-colors py-1" @click="emit('refresh')">
      换一批
    </button>
  </div>
</template>
