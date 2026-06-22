<script setup lang="ts">
import SocialEnergySlider from './SocialEnergySlider.vue'
import RoleSelector from './RoleSelector.vue'
import DiceButton from './DiceButton.vue'
import { useLaunchStore } from '@/stores/launch'
import type { RoleType } from '@/stores/launch'
import { storeToRefs } from 'pinia'

const launch = useLaunchStore()
const { socialEnergy, selectedRole, isDiceRolling, diceResult } = storeToRefs(launch)

function onRoleChange(role: RoleType) {
  launch.selectedRole = role
}

async function onRoll() {
  await launch.rollDice()
}

// Mock teammates
const teammates = [
  { id: '1', name: '阿强', energy: 78, avatar: '🦊' },
  { id: '2', name: '小美', energy: 45, avatar: '🐱' },
]
</script>

<template>
  <aside class="flex flex-col gap-5 w-56 shrink-0 items-center">
    <!-- Social energy slider -->
    <SocialEnergySlider
      :model-value="socialEnergy"
      @update:model-value="launch.socialEnergy = $event"
    />

    <!-- Role selector -->
    <RoleSelector
      :model-value="selectedRole"
      @update:model-value="onRoleChange"
    />

    <!-- Dice -->
    <DiceButton
      :is-rolling="isDiceRolling"
      @roll="onRoll"
    />

    <!-- Dice result -->
    <div
      v-if="diceResult"
      class="text-xs text-amber-300 text-center glass-card p-2 w-full animate-pulse"
    >
      骰子指向：{{ diceResult.spotName }}
    </div>

    <!-- Teammate status -->
    <div class="glass-card p-4 w-full flex flex-col gap-3">
      <span class="text-xs text-stone-400 font-medium">队友状态</span>
      <div
        v-for="t in teammates"
        :key="t.id"
        class="flex items-center gap-2"
      >
        <span class="text-lg">{{ t.avatar }}</span>
        <div class="flex flex-col gap-0.5 flex-1 min-w-0">
          <span class="text-xs text-stone-300">{{ t.name }}</span>
          <div class="w-full h-1.5 bg-stone-800 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all duration-500"
              :class="{
                'bg-amber-400': t.energy > 60,
                'bg-violet-400': t.energy <= 60 && t.energy > 30,
                'bg-orange-400': t.energy <= 30,
              }"
              :style="{ width: t.energy + '%' }"
            />
          </div>
        </div>
        <span class="text-[10px] text-stone-500">{{ t.energy }}%</span>
      </div>
    </div>
  </aside>
</template>
