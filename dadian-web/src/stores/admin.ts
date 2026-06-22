import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

// -------------------- Types --------------------
export interface AdminStats {
  dau: number
  outings: number
  memories: number
  aiTokens: number
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
  label: string
  content: string
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
      stats.value = { dau: 128, outings: 47, memories: 23, aiTokens: 18420 }
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
        { key: 'persuade_low',  label: '低社交能量 (0-30)',   content: '今天不太想动，随便去个地方吧。' },
        { key: 'persuade_mid',  label: '中社交能量 (31-70)',  content: '找个有意思的地方，别太远。' },
        { key: 'persuade_high', label: '高社交能量 (71-100)', content: '来吧！给我最惊喜的目的地！' },
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
