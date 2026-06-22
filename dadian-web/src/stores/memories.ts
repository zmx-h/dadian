import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export const useMemoriesStore = defineStore('memories', () => {
  const memories = ref<any[]>([])
  const currentMemory = ref<any>(null)
  const currentPhotos = ref<any[]>([])
  const filter = ref('all')

  async function fetchMemories(visibility='private') {
    const { data } = await api.get('/memories', { params: { visibility, limit: 20 } })
    if (data.code===0) memories.value = data.data
  }
  async function fetchDetail(id: string) {
    const { data } = await api.get(`/memories/${id}`)
    if (data.code===0) { currentMemory.value = data.data.memory; currentPhotos.value = data.data.photos }
  }
  async function generate(outingId: string, style='wangjiawei', visibility='private') {
    const { data } = await api.post('/memories/generate', { outingId, style, visibility })
    if (data.code===0) { await fetchMemories(); return data.data }
  }
  async function updateVisibility(id: string, visibility: string) {
    await api.patch(`/memories/${id}/visibility`, { visibility })
  }
  async function deleteMemory(id: string) {
    await api.delete(`/memories/${id}`)
    await fetchMemories(); currentMemory.value = null
  }
  async function addComment(memoryId: string, content: string) {
    await api.post(`/memories/${memoryId}/comments`, { content })
  }
  async function toggleCharge(commentId: string) {
    await api.post(`/comments/${commentId}/charge`)
  }
  async function fetchComments(memoryId: string) {
    const { data } = await api.get(`/memories/${memoryId}/comments`)
    return data.code===0 ? data.data : []
  }
  return { memories, currentMemory, currentPhotos, filter,
    fetchMemories, fetchDetail, generate, updateVisibility, deleteMemory, addComment, toggleCharge, fetchComments }
})
