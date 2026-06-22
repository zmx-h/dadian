<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Teammate } from '@/stores/explore'
import { UserPlus, Radio, Circle } from '@lucide/vue'
import InviteTeammateModal from '@/components/explore/InviteTeammateModal.vue'

const props = defineProps<{
  teammates: Teammate[]
  isRemote?: boolean
}>()

const showInvite = ref(false)

const teamLabel = computed(() => {
  if (props.isRemote) return '📡 远程联机'
  if (props.teammates.length) return `${props.teammates.length} 人小队`
  return null
})

function roleBadge(role: string) {
  if (role === 'agent') return '发起者'
  if (role === 'tank') return '坦克'
  if (role === 'explorer') return '探索者'
  if (role === 'support') return '辅助'
  return role
}
</script>

<template>
  <div class="glass-card p-4 flex flex-col gap-3">
    <div class="flex items-center justify-between">
      <h4 class="text-xs text-stone-500" v-if="teamLabel">{{ teamLabel }}</h4>
      <h4 class="text-xs text-stone-500" v-else>队友状态</h4>
      <span v-if="isRemote" class="text-[10px] text-violet-400 flex items-center gap-1">
        <Radio :size="10" /> 远程
      </span>
    </div>

    <!-- Empty state -->
    <div v-if="!teammates.length" class="flex flex-col items-center gap-3 py-4">
      <p class="text-xs text-stone-600">一个人出发 · 邀请搭子吗？</p>
      <button class="glass-button px-4 py-1.5 text-xs flex items-center gap-1.5" @click="showInvite = true">
        <UserPlus :size="14" /> 邀请搭子
      </button>
    </div>

    <!-- Teammate list -->
    <template v-else>
      <div v-for="t in teammates" :key="t.id" class="flex items-center gap-3 py-1.5">
        <div class="relative flex-shrink-0">
          <div class="w-8 h-8 rounded-full bg-violet-600/30 flex items-center justify-center text-xs text-violet-300 font-semibold">
            {{ t.displayName?.charAt(0) || '?' }}
          </div>
          <Circle
            v-if="isRemote"
            :size="8"
            class="absolute -bottom-0.5 -right-0.5 text-violet-400 fill-violet-400"
          />
          <Circle
            v-else
            :size="8"
            class="absolute -bottom-0.5 -right-0.5 text-success fill-success"
          />
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-1.5">
            <span class="text-xs text-stone-300 truncate">{{ t.displayName }}</span>
            <span class="text-[9px] px-1.5 py-0.5 rounded-full bg-amber-500/15 text-amber-400 flex-shrink-0">
              {{ roleBadge(t.role) }}
            </span>
            <span v-if="isRemote" class="text-[9px] px-1.5 py-0.5 rounded-full bg-violet-500/15 text-violet-400 flex-shrink-0 flex items-center gap-0.5">
              <Radio :size="9" /> 远程
            </span>
          </div>
          <div class="flex items-center gap-2 mt-1">
            <div class="h-1 flex-1 rounded-full bg-stone-700 overflow-hidden">
              <div
                class="h-full rounded-full transition-all duration-500"
                :class="t.socialEnergy > 60 ? 'bg-success' : t.socialEnergy > 30 ? 'bg-amber-500' : 'bg-danger'"
                :style="{ width: t.socialEnergy + '%' }"
              />
            </div>
            <span class="text-[10px] text-stone-500 flex-shrink-0">{{ t.socialEnergy }}%</span>
          </div>
        </div>
      </div>

      <button class="glass-button px-4 py-1.5 text-xs flex items-center justify-center gap-1.5 mt-1" @click="showInvite = true">
        <UserPlus :size="14" /> 邀请搭子
      </button>
    </template>

    <InviteTeammateModal v-if="showInvite" @close="showInvite = false" />
  </div>
</template>
