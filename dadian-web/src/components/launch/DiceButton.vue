<script setup lang="ts">
import { Dices } from '@lucide/vue'

defineProps<{
  isRolling: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'roll'): void
}>()
</script>

<template>
  <button
    class="glass-button px-5 py-2.5 flex items-center gap-2 text-sm font-medium transition-all duration-300 disabled:opacity-40 disabled:cursor-not-allowed hover:border-amber-400/50 hover:bg-amber-500/15"
    :disabled="disabled || isRolling"
    @click="emit('roll')"
  >
    <Dices
      :size="18"
      :class="[
        'transition-transform',
        isRolling ? 'text-amber-400 animate-spin' : 'text-amber-500',
      ]"
    />
    <span :class="isRolling ? 'text-amber-400' : 'text-amber-300'">
      {{ isRolling ? '正在感应灵感...' : '灵感骰子' }}
    </span>
  </button>
</template>
