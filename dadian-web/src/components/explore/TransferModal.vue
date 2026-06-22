<script setup lang="ts">
import { Search, Utensils, Coffee } from '@lucide/vue'
import { X } from '@lucide/vue'

defineEmits<{ close: [] }>()
</script>

<template>
  <div class="glass-overlay fixed inset-0 z-50 flex items-center justify-center" @click.self="$emit('close')">
    <div class="glass-card w-80 p-6">
      <div class="flex items-center justify-between mb-4">
        <span class="text-sm font-semibold text-stone-200">转职</span>
        <button class="text-stone-500 hover:text-stone-300" @click="$emit('close')"><X :size="18" /></button>
      </div>
      <div class="flex gap-3 mb-4">
        <button class="flex-1 glass-card p-3 text-center border-amber-500/40">
          <Search :size="18" class="mx-auto text-amber-400 mb-1" />
          <div class="text-xs text-amber-300">特工</div>
        </button>
        <button class="flex-1 glass-card p-3 text-center">
          <Utensils :size="18" class="mx-auto text-orange-400 mb-1" />
          <div class="text-xs text-stone-400">美食家</div>
        </button>
        <button class="flex-1 glass-card p-3 text-center">
          <Coffee :size="18" class="mx-auto text-violet-400 mb-1" />
          <div class="text-xs text-stone-400">NPC</div>
        </button>
      </div>
      <button disabled class="w-full py-2 glass-button text-xs text-stone-500 cursor-not-allowed">转职功能即将上线</button>
    </div>
  </div>
</template>
