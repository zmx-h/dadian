import { computed, type ComputedRef } from 'vue'

export interface SocialEnergyAPI {
  energy: { value: number }
  color: ComputedRef<string>
  label: ComputedRef<string>
  rangeLabel: ComputedRef<string>
}

/**
 * Derives color and intensity label from a social-energy ref.
 * The caller passes a reactive ref (e.g. from Pinia store) so this
 * stays pure and testable.
 */
export function useSocialEnergy(energy: { value: number }): SocialEnergyAPI {
  const color = computed(() => {
    if (energy.value <= 30) return 'violet'
    if (energy.value <= 60) return 'amber'
    return 'orange'
  })

  const label = computed(() => {
    if (energy.value <= 30) return '静谧'
    if (energy.value <= 60) return '刚好'
    return '满电'
  })

  const rangeLabel = computed(() => {
    if (energy.value <= 25) return '低电量'
    if (energy.value <= 50) return '舒适区'
    if (energy.value <= 75) return '适度探索'
    return '高能出击'
  })

  return { energy, color, label, rangeLabel }
}
