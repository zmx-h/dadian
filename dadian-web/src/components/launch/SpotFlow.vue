<script setup lang="ts">
import { computed } from 'vue'
import type { Spot } from '@/stores/launch'
import SpotCard from './SpotCard.vue'

const props = defineProps<{
  region: string | null
}>()

const emit = defineEmits<{
  (e: 'select', spot: Spot): void
}>()

// Mock data — 6 Shanghai spots
const allSpots: Spot[] = [
  {
    id: '1',
    name: 'Manner Coffee 武康路店',
    category: '咖啡馆',
    rating: 4.7,
    crowdLevel: 'high',
    tags: ['咖啡', '露台', '宠物友好'],
    imageUrl: '',
    region: '武康路-安福路片区',
  },
  {
    id: '2',
    name: '多抓鱼循环商店',
    category: '书店',
    rating: 4.5,
    crowdLevel: 'medium',
    tags: ['二手书', '循环', '文艺'],
    imageUrl: '',
    region: '武康路-安福路片区',
  },
  {
    id: '3',
    name: 'Speak Low 彼楼',
    category: '酒吧',
    rating: 4.8,
    crowdLevel: 'high',
    tags: ['speakeasy', '鸡尾酒', '隐藏入口'],
    imageUrl: '',
    region: '富民路-巨鹿路片区',
  },
  {
    id: '4',
    name: '甬府尊鲜 外滩店',
    category: '餐厅',
    rating: 4.6,
    crowdLevel: 'medium',
    tags: ['宁波菜', '江景', '包间'],
    imageUrl: '',
    region: '外滩-南京路片区',
  },
  {
    id: '5',
    name: 'Green&Safe 新天地店',
    category: '餐厅',
    rating: 4.3,
    crowdLevel: 'medium',
    tags: ['有机', 'brunch', '宠物友好'],
    imageUrl: '',
    region: '新天地-马当路片区',
  },
  {
    id: '6',
    name: 'Seesaw Coffee 愚园路',
    category: '咖啡馆',
    rating: 4.4,
    crowdLevel: 'low',
    tags: ['精品咖啡', '安静', '办公友好'],
    imageUrl: '',
    region: '愚园路-江苏路片区',
  },
]

const filteredSpots = computed(() => {
  if (!props.region) return allSpots
  return allSpots.filter((s) => s.region === props.region)
})
</script>

<template>
  <div v-if="!region" class="text-xs text-stone-500 mb-3">
    请先选择一个区域，或滚动查看全城精选
  </div>
  <div v-else class="text-xs text-amber-400/70 mb-3">
    {{ region }} · {{ filteredSpots.length }} 个地点
  </div>

  <div class="flex flex-wrap gap-3 justify-center">
    <button
      v-for="spot in filteredSpots"
      :key="spot.id"
      class="cursor-pointer bg-transparent border-0 p-0"
      @click="emit('select', spot)"
    >
      <SpotCard :spot="spot" />
    </button>
  </div>
</template>
