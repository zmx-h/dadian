import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

// -------------------- Types --------------------
export interface AdminStats {
  dau: number
  outingCount: number
  memoryCount: number
  aiTokenUsed: number
}

export interface PendingMemory {
  id: string
  authorName: string
  text: string
  createdAt: string
}

export interface PendingComment {
  id: string
  authorName: string
  text: string
  memoryId: string
  createdAt: string
}

export interface DiceConfig {
  key: string
  persuasionText: string
  scene: string
}

// -------------------- Store --------------------
export const useAdminStore = defineStore('admin', () => {
  const stats = ref<AdminStats | null>(null)
  const pendingMemories = ref<PendingMemory[]>([])
  const pendingComments = ref<PendingComment[]>([])
  const diceConfigs = ref<DiceConfig[]>([])
  const loading = ref(false)

  // ---- Stats ----
  async function fetchStats() {
    try {
      const { data } = await api.get('/admin/stats')
      stats.value = data.data ?? data
    } catch {
      // mock fallback
      stats.value = { dau: 128, outingCount: 47, memoryCount: 23, aiTokenUsed: 18420 }
    }
  }

  // ---- Pending Memories ----
  async function fetchPendingMemories() {
    try {
      const { data } = await api.get('/admin/memories/pending')
      pendingMemories.value = data.data ?? data ?? []
    } catch {
      pendingMemories.value = []
    }
  }

  async function approveMemory(id: string) {
    await api.post(`/admin/memories/${id}/approve`)
    pendingMemories.value = pendingMemories.value.filter((m) => m.id !== id)
  }

  async function rejectMemory(id: string) {
    await api.post(`/admin/memories/${id}/reject`)
    pendingMemories.value = pendingMemories.value.filter((m) => m.id !== id)
  }

  // ---- Pending Comments ----
  async function fetchPendingComments() {
    try {
      const { data } = await api.get('/admin/comments/pending')
      pendingComments.value = data.data ?? data ?? []
    } catch {
      pendingComments.value = []
    }
  }

  async function approveComment(id: string) {
    await api.post(`/admin/comments/${id}/approve`)
    pendingComments.value = pendingComments.value.filter((c) => c.id !== id)
  }

  async function rejectComment(id: string) {
    await api.post(`/admin/comments/${id}/reject`)
    pendingComments.value = pendingComments.value.filter((c) => c.id !== id)
  }

  // ---- Dice Configs ----
  async function fetchDiceConfigs() {
    try {
      const { data } = await api.get('/admin/dice-configs')
      diceConfigs.value = data.data ?? data ?? []
    } catch {
      // mock fallback
      diceConfigs.value = [
        { key: 'persuasion_default',  persuasionText: '来吧，今晚的剧本已经写好了，就差你这一笔。',  scene: 'default' },
        { key: 'persuasion_wangjiawei',  persuasionText: '有些地方，一个人去太安静了，带上我，一起去热闹一下。',  scene: 'wangjiawei' },
        { key: 'persuasion_npc',  persuasionText: '系统检测到今晚出门概率87.6%，建议立即执行。',  scene: 'npc' },
      ]
    }
  }

  async function saveDiceConfigs() {
    loading.value = true
    try {
      await api.put('/admin/dice-configs', { configs: diceConfigs.value })
    } finally {
      loading.value = false
    }
  }

  return {
    stats,
    pendingMemories,
    pendingComments,
    diceConfigs,
    loading,
    fetchStats,
    fetchPendingMemories,
    fetchPendingComments,
    approveMemory,
    rejectMemory,
    approveComment,
    rejectComment,
    fetchDiceConfigs,
    saveDiceConfigs,
  }
})
