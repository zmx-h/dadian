<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMemoriesStore } from '@/stores/memories'
import { ChevronLeft, ChevronRight, Zap, MessageCircle } from '@lucide/vue'

const route = useRoute(); const router = useRouter()
const store = useMemoriesStore()
const memoryId = route.params.memoryId as string
const pageIndex = ref(0)
const showComments = ref(false)
const comments = ref<any[]>([])
const newComment = ref('')
const total = ref(0)

onMounted(async () => {
  await store.fetchDetail(memoryId)
  comments.value = await store.fetchComments(memoryId)
  total.value = store.currentPhotos?.length ?? 0
})

watch(() => route.params.memoryId, async (id: any) => {
  if (id) { await store.fetchDetail(id); pageIndex.value = 0; total.value = store.currentPhotos?.length ?? 0 }
})

const currentPhoto = () => (store.currentPhotos?.length ? store.currentPhotos[pageIndex.value] : null)
const isFirst = () => pageIndex.value === 0
const isLast = () => pageIndex.value >= total.value - 1

async function toggleCharge(commentId: string) { await store.toggleCharge(commentId) }
async function handleAddComment() {
  if (!newComment.value.trim()) return
  await store.addComment(memoryId, newComment.value)
  newComment.value = ''
  comments.value = await store.fetchComments(memoryId)
}
</script>

<template>
  <div v-if="store.currentMemory" class="memory-detail-page min-h-screen p-6 flex flex-col items-center">
    <button class="text-stone-500 hover:text-stone-300 mb-4 self-start flex items-center gap-1" @click="router.push('/hui-yi')">
      <ChevronLeft :size="16" /> 返回迷雾墙
    </button>

    <div class="flex items-center gap-4 w-full max-w-3xl justify-center">
      <button :disabled="isFirst()" class="glass-button p-2 disabled:opacity-20" @click="pageIndex--">
        <ChevronLeft :size="20" />
      </button>

      <div class="glass-card w-full max-w-lg p-0 overflow-hidden">
        <div class="h-[60vh] bg-stone-800 flex flex-col items-center justify-center relative">
          <span class="text-6xl mb-4">{{ (store.currentMemory as any).style === 'wangjiawei' ? '🎬' : '🤖' }}</span>
          <span class="text-stone-500 text-sm">{{ (currentPhoto() as any)?.caption || (store.currentMemory as any).title }}</span>
        </div>
      </div>

      <button :disabled="isLast()" class="glass-button p-2 disabled:opacity-20" @click="pageIndex++">
        <ChevronRight :size="20" />
      </button>
    </div>

    <div class="glass-card p-6 max-w-lg w-full mt-6 text-center">
      <div class="text-xs text-amber-400 mb-2">{{ (store.currentMemory as any).style }} 风格</div>
      <p class="text-sm text-stone-300 leading-relaxed">{{ (store.currentMemory as any).summary }}</p>
    </div>

    <div class="max-w-lg w-full mt-4">
      <button class="glass-button px-4 py-1.5 text-xs flex items-center gap-2" @click="showComments = !showComments">
        <MessageCircle :size="14" /> 留言 ({{ comments.length }})
      </button>
      <div v-if="showComments" class="mt-3 space-y-2">
        <div v-for="c in comments" :key="c.id" class="glass-card p-3">
          <div class="flex items-center justify-between">
            <span class="text-xs text-stone-400">{{ c.userName }}</span>
            <button class="flex items-center gap-1 text-xs" :class="c.chargedByMe ? 'text-amber-400' : 'text-stone-600'" @click="toggleCharge(c.id)">
              <Zap :size="10" :fill="c.chargedByMe ? 'currentColor' : 'none'" /> {{ c.chargeCount }}
            </button>
          </div>
          <div class="text-xs text-stone-300 mt-1">{{ c.content }}</div>
        </div>
        <div class="flex gap-2 mt-2">
          <input v-model="newComment" maxlength="100" placeholder="留言..."
            class="flex-1 bg-stone-900 border border-stone-700 rounded-lg px-3 py-1.5 text-xs text-stone-300 placeholder-stone-600" />
          <button class="glass-button px-3 text-xs text-amber-400" @click="handleAddComment">发送</button>
        </div>
      </div>
    </div>
  </div>
</template>
