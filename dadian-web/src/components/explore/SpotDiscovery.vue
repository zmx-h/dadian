<script setup lang="ts">
import type { Spot } from '@/stores/explore'
import SpotCard from '@/components/explore/SpotCard.vue'

defineProps<{ spots: Spot[] }>()
const emit = defineEmits<{ select: [id: string] }>()
</script>

<template>
  <div class="spot-discovery">
    <h3 class="text-sm text-stone-400 mb-3">附近推荐</h3>
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
      <SpotCard v-for="spot in spots" :key="spot.id" :spot="spot" @click="emit('select', spot.id)" />
    </div>
  </div>
</template>
