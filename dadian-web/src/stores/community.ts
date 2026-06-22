import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import api from '@/api'

export interface ResonantUser {
  id: string
  displayName: string
  avatarUrl: string
  similarityScore: number
  commonTags: string[]
}

export interface Message {
  id: string
  senderId: string
  content: string
  createdAt: string
}

export interface InboxItem {
  userId: string
  displayName: string
  avatarUrl: string
  lastMessage: string
  lastMessageAt: string
  unreadCount: number
  mutualFollow: boolean
}

export interface FeedItem {
  id: string
  userId: string
  title: string
  style: string
  photos: string[]
  chargeCount: number
  summary: string
  visibility: string
  collected: boolean
  user: { id: string; displayName: string; avatarUrl: string }
  createdAt: string
  energy: number
}

export interface AlmanacEntry {
  rank: number
  userId: string
  displayName: string
  score: number
  avatarUrl: string
}

export const useCommunityStore = defineStore('community', () => {
  // ── Feed state ──────────────────────────────────────────────
  const feed = ref<FeedItem[]>([])
  const feedPage = ref(1)
  const feedHasMore = ref(true)
  const feedLoading = ref(false)

  // ── Resonant people ─────────────────────────────────────────
  const resonantUsers = ref<ResonantUser[]>([])
  const resonantSeed = ref(0)

  // ── Following ───────────────────────────────────────────────
  const followingIds = reactive<Record<string, boolean>>({})

  // ── Collections ─────────────────────────────────────────────
  const collectedIds = ref<Set<string>>(new Set())

  // ── Messages / Inbox ────────────────────────────────────────
  const inbox = ref<InboxItem[]>([])
  const currentConversation = ref<{ userId: string; displayName: string; avatarUrl: string; mutualFollow: boolean } | null>(null)
  const messages = ref<Message[]>([])
  const unreadMessages = ref(0)

  // ── Almanac ─────────────────────────────────────────────────
  const almanac = ref<Record<string, AlmanacEntry[]>>({})

  // ── Computed ────────────────────────────────────────────────
  const isFollowingMap = computed(() => ({ ...followingIds }))
  const isFollowing = (userId: string): boolean => !!followingIds[userId]
  const isCollected = (memoryId: string): boolean => collectedIds.value.has(memoryId)

  // ── Feed actions ────────────────────────────────────────────
  async function fetchFeed(reset = false) {
    if (feedLoading.value) return
    if (reset) { feedPage.value = 1; feed.value = []; feedHasMore.value = true }
    feedLoading.value = true
    try {
      const { data } = await api.get('/community/feed', {
        params: { page: feedPage.value, limit: 12 },
      })
      if (data.code === 0) {
        const items = data.data.memories || data.data || []
        if (reset) feed.value = items
        else feed.value = [...feed.value, ...items]
        feedHasMore.value = items.length >= 12
        feedPage.value += 1
      }
    } catch {
      // API not ready — use mock data on first load
      if (feed.value.length === 0) {
        feed.value = generateMockFeed()
        feedHasMore.value = false
      }
    } finally {
      feedLoading.value = false
    }
  }

  async function loadMore() {
    if (!feedHasMore.value || feedLoading.value) return
    await fetchFeed()
  }

  async function collectMemory(memoryId: string) {
    try {
      await api.post(`/outings/${memoryId}/collect`)
    } catch { /* stub */ }
    collectedIds.value.add(memoryId)
  }

  // ── Resonant users ──────────────────────────────────────────
  async function fetchResonantUsers() {
    try {
      const { data } = await api.get('/community/discover', { params: { limit: 5, seed: resonantSeed.value } })
      if (data.code === 0 && data.data?.length) {
        resonantUsers.value = data.data
        return
      }
    } catch { /* fall through to mock */ }
    resonantUsers.value = generateMockUsers()
  }

  function refreshResonantUsers() {
    resonantSeed.value += 1
    fetchResonantUsers()
  }

  // ── Follow ──────────────────────────────────────────────────
  async function follow(userId: string) {
    try { await api.post('/follows', { followedId: userId }) } catch { /* stub */ }
    followingIds[userId] = true
  }

  async function unfollow(userId: string) {
    try { await api.delete(`/follows/${userId}`) } catch { /* stub */ }
    delete followingIds[userId]
  }

  // ── Messages ────────────────────────────────────────────────
  async function fetchInbox() {
    try {
      const { data } = await api.get('/messages/inbox')
      if (data.code === 0) { inbox.value = data.data; return }
    } catch { /* fall through to mock */ }
    inbox.value = generateMockInbox()
  }

  async function fetchConversation(userId: string) {
    try {
      const { data } = await api.get(`/messages/conversation/${userId}`)
      if (data.code === 0) { messages.value = data.data.messages || []; return }
    } catch { /* fall through to mock */ }
    // mock messages
    messages.value = [] // stub
    const person = inbox.value.find(i => i.userId === userId)
    if (person) {
      currentConversation.value = {
        userId: person.userId,
        displayName: person.displayName,
        avatarUrl: person.avatarUrl,
        mutualFollow: person.mutualFollow,
      }
      messages.value = [
        { id: '1', senderId: userId, content: '嗨！最近有什么好片吗？', createdAt: '10:30' },
        { id: '2', senderId: 'me', content: '最近看了《繁花》，王家卫风格太绝了', createdAt: '10:32' },
        { id: '3', senderId: userId, content: '对对对！我就是因为这个才来搭电的', createdAt: '10:33' },
      ]
    }
  }

  async function sendMessage(userId: string, content: string) {
    const tempId = 'temp_' + Date.now()
    messages.value.push({ id: tempId, senderId: 'me', content, createdAt: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) })
    try {
      await api.post('/messages', { receiverId: userId, content })
    } catch { /* stub — message kept locally */ }
  }

  function openConversation(item: InboxItem) {
    currentConversation.value = { userId: item.userId, displayName: item.displayName, avatarUrl: item.avatarUrl, mutualFollow: item.mutualFollow }
    fetchConversation(item.userId)
  }

  // ── Almanac ─────────────────────────────────────────────────
  async function fetchAlmanac(type: string, limit = 5) {
    try {
      const { data } = await api.get('/almanac/leaderboard', { params: { type, limit } })
      if (data.code === 0) {
        almanac.value[type] = data.data
        return data.data
      }
    } catch { /* fall through to mock */ }
    const mock = generateMockAlmanac(type, limit)
    almanac.value[type] = mock
    return mock
  }

  function getAlmanac(type: string): AlmanacEntry[] {
    return almanac.value[type] || []
  }

  // ── Mock generators ─────────────────────────────────────────
  function generateMockFeed() {
    const users = ['沈默', '@chenlulu', '阿飞', '小满', '唐露', '@laowang', '苏打', '阿橘', '徐半仙', '林海绵', '豆子', '@7号特工']
    const titles = [
      '我在外滩的深夜', '梧桐区的咖啡故事', '雨天法租界漫步', '武康大楼的黄昏',
      '金陵东路的旧时光', '绍兴路书店偶遇', '复兴公园的早晨', '永嘉路一地金黄',
      '安福路的摄影日', '巨鹿路深夜食堂记', '思南路的胶片拍', '愚园路自行车日记',
    ]
    const styles = ['wangjiawei', 'cyberpunk', 'documentary', 'wangjiawei', 'cyberpunk', 'documentary', 'wangjiawei', 'cyberpunk', 'documentary', 'wangjiawei', 'documentary', 'wangjiawei']
    return Array.from({ length: 12 }, (_, i) => ({
      id: `feed-mock-${i}`,
      userId: `user-${i % 6}`,
      title: titles[i],
      style: styles[i],
      photos: [],
      chargeCount: Math.floor(Math.random() * 20) + 1,
      summary: '这是一段关于上海城市探索的记忆...',
      visibility: 'public',
      collected: i < 3,
      user: {
        id: `user-${i % 6}`,
        displayName: users[i],
        avatarUrl: '',
      },
      createdAt: new Date(Date.now() - Math.floor(Math.random() * 7 * 86400000)).toISOString(),
      energy: Math.floor(Math.random() * 40) + 30,
    }))
  }

  function generateMockUsers(): ResonantUser[] {
    return [
      { id: 'ru-1', displayName: '沈默', avatarUrl: '', similarityScore: 92, commonTags: ['低电量', '咖啡党', '王家卫'] },
      { id: 'ru-2', displayName: '阿飞', avatarUrl: '', similarityScore: 87, commonTags: ['赛博朋克', '深夜档', '独行者'] },
      { id: 'ru-3', displayName: '小满', avatarUrl: '', similarityScore: 83, commonTags: ['纪录片', '城市漫步', '胶片摄影'] },
      { id: 'ru-4', displayName: '唐露', avatarUrl: '', similarityScore: 79, commonTags: ['咖啡党', '法租界', '文艺片'] },
      { id: 'ru-5', displayName: '徐半仙', avatarUrl: '', similarityScore: 75, commonTags: ['老上海', '复古', '慢节奏'] },
    ]
  }

  function generateMockInbox(): InboxItem[] {
    return [
      { userId: 'ru-1', displayName: '沈默', avatarUrl: '', lastMessage: '那个外滩的黄昏记忆太棒了', lastMessageAt: '5分钟前', unreadCount: 2, mutualFollow: true },
      { userId: 'ru-2', displayName: '阿飞', avatarUrl: '', lastMessage: '下次一起去拍赛博夜景吧', lastMessageAt: '1小时前', unreadCount: 0, mutualFollow: true },
      { userId: 'ru-3', displayName: '小满', avatarUrl: '', lastMessage: '你也喜欢胶片摄影？', lastMessageAt: '昨天', unreadCount: 1, mutualFollow: false },
      { userId: 'ru-4', displayName: '唐露', avatarUrl: '', lastMessage: '好的！', lastMessageAt: '3天前', unreadCount: 0, mutualFollow: true },
    ]
  }

  function generateMockAlmanac(type: string, limit: number): AlmanacEntry[] {
    const names = ['沈默', '阿飞', '唐露', '小满', '徐半仙', '陈露露', '@7号特工', '豆子']
    return Array.from({ length: limit }, (_, i) => ({
      rank: i + 1,
      userId: `al-${type}-${i}`,
      displayName: names[i] || `用户${i}`,
      score: Math.floor(100 - i * 8),
      avatarUrl: '',
    }))
  }

  return {
    feed, feedPage, feedHasMore, feedLoading,
    resonantUsers, resonantSeed,
    followingIds, collectedIds,
    inbox, currentConversation, messages, unreadMessages,
    almanac,
    isFollowing, isFollowingMap, isCollected,
    fetchFeed, loadMore, collectMemory,
    fetchResonantUsers, refreshResonantUsers,
    follow, unfollow,
    fetchInbox, fetchConversation, sendMessage, openConversation,
    fetchAlmanac, getAlmanac,
  }
})
