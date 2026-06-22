<script setup lang="ts">
defineProps<{
  modelValue: string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void
}>()

const regions = [
  {
    name: '武康路-安福路片区',
    desc: '法租界梧桐街区，文艺咖啡馆与买手店聚集地',
    tag: '文艺',
  },
  {
    name: '富民路-巨鹿路片区',
    desc: '小众酒吧与独立设计师店，夜间活力十足',
    tag: '潮流',
  },
  {
    name: '外滩-南京路片区',
    desc: '历史建筑群与江景，经典上海城市名片',
    tag: '经典',
  },
  {
    name: '新天地-马当路片区',
    desc: '石库门里的精致生活，brunch与画廊汇聚',
    tag: '精致',
  },
  {
    name: '愚园路-江苏路片区',
    desc: '老弄堂与新空间的共生，社区感浓厚',
    tag: '社区',
  },
]
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
    <button
      v-for="r in regions"
      :key="r.name"
      class="glass-card p-4 text-left transition-all duration-300 hover:border-amber-500/30"
      :class="
        modelValue === r.name
          ? 'border-amber-500 shadow-[0_0_16px_rgba(245,158,11,0.25)]'
          : 'border-white/5'
      "
      @click="emit('update:modelValue', r.name)"
    >
      <div class="flex items-center justify-between mb-2">
        <span
          class="text-sm font-semibold"
          :class="modelValue === r.name ? 'text-amber-300' : 'text-stone-200'"
        >
          {{ r.name }}
        </span>
        <span
          class="text-[10px] px-2 py-0.5 rounded-full border"
          :class="
            modelValue === r.name
              ? 'border-amber-500/40 text-amber-400 bg-amber-500/10'
              : 'border-stone-700 text-stone-500 bg-stone-800/50'
          "
        >
          {{ r.tag }}
        </span>
      </div>
      <p class="text-xs text-stone-500 leading-relaxed">{{ r.desc }}</p>
    </button>
  </div>
</template>
