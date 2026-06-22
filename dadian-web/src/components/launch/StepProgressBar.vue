<script setup lang="ts">
defineProps<{
  currentStep: number
}>()

const steps = [
  { step: 1, label: '选区域' },
  { step: 2, label: '选地点' },
  { step: 3, label: '定路线' },
]
</script>

<template>
  <div class="flex items-center justify-center gap-0 py-6">
    <template v-for="(s, i) in steps" :key="s.step">
      <!-- circle -->
      <div class="flex flex-col items-center">
        <div
          class="w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold transition-all duration-500"
          :class="[
            currentStep > s.step
              ? 'bg-amber-500 text-stone-950 shadow-[0_0_12px_rgba(245,158,11,0.5)]'
              : currentStep === s.step
                ? 'border-2 border-amber-500 text-amber-400 amber-breathe'
                : 'border border-stone-700 text-stone-600',
          ]"
        >
          {{ s.step }}
        </div>
        <span
          class="mt-1.5 text-xs transition-colors duration-300"
          :class="
            currentStep >= s.step ? 'text-amber-400' : 'text-stone-600'
          "
        >
          {{ s.label }}
        </span>
      </div>

      <!-- connector line -->
      <div
        v-if="i < steps.length - 1"
        class="w-16 md:w-24 h-0.5 mx-1 rounded-full transition-colors duration-500"
        :class="
          currentStep > s.step
            ? 'bg-amber-500'
            : 'bg-stone-700'
        "
      />
    </template>
  </div>
</template>
