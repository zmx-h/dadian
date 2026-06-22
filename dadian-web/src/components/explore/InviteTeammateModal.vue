<script setup lang="ts">
import { ref } from 'vue'
import { X, Search, UserPlus } from '@lucide/vue'
import { useExploreStore } from '@/stores/explore'

const emit = defineEmits<{ close: [] }>()
const explore = useExploreStore()

const searchQuery = ref('')
const invitedIds = ref<Set<string>>(new Set())
const invitingId = ref<string | null>(null)

const mockUsers = [
  { id: 'u1', nickname: '张小明', phone: '138****1234' },
  { id: 'u2', nickname: '李小红', phone: '139****5678' },
  { id: 'u3', nickname: '王大锤', phone: '137****9012' },
  { id: 'u4', nickname: '赵灵儿', phone: '136****3456' },
  { id: 'u5', nickname: '陈大文', phone: '135****7890' },
]

const filteredUsers = ref(mockUsers)

function search() {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) {
    filteredUsers.value = mockUsers
    return
  }
  filteredUsers.value = mockUsers.filter(
    u => u.nickname.toLowerCase().includes(q) || u.phone.includes(q),
  )
}

async function invite(userId: string) {
  invitingId.value = userId
  try {
    await explore.inviteTeammate(userId)
    invitedIds.value.add(userId)
  } finally {
    invitingId.value = null
  }
}
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center">
    <div class="glass-overlay fixed inset-0" @click="emit('close')" />
    <div class="glass-card relative z-10 w-[90vw] max-w-sm p-5 flex flex-col gap-4 max-h-[80vh] overflow-y-auto">
      <div class="flex items-center justify-between">
        <h3 class="text-sm font-semibold text-stone-200">邀请搭子</h3>
        <button class="text-stone-500 hover:text-stone-300" @click="emit('close')">
          <X :size="18" />
        </button>
      </div>

      <div class="flex items-center gap-2 glass-card px-3 py-2">
        <Search :size="14" class="text-stone-500" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索手机号或昵称..."
          class="bg-transparent text-xs text-stone-300 placeholder-stone-600 outline-none flex-1"
          @input="search"
        />
      </div>

      <ul class="flex flex-col gap-1">
        <li
          v-for="u in filteredUsers"
          :key="u.id"
          class="flex items-center gap-3 py-2 px-2 rounded-lg hover:bg-stone-800/40 transition-colors"
        >
          <div class="w-8 h-8 rounded-full bg-violet-600/30 flex items-center justify-center text-xs text-violet-300 font-semibold flex-shrink-0">
            {{ u.nickname.charAt(0) }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-xs text-stone-300 truncate">{{ u.nickname }}</div>
            <div class="text-[10px] text-stone-500">{{ u.phone }}</div>
          </div>
          <button
            v-if="invitedIds.has(u.id)"
            class="text-[10px] text-stone-600 bg-stone-800/50 px-3 py-1 rounded-full cursor-default"
            disabled
          >
            已邀请
          </button>
          <button
            v-else
            class="glass-button px-3 py-1 text-xs flex items-center gap-1 disabled:opacity-40"
            :disabled="invitingId === u.id"
            @click="invite(u.id)"
          >
            <UserPlus :size="12" />
            {{ invitingId === u.id ? '发送中...' : '邀请' }}
          </button>
        </li>
      </ul>

      <p v-if="!filteredUsers.length" class="text-xs text-stone-600 text-center py-4">
        未找到匹配的用户
      </p>
    </div>
  </div>
</template>
