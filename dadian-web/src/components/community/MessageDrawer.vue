<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useCommunityStore } from '@/stores/community'
import { X, ChevronLeft, Send, Lock, MessageCircle } from '@lucide/vue'

const props = defineProps<{
  visible: boolean
  initialUserId?: string
}>()

const emit = defineEmits<{
  close: []
  openConversation: [userId: string]
}>()

const store = useCommunityStore()
const messageInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const isMobile = ref(false)

const isMutualFollow = computed(() => {
  return store.currentConversation?.mutualFollow ?? false
})

function checkMobile() {
  isMobile.value = window.innerWidth < 768
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(() => store.messages.length, scrollToBottom)
watch(() => store.currentConversation, scrollToBottom)

watch(() => props.visible, async (v) => {
  if (v) {
    document.body.style.overflow = 'hidden'
    await store.fetchInbox()
    if (props.initialUserId) {
      store.openConversation(store.inbox.find(i => i.userId === props.initialUserId) || store.inbox[0])
    }
  } else {
    document.body.style.overflow = ''
  }
})

async function handleSelectConversation(userId: string) {
  const item = store.inbox.find(i => i.userId === userId)
  if (item) {
    store.openConversation(item)
    scrollToBottom()
  }
}

async function handleSend() {
  const content = messageInput.value.trim()
  if (!content || !store.currentConversation) return
  await store.sendMessage(store.currentConversation.userId, content)
  messageInput.value = ''
  scrollToBottom()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function closeDrawer() {
  emit('close')
}

function backToInbox() {
  store.currentConversation = null
  store.messages = []
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <!-- Overlay -->
    <div
      v-if="visible"
      class="fixed inset-0 z-50 flex"
      :class="isMobile ? 'items-end' : 'justify-end'"
    >
      <div class="absolute inset-0 glass-overlay" @click="closeDrawer" />

      <!-- Drawer body -->
      <div
        class="message-drawer relative z-10 flex flex-col"
        :class="isMobile ? 'drawer-bottom' : 'drawer-side'"
      >
        <!-- Header -->
        <div class="drawer-header">
          <button
            v-if="store.currentConversation"
            class="p-1 rounded-lg text-stone-500 hover:text-stone-300 transition-colors"
            @click="backToInbox"
          >
            <ChevronLeft :size="20" />
          </button>
          <div v-else class="w-7" />
          <span class="text-sm font-medium text-stone-200 truncate flex-1 text-center">
            {{ store.currentConversation ? store.currentConversation.displayName : '消息' }}
          </span>
          <button
            class="p-1 rounded-lg text-stone-500 hover:text-stone-300 transition-colors"
            @click="closeDrawer"
          >
            <X :size="20" />
          </button>
        </div>

        <!-- Inbox list -->
        <div v-if="!store.currentConversation" class="flex-1 overflow-y-auto">
          <div v-if="!store.inbox.length" class="flex flex-col items-center justify-center h-full text-stone-600 gap-2">
            <MessageCircle :size="36" class="text-stone-700" />
            <p class="text-sm">暂无消息</p>
            <p class="text-xs">在社区找到同频人开始对话吧</p>
          </div>
          <button
            v-for="item in store.inbox"
            :key="item.userId"
            class="inbox-item w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-stone-800/50 transition-colors relative"
            @click="handleSelectConversation(item.userId)"
          >
            <div class="flex-shrink-0 w-10 h-10 rounded-full bg-stone-800 flex items-center justify-center text-sm text-stone-300">
              {{ item.displayName[0] }}
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-sm text-stone-200 font-medium">{{ item.displayName }}</span>
                <span v-if="!item.mutualFollow" class="text-[9px] px-1.5 py-0.5 rounded bg-stone-800 text-stone-500">未互关</span>
              </div>
              <div class="text-xs text-stone-500 truncate mt-0.5">{{ item.lastMessage }}</div>
            </div>
            <div class="flex flex-col items-end gap-1 flex-shrink-0">
              <span class="text-[10px] text-stone-600">{{ item.lastMessageAt }}</span>
              <span
                v-if="item.unreadCount > 0"
                class="w-4 h-4 rounded-full bg-amber-500 text-[10px] text-stone-950 flex items-center justify-center font-medium"
              >
                {{ item.unreadCount }}
              </span>
            </div>
          </button>
        </div>

        <!-- Conversation -->
        <div v-else class="flex-1 flex flex-col min-h-0">
          <!-- Messages -->
          <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-3">
            <div
              v-for="msg in store.messages"
              :key="msg.id"
              class="flex"
              :class="msg.senderId === 'me' ? 'justify-end' : 'justify-start'"
            >
              <div
                class="message-bubble max-w-[75%] px-3 py-2 rounded-2xl text-sm"
                :class="msg.senderId === 'me'
                  ? 'bg-amber-500/15 text-amber-100 rounded-br-md border border-amber-500/20'
                  : 'bg-stone-800 text-stone-200 rounded-bl-md border border-stone-700/30'"
              >
                {{ msg.content }}
                <div class="text-[9px] mt-1 opacity-50" :class="msg.senderId === 'me' ? 'text-right' : 'text-left'">
                  {{ msg.createdAt }}
                </div>
              </div>
            </div>
          </div>

          <!-- Input area -->
          <div class="p-3 border-t border-stone-800">
            <!-- Not mutual follow -->
            <div v-if="!isMutualFollow" class="flex items-center gap-2 px-3 py-2 rounded-xl bg-stone-800/60 border border-stone-700/30 mb-2">
              <Lock :size="12" class="text-stone-500 flex-shrink-0" />
              <span class="text-xs text-stone-500">互相关注后可发消息</span>
            </div>

            <div class="flex items-end gap-2">
              <textarea
                v-model="messageInput"
                :disabled="!isMutualFollow"
                class="flex-1 bg-stone-800 border border-stone-700 rounded-xl px-3 py-2 text-sm text-stone-200 placeholder-stone-600 resize-none focus:outline-none focus:border-amber-500/50 transition-colors"
                :class="{ 'opacity-50 cursor-not-allowed': !isMutualFollow }"
                rows="1"
                :placeholder="isMutualFollow ? '写点什么...' : '互关后才能聊天'"
                @keydown="handleKeydown"
              />
              <button
                :disabled="!isMutualFollow || !messageInput.trim()"
                class="p-2 rounded-xl transition-all flex-shrink-0"
                :class="isMutualFollow && messageInput.trim()
                  ? 'bg-amber-500 text-stone-950 hover:bg-amber-400'
                  : 'bg-stone-800 text-stone-600 cursor-not-allowed'"
                @click="handleSend"
              >
                <Send :size="16" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.message-drawer {
  background: rgba(12, 10, 9, 0.98);
  backdrop-filter: blur(20px);
}

/* Desktop: slide from right */
.drawer-side {
  width: 420px;
  height: 100vh;
  border-left: 1px solid rgba(255, 255, 255, 0.06);
  animation: slideInRight 0.25s ease-out;
}

/* Mobile: bottom sheet */
.drawer-bottom {
  width: 100%;
  max-height: 80vh;
  border-radius: 16px 16px 0 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  animation: slideInUp 0.3s ease-out;
}

.drawer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.inbox-item {
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  cursor: pointer;
}

@keyframes slideInRight {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

@keyframes slideInUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
</style>
