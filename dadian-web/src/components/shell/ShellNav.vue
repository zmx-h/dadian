<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { Compass, Camera, Users, User, Zap } from '@lucide/vue'

const router = useRouter()
const route = useRoute()

const navItems = [
  { path: '/chu-fa', icon: Zap, label: '出发' },
  { path: '/tan-suo', icon: Compass, label: '探索' },
  { path: '/hui-yi', icon: Camera, label: '回忆' },
  { path: '/she-qu', icon: Users, label: '社区' },
  { path: '/profile', icon: User, label: '我的' },
]

function isActive(path: string) {
  return route.path.startsWith(path)
}
</script>

<template>
  <!-- Desktop sidebar -->
  <nav class="shell-sidebar">
    <div class="sidebar-inner">
      <div class="nav-brand">⚡</div>
      <button
        v-for="item in navItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
        @click="router.push(item.path)"
      >
        <component :is="item.icon" class="nav-icon" :size="22" />
        <span class="nav-label">{{ item.label }}</span>
      </button>
    </div>
  </nav>

  <!-- Tablet bottom tabs -->
  <nav class="shell-tabs">
    <button
      v-for="item in navItems"
      :key="item.path"
      class="tab-item"
      :class="{ active: isActive(item.path) }"
      @click="router.push(item.path)"
    >
      <component :is="item.icon" :size="18" />
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>

<style scoped>
.shell-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 64px;
  background: rgba(12, 10, 9, 0.95);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 16px;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.nav-brand {
  font-size: 24px;
  margin-bottom: 12px;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: #78716c;
  cursor: pointer;
  transition: color 0.2s;
  width: 56px;
}

.nav-item:hover {
  color: #d6d3d1;
}

.nav-item.active {
  color: var(--amber-500);
}

.nav-icon {
  flex-shrink: 0;
}

.nav-label {
  font-size: 10px;
  line-height: 1;
}

.shell-tabs {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: rgba(12, 10, 9, 0.95);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  z-index: 100;
  justify-content: space-around;
  align-items: center;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  border: none;
  background: transparent;
  color: #78716c;
  cursor: pointer;
  font-size: 10px;
  padding: 4px 12px;
  border-radius: 8px;
  transition: color 0.2s;
}

.tab-item.active {
  color: var(--amber-500);
}

@media (max-width: 1023px) {
  .shell-sidebar {
    display: none;
  }
  .shell-tabs {
    display: flex;
  }
}
</style>
