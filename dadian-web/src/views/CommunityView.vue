<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCommunityStore } from '@/stores/community'
import MemoryFeed from '@/components/community/MemoryFeed.vue'
import ResonantPeople from '@/components/community/ResonantPeople.vue'
import AlmanacEntry from '@/components/community/AlmanacEntry.vue'
import MessageDrawer from '@/components/community/MessageDrawer.vue'
import { Sparkles, Trophy } from '@lucide/vue'

const store = useCommunityStore()
const drawerVisible = ref(false)
const drawerTargetUserId = ref<string | undefined>(undefined)

onMounted(async () => {
  await Promise.all([
    store.fetchFeed(true),
    store.fetchResonantUsers(),
  ])
})

function handleCollect(memoryId: string) {
  store.collectMemory(memoryId)
}

async function handleLoadMore() {
  await store.loadMore()
}

function handleRefreshUsers() {
  store.refreshResonantUsers()
}

function handleMessageUser(userId: string) {
  drawerTargetUserId.value = userId
  drawerVisible.value = true
}

function handleCloseDrawer() {
  drawerVisible.value = false
  drawerTargetUserId.value = undefined
}
</script>

<template>
  <div class="community-page min-h-screen">
    <div class="community-layout">
      <!-- ── Left: Memory Feed ──────────────────────────────── -->
      <main class="feed-area">
        <div class="section-header mb-4">
          <h1 class="text-lg font-semibold text-stone-100 tracking-wide">记忆长河</h1>
          <p class="text-xs text-stone-500 mt-1">来自社区同频人的城市记忆</p>
        </div>

        <MemoryFeed
          :memories="store.feed"
          :loading="store.feedLoading"
          @collect="handleCollect"
          @load-more="handleLoadMore"
        />
      </main>

      <!-- ── Right Sidebar: Resonant + Almanac ───────────────── -->
      <aside class="sidebar-area">
        <!-- Mobile/Tablet: horizontal scroll sections at top -->
        <div class="mobile-top-sections">
          <section class="horizontal-section">
            <div class="section-label">
              <Sparkles :size="14" class="text-amber-400" />
              <span class="text-xs font-medium text-stone-300">同频共振</span>
            </div>
            <ResonantPeople
              :users="store.resonantUsers"
              @refresh="handleRefreshUsers"
              @message="handleMessageUser"
            />
          </section>

          <section class="horizontal-section mt-4">
            <div class="section-label">
              <Trophy :size="14" class="text-amber-500" />
              <span class="text-xs font-medium text-stone-300">城市年鉴</span>
            </div>
            <AlmanacEntry />
          </section>
        </div>

        <!-- Desktop: sticky sidebar sections -->
        <div class="desktop-sidebar-sections">
          <ResonantPeople
            :users="store.resonantUsers"
            @refresh="handleRefreshUsers"
            @message="handleMessageUser"
          />

          <div class="mt-5">
            <AlmanacEntry />
          </div>
        </div>
      </aside>
    </div>

    <!-- Message drawer -->
    <MessageDrawer
      :visible="drawerVisible"
      :initial-user-id="drawerTargetUserId"
      @close="handleCloseDrawer"
    />
  </div>
</template>

<style scoped>
.community-page {
  padding: 24px;
  padding-left: 88px;
}

.community-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  max-width: 1440px;
  margin: 0 auto;
}

.section-header h1 {
  letter-spacing: 0.02em;
}

/* Desktop 3-column */
@media (min-width: 1280px) {
  .community-layout {
    grid-template-columns: 1fr 320px;
  }
}

/* Show desktop sidebar sections only on >=1280px */
.desktop-sidebar-sections {
  display: none;
}

@media (min-width: 1280px) {
  .desktop-sidebar-sections {
    display: block;
    position: sticky;
    top: 24px;
  }
}

/* Mobile/Tablet: horizontal + stacked sidebar at top */
.mobile-top-sections {
  display: block;
}

@media (min-width: 1280px) {
  .mobile-top-sections {
    display: none;
  }
}

.horizontal-section {
  /* On mobile (<640px), ResonantPeople scrolls horizontally, Almanac stacks */
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

/* Tablet sidebar layout: 2-col feed + 1 right rail */
@media (min-width: 640px) and (max-width: 1279px) {
  .community-layout {
    grid-template-columns: 1fr 280px;
  }

  .mobile-top-sections {
    display: none;
  }

  .desktop-sidebar-sections {
    display: block;
    position: sticky;
    top: 24px;
  }
}

/* Mobile padding */
@media (max-width: 639px) {
  .community-page {
    padding: 16px;
    padding-bottom: 80px; /* room for bottom tabs */
  }
}

/* Tablet padding */
@media (min-width: 640px) and (max-width: 1023px) {
  .community-page {
    padding-left: 88px;
    padding-bottom: 80px;
  }
}
</style>
