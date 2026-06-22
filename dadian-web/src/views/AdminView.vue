<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAdminStore, type PendingMemory } from '@/stores/admin'
import {
  Users, MapPin, Camera, Cpu, Check, X, Save, MessageSquareText, Dice6, LayoutDashboard,
} from '@lucide/vue'

const admin = useAdminStore()
const { stats, pendingMemories, pendingComments, diceConfigs, loading } = storeToRefs(admin)

type Tab = 'stats' | 'review' | 'dice'
const activeTab = ref<Tab>('stats')

const toastMsg = ref('')
const toastShow = ref(false)

function flash(msg: string) {
  toastMsg.value = msg
  toastShow.value = true
  setTimeout(() => { toastShow.value = false }, 2500)
}

onMounted(() => {
  admin.fetchStats()
  admin.fetchPendingMemories()
  admin.fetchPendingComments()
  admin.fetchDiceConfigs()
})

// ---- Review helper ----
const allPending = computed(() => {
  const m = pendingMemories.value.map((i) => ({ ...i, _type: 'memory' as const }))
  const c = pendingComments.value.map((i) => ({ ...i, _type: 'comment' as const }))
  return [...m, ...c].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
})

async function handleApprove(item: PendingMemory & { _type: 'memory' | 'comment' }) {
  if (item._type === 'memory') await admin.approveMemory(item.id)
  else await admin.approveComment(item.id)
  flash('已通过')
}

async function handleReject(item: PendingMemory & { _type: 'memory' | 'comment' }) {
  if (item._type === 'memory') await admin.rejectMemory(item.id)
  else await admin.rejectComment(item.id)
  flash('已驳回')
}

async function handleSaveDice() {
  await admin.saveDiceConfigs()
  flash('骰子文案已保存')
}
</script>

<template>
  <div class="admin-page min-h-screen px-4 py-6 max-w-3xl mx-auto space-y-6">

    <!-- TOAST -->
    <Transition name="toast">
      <div v-if="toastShow"
        class="fixed top-6 left-1/2 -translate-x-1/2 z-[9999] px-5 py-3 rounded-xl text-sm font-medium
               bg-amber-500/90 text-stone-950 shadow-lg backdrop-blur-sm">
        {{ toastMsg }}
      </div>
    </Transition>

    <h1 class="text-xl font-bold flex items-center gap-2">
      <LayoutDashboard :size="22" class="text-amber-400" /> 管理面板
    </h1>

    <!-- TABS -->
    <div class="flex gap-2 border-b border-white/10 pb-2">
      <button
        v-for="t in ([
          { key: 'stats' as Tab, label: '数据看板', icon: LayoutDashboard },
          { key: 'review' as Tab, label: '内容审核', icon: MessageSquareText },
          { key: 'dice' as Tab, label: '骰子文案', icon: Dice6 },
        ])"
        :key="t.key"
        class="flex items-center gap-1.5 px-4 py-2 rounded-t-lg text-sm font-medium transition-colors"
        :class="activeTab === t.key
          ? 'text-amber-400 border-b-2 border-amber-500 bg-amber-500/5'
          : 'text-stone-500 hover:text-stone-300'"
        @click="activeTab = t.key"
      >
        <component :is="t.icon" :size="14" />
        {{ t.label }}
      </button>
    </div>

    <!-- ===== TAB: 数据看板 ===== -->
    <div v-if="activeTab === 'stats'" class="grid grid-cols-2 sm:grid-cols-4 gap-3">
      <div class="glass-card p-4 text-center">
        <div class="flex items-center justify-center gap-1 text-stone-400 text-xs mb-1">
          <Users :size="12" /> DAU
        </div>
        <div class="text-2xl font-bold text-amber-400">{{ stats?.dau ?? '--' }}</div>
      </div>
      <div class="glass-card p-4 text-center">
        <div class="flex items-center justify-center gap-1 text-stone-400 text-xs mb-1">
          <MapPin :size="12" /> 出行数
        </div>
        <div class="text-2xl font-bold text-amber-400">{{ stats?.outingCount ?? '--' }}</div>
      </div>
      <div class="glass-card p-4 text-center">
        <div class="flex items-center justify-center gap-1 text-stone-400 text-xs mb-1">
          <Camera :size="12" /> 回忆录数
        </div>
        <div class="text-2xl font-bold text-amber-400">{{ stats?.memoryCount ?? '--' }}</div>
      </div>
      <div class="glass-card p-4 text-center">
        <div class="flex items-center justify-center gap-1 text-stone-400 text-xs mb-1">
          <Cpu :size="12" /> AI Token
        </div>
        <div class="text-2xl font-bold text-amber-400">{{ stats?.aiTokenUsed ?? '--' }}</div>
      </div>
    </div>

    <!-- ===== TAB: 内容审核 ===== -->
    <div v-if="activeTab === 'review'" class="space-y-3">
      <div v-if="allPending.length === 0" class="glass-card p-8 text-center text-stone-500 text-sm">
        暂无待审核内容
      </div>
      <div
        v-for="item in allPending"
        :key="item.id"
        class="glass-card p-4 flex items-start justify-between gap-4"
      >
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-[11px] text-stone-500">{{ item.authorName }}</span>
            <span
              class="text-[10px] px-1.5 py-0.5 rounded-full"
              :class="item._type === 'memory'
                ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                : 'bg-blue-500/10 text-blue-400 border border-blue-500/20'"
            >
              {{ item._type === 'memory' ? '回忆录' : '评论' }}
            </span>
          </div>
          <p class="text-sm text-stone-300 leading-relaxed line-clamp-3">{{ item.text }}</p>
          <p class="text-[10px] text-stone-600 mt-1">{{ item.createdAt }}</p>
        </div>
        <div class="flex gap-2 shrink-0">
          <button
            class="p-2 rounded-full bg-green-500/10 text-green-400 border border-green-500/20
                   hover:bg-green-500/20 transition-colors"
            title="通过"
            @click="handleApprove(item)"
          >
            <Check :size="14" />
          </button>
          <button
            class="p-2 rounded-full bg-red-500/10 text-red-400 border border-red-500/20
                   hover:bg-red-500/20 transition-colors"
            title="驳回"
            @click="handleReject(item)"
          >
            <X :size="14" />
          </button>
        </div>
      </div>
    </div>

    <!-- ===== TAB: 骰子文案 ===== -->
    <div v-if="activeTab === 'dice'" class="space-y-4">
      <div
        v-for="cfg in diceConfigs"
        :key="cfg.key"
        class="glass-card p-4 space-y-2"
      >
        <label class="text-xs font-medium text-stone-400">{{ cfg.scene }} · {{ cfg.key }}</label>
        <textarea
          v-model="cfg.persuasionText"
          rows="3"
          class="w-full bg-stone-800 text-stone-200 text-sm rounded-lg px-3 py-2 border border-stone-700
                 focus:outline-none focus:border-amber-500/50 resize-none"
        />
      </div>
      <button
        class="glass-button px-6 py-2.5 text-sm font-medium text-amber-400
               hover:bg-amber-500/20 transition-colors disabled:opacity-50 flex items-center gap-2"
        :disabled="loading"
        @click="handleSaveDice"
      >
        <Save :size="14" />
        {{ loading ? '保存中...' : '保存配置' }}
      </button>
    </div>

  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -12px);
}
</style>
