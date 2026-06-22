import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/chu-fa',
    },
    {
      path: '/chu-fa',
      name: 'launch',
      component: () => import('@/views/LaunchView.vue'),
      meta: { shell: 'standard' },
    },
    {
      path: '/tan-suo',
      name: 'explore',
      component: () => import('@/views/ExploreView.vue'),
      meta: { shell: 'immersive' },
    },
    {
      path: '/tan-suo/:outingId',
      name: 'explore-active',
      component: () => import('@/views/ExploreView.vue'),
      meta: { shell: 'immersive' },
    },
    {
      path: '/hui-yi',
      name: 'memories',
      component: () => import('@/views/MemoriesView.vue'),
      meta: { shell: 'standard' },
    },
    {
      path: '/hui-yi/:memoryId',
      name: 'memory-detail',
      component: () => import('@/views/MemoryDetailView.vue'),
      meta: { shell: 'immersive' },
    },
    {
      path: '/she-qu',
      name: 'community',
      component: () => import('@/views/CommunityView.vue'),
      meta: { shell: 'standard' },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { shell: 'standard' },
    },
  ],
})

export default router
