<script setup lang="ts">
import { Star } from '@lucide/vue'

const props = defineProps<{ memoryId: string; collected?: boolean }>()
const emit = defineEmits<{ collect: [id: string] }>()
</script>

<template>
  <button
    class="collect-chip inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] border transition-all duration-200"
    :class="props.collected
      ? 'bg-amber-500/10 text-amber-400 border-amber-500/30 amber-breathe'
      : 'bg-stone-800/50 text-stone-500 border-stone-700 hover:border-amber-500/30'"
    @click.stop="emit('collect', props.memoryId)"
  >
    <Star :size="10" :fill="props.collected ? 'currentColor' : 'none'" />
    收藏剧本
  </button>
</template>
