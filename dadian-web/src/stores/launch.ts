import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type LaunchStep = 'region' | 'spot' | 'route'
export type RoleType = 'agent' | 'foodie' | 'npc'

export interface Spot {
  id: string
  name: string
  category: string
  rating: number
  crowdLevel: 'low' | 'medium' | 'high'
  tags: string[]
  imageUrl: string
  region: string
}

export interface RouteWaypoint {
  id: string
  name: string
  type: 'start' | 'waypoint' | 'end'
  description: string
}

export interface GeneratedRoute {
  waypoints: RouteWaypoint[]
  totalDistance: string
  estimatedTime: string
}

export interface DiceResult {
  spotId: string
  spotName: string
}

const STEP_MAP: Record<number, LaunchStep> = {
  1: 'region',
  2: 'spot',
  3: 'route',
}

export const useLaunchStore = defineStore('launch', () => {
  const currentStep = ref<number>(1)
  const selectedRegion = ref<string | null>(null)
  const selectedSpot = ref<Spot | null>(null)
  const generatedRoute = ref<GeneratedRoute | null>(null)
  const socialEnergy = ref<number>(50)
  const selectedRole = ref<RoleType>('agent')
  const isDiceRolling = ref(false)
  const diceResult = ref<DiceResult | null>(null)

  const currentStepName = computed<LaunchStep>(() => STEP_MAP[currentStep.value])

  function setStep(step: number) {
    if (step >= 1 && step <= 3) {
      currentStep.value = step
    }
  }

  function selectRegion(region: string) {
    selectedRegion.value = region
    selectedSpot.value = null
    generatedRoute.value = null
    currentStep.value = 2
  }

  function selectSpot(spot: Spot) {
    selectedSpot.value = spot
    generatedRoute.value = null
    currentStep.value = 3
  }

  function rollDice(): Promise<DiceResult> {
    return new Promise((resolve) => {
      isDiceRolling.value = true
      diceResult.value = null

      setTimeout(() => {
        const result: DiceResult = {
          spotId: 'dice-random',
          spotName: '灵感推荐 · 永嘉路309号',
        }
        diceResult.value = result
        isDiceRolling.value = false
        resolve(result)
      }, 3000)
    })
  }

  function confirmLaunch() {
    // Stub — will emit to backend in future iteration
    console.log('[launch] confirmLaunch', {
      region: selectedRegion.value,
      spot: selectedSpot.value,
      route: generatedRoute.value,
      energy: socialEnergy.value,
      role: selectedRole.value,
    })
  }

  function reset() {
    currentStep.value = 1
    selectedRegion.value = null
    selectedSpot.value = null
    generatedRoute.value = null
    diceResult.value = null
    isDiceRolling.value = false
  }

  return {
    currentStep,
    currentStepName,
    selectedRegion,
    selectedSpot,
    generatedRoute,
    socialEnergy,
    selectedRole,
    isDiceRolling,
    diceResult,
    setStep,
    selectRegion,
    selectSpot,
    rollDice,
    confirmLaunch,
    reset,
  }
})
