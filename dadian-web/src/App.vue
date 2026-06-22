<script setup lang="ts">
import ShellNav from '@/components/shell/ShellNav.vue'
</script>

<template>
  <div class="app-shell">
    <ShellNav />
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
}

.app-main {
  flex: 1;
  margin-left: 64px;
  min-width: 0;
}

@media (max-width: 1023px) {
  .app-shell {
    flex-direction: column;
  }
  .app-main {
    margin-left: 0;
    margin-top: 56px;
  }
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease;
}
.page-enter-from,
.page-leave-to {
  opacity: 0;
}
</style>
