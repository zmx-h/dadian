<script setup lang="ts">
import { Search, Utensils, Coffee } from '@lucide/vue'
import type { RoleType } from '@/stores/launch'

defineProps<{
  modelValue: RoleType
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: RoleType): void
}>()

const roles: { type: RoleType; icon: typeof Search; name: string; desc: string }[] = [
  {
    type: 'agent',
    icon: Search,
    name: '特工',
    desc: '探寻隐藏的角落，像特工一样发现城市秘密',
  },
  {
    type: 'foodie',
    icon: Utensils,
    name: '美食家',
    desc: '用味蕾丈量城市，打卡每一家心动小馆',
  },
  {
    type: 'npc',
    icon: Coffee,
    name: 'NPC',
    desc: '漫无目的地游荡，享受成为背景板的自由',
  },
]
</script>

<template>
  <div class="flex flex-col gap-2">
    <span class="text-xs text-stone-500 px-1">角色</span>
    <button
      v-for="r in roles"
      :key="r.type"
      class="glass-card p-3 flex items-center gap-3 transition-all duration-300 hover:border-amber-500/30"
      :class="
        modelValue === r.type
          ? 'border-amber-500 shadow-[0_0_12px_rgba(245,158,11,0.3)]'
          : 'border-white/5'
      "
      @click="emit('update:modelValue', r.type)"
    >
      <component
        :is="r.icon"
        :size="18"
        :class="modelValue === r.type ? 'text-amber-400' : 'text-stone-500'"
      />
      <div class="flex flex-col items-start text-left">
        <span
          class="text-sm font-medium"
          :class="modelValue === r.type ? 'text-amber-300' : 'text-stone-300'"
        >
          {{ r.name }}
        </span>
        <span class="text-[10px] text-stone-500 leading-tight mt-0.5">
          {{ r.desc }}
        </span>
      </div>
    </button>
  </div>
</template>
