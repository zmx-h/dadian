<script setup lang="ts">
import { ref } from 'vue'
import { X } from '@lucide/vue'

const emit = defineEmits<{ close: []; confirm: [{spotId:string;photoUrl?:string;comment?:string}] }>()
const comment = ref('')
const spotId = ref('')
</script>

<template>
  <div class="glass-overlay fixed inset-0 z-50 flex items-end justify-center" @click.self="emit('close')">
    <div class="glass-card w-full max-w-md mx-4 mb-4 p-5 rounded-t-2xl animate-slide-up">
      <div class="flex items-center justify-between mb-4">
        <span class="text-sm font-semibold text-stone-200">拍照打卡</span>
        <button class="text-stone-500 hover:text-stone-300" @click="emit('close')"><X :size="18" /></button>
      </div>
      <div class="h-32 bg-stone-800 rounded-lg flex items-center justify-center mb-3 cursor-pointer">
        <span class="text-xs text-stone-500">点击拍照或拖拽照片到此处</span>
      </div>
      <textarea v-model="comment" maxlength="200" placeholder="留言..." class="w-full bg-stone-900 border border-stone-700 rounded-lg p-2 text-xs text-stone-300 placeholder-stone-600 h-16 resize-none mb-3" />
      <button class="w-full py-2 glass-button text-sm text-amber-400" @click="emit('confirm',{spotId,comment:comment})">
        打卡签到
      </button>
    </div>
  </div>
</template>

<style scoped>
@keyframes slide-up { from { transform: translateY(100%); } to { transform: translateY(0); } }
.animate-slide-up { animation: slide-up 0.3s ease-out; }
</style>
