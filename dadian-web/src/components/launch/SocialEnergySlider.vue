<script setup lang="ts">
import { computed } from 'vue'
import { useSocialEnergy } from '@/composables/useSocialEnergy'

const props = defineProps<{
  modelValue: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: number): void
}>()

// Wrap in a reactive-like container so composable can read it
const energyRef = computed(() => ({ value: props.modelValue }))
const { color, label } = useSocialEnergy(energyRef.value)

function onInput(e: Event) {
  emit('update:modelValue', Number((e.target as HTMLInputElement).value))
}

const trackAccent = computed(() => {
  switch (color.value) {
    case 'violet': return '#a78bfa'
    case 'amber': return '#f59e0b'
    case 'orange': return '#f97316'
  }
})

const labelColor = computed(() => {
  switch (color.value) {
    case 'violet': return 'text-violet-400'
    case 'amber': return 'text-amber-400'
    case 'orange': return 'text-orange-400'
  }
})
</script>

<template>
  <div class="flex flex-col items-center gap-3 p-4 glass-card" style="width: 72px">
    <!-- label + value -->
    <span class="text-xs text-stone-500">社交电量</span>
    <span class="text-lg font-bold" :class="labelColor">
      {{ modelValue }}
    </span>

    <!-- vertical slider via rotation wrapper -->
    <div class="flex items-center justify-center h-48 w-full">
      <div class="rotate-[-90deg] w-48 flex items-center">
        <input
          type="range"
          min="10"
          max="100"
          :value="modelValue"
          class="energy-range w-full"
          :style="{ '--track-accent': trackAccent }"
          @input="onInput"
        />
      </div>
    </div>

    <!-- intensity word -->
    <span class="text-xs font-medium" :class="labelColor">
      {{ label }}
    </span>
  </div>
</template>

<style scoped>
.energy-range {
  -webkit-appearance: none;
  appearance: none;
  height: 6px;
  border-radius: 3px;
  background: #292524;
  cursor: pointer;
  outline: none;
}

.energy-range::-webkit-slider-runnable-track {
  height: 6px;
  border-radius: 3px;
  background: linear-gradient(
    to right,
    #8b5cf6 0%,
    #a78bfa 30%,
    #f59e0b 60%,
    #f97316 100%
  );
}

.energy-range::-moz-range-track {
  height: 6px;
  border-radius: 3px;
  background: linear-gradient(
    to right,
    #8b5cf6 0%,
    #a78bfa 30%,
    #f59e0b 60%,
    #f97316 100%
  );
}

.energy-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f59e0b;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.2);
  margin-top: -6px;
  animation: breathe 2s ease-in-out infinite;
}

.energy-range::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f59e0b;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.2);
}
</style>
