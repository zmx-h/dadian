import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ShellMode = 'immersive' | 'standard' | 'locked'

export const useShellStore = defineStore('shell', () => {
  const mode = ref<ShellMode>('standard')

  function setMode(m: ShellMode) {
    mode.value = m
  }

  return { mode, setMode }
})
