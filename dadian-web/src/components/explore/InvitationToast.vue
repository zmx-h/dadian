<script setup lang="ts">
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useExploreStore } from '@/stores/explore'
import { Check, X, ChevronDown, ChevronUp, Bell } from '@lucide/vue'

const explore = useExploreStore()
const { pendingInvitations } = storeToRefs(explore)

const expanded = ref(false)

function toggleExpanded() {
  expanded.value = !expanded.value
}

async function handleRespond(invitationId: string, accept: boolean) {
  await explore.respondToInvitation(invitationId, accept)
}
</script>

<template>
  <div v-if="pendingInvitations.length" class="fixed top-4 left-1/2 -translate-x-1/2 z-40 w-[90vw] max-w-sm">
    <div class="glass-card border-amber-500/30 px-4 py-3 cursor-pointer" @click="toggleExpanded">
      <div class="flex items-center gap-2">
        <Bell :size="14" class="text-amber-400 amber-breathe" />
        <span class="text-xs text-amber-300 font-medium">
          {{ pendingInvitations.length }} 个组队邀请
        </span>
        <component :is="expanded ? ChevronUp : ChevronDown" :size="14" class="text-stone-500 ml-auto" />
      </div>
    </div>

    <div v-if="expanded" class="glass-card mt-2 p-3 flex flex-col gap-2 max-h-64 overflow-y-auto">
      <div
        v-for="inv in pendingInvitations"
        :key="inv.id"
        class="flex items-center gap-3 py-1.5"
      >
        <div class="w-7 h-7 rounded-full bg-amber-500/20 flex items-center justify-center text-[10px] text-amber-400 font-semibold flex-shrink-0">
          {{ inv.inviterName?.charAt(0) || '?' }}
        </div>
        <div class="flex-1 min-w-0">
          <div class="text-xs text-stone-300 truncate">
            {{ inv.inviterName }} 邀请你加入 {{ inv.outingTitle }}
          </div>
        </div>
        <div class="flex items-center gap-1 flex-shrink-0">
          <button
            class="w-7 h-7 rounded-full bg-success/20 flex items-center justify-center hover:bg-success/30 transition-colors"
            @click.stop="handleRespond(inv.id, true)"
          >
            <Check :size="14" class="text-success" />
          </button>
          <button
            class="w-7 h-7 rounded-full bg-danger/20 flex items-center justify-center hover:bg-danger/30 transition-colors"
            @click.stop="handleRespond(inv.id, false)"
          >
            <X :size="14" class="text-danger" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
