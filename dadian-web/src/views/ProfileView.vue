<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import api from '@/api'
import {
  MapPin, Camera, Users, Trophy, Lock,
  Shield, MapPinned, Download, Trash2, Check, AlertTriangle,
  Upload, Zap, Smile, Eye,
} from '@lucide/vue'

const auth = useAuthStore()
const { user } = storeToRefs(auth)

// -------------------- Mock stats & achievements --------------------
const stats = reactive({
  trips: 12,
  checkins: 27,
  memories: 5,
  followers: 48,
  following: 36,
})

const achievements = ref([
  { key: 'first_trip',    name: '初次出发', icon: MapPin,  desc: '完成一次出行',         target: 1,  progress: 1,  unlocked: true  },
  { key: 'escape_master',  name: '逃跑大师', icon: Zap,     desc: '骰子逃跑 3 次',       target: 3,  progress: 2,  unlocked: false },
  { key: 'wkw_heir',       name: '王家卫传人',icon: Camera,  desc: '3 段王家卫回忆',      target: 3,  progress: 3,  unlocked: true  },
  { key: 'checkin_king',   name: '打卡王',   icon: Check,   desc: '打卡 10 次',           target: 10, progress: 7,  unlocked: false },
  { key: 'buddha_master',  name: '佛系大师', icon: Smile,   desc: '5 次 NPC 出行',        target: 5,  progress: 5,  unlocked: true  },
  { key: 'spy_king',       name: '特工之王', icon: Eye,     desc: '拍摄 50 张照片',        target: 50, progress: 12, unlocked: false },
])

// -------------------- Privacy settings --------------------
type Visibility = 'private' | 'friends_only' | 'public'
type Retention = 7 | 14 | 30

const privacyVisibility = ref<Visibility>('public')
const privacyRetention = ref<Retention>(14)
const privacySaving = ref(false)

async function savePrivacy() {
  privacySaving.value = true
  try {
    await api.patch('/users/me/privacy', {
      achievement_visibility: privacyVisibility.value,
      location_retention_days: privacyRetention.value,
    })
    // simple inline toast
    toastMsg.value = '隐私设置已保存'
    toastShow.value = true
    setTimeout(() => { toastShow.value = false }, 2500)
  } catch {
    toastMsg.value = '保存失败，请重试'
    toastShow.value = true
    setTimeout(() => { toastShow.value = false }, 2500)
  } finally {
    privacySaving.value = false
  }
}

// -------------------- Danger zone --------------------
const showDeleteModal = ref(false)
const exporting = ref(false)
const deleting = ref(false)

async function handleExport() {
  exporting.value = true
  try {
    await api.post('/users/me/export')
    toastMsg.value = '数据导出中，预计 5 分钟'
    toastShow.value = true
    setTimeout(() => { toastShow.value = false }, 4000)
  } catch {
    toastMsg.value = '导出失败'
    toastShow.value = true
    setTimeout(() => { toastShow.value = false }, 2500)
  } finally {
    exporting.value = false
  }
}

async function handleDelete() {
  deleting.value = true
  try {
    await api.delete('/users/me?confirm=true')
    auth.logout()
    window.location.href = '/'
  } catch {
    toastMsg.value = '注销失败'
    toastShow.value = true
    setTimeout(() => { toastShow.value = false }, 2500)
  } finally {
    deleting.value = false
    showDeleteModal.value = false
  }
}

// -------------------- Toast --------------------
const toastMsg = ref('')
const toastShow = ref(false)

// -------------------- Computed helpers --------------------
const displayName = computed(() => user.value?.nickname || '未命名')
const avatarLetter = computed(() => displayName.value.charAt(0))
const joinedDate = computed(() => '2025-11-03') // mock
</script>

<template>
  <div class="profile-page min-h-screen px-4 py-6 max-w-2xl mx-auto space-y-6">

    <!-- ===== TOAST ===== -->
    <Transition name="toast">
      <div v-if="toastShow"
        class="fixed top-6 left-1/2 -translate-x-1/2 z-[9999] px-5 py-3 rounded-xl text-sm font-medium
               bg-amber-500/90 text-stone-950 shadow-lg backdrop-blur-sm">
        {{ toastMsg }}
      </div>
    </Transition>

    <!-- ===== HEADER ===== -->
    <div class="flex flex-col items-center gap-4">
      <div class="w-24 h-24 rounded-full bg-gradient-to-br from-amber-400 to-orange-500
                  flex items-center justify-center text-3xl font-bold text-stone-950
                  shadow-lg shadow-amber-500/20 ring-2 ring-amber-500/30">
        {{ avatarLetter }}
      </div>
      <div class="text-center">
        <h1 class="text-2xl font-bold">{{ displayName }}</h1>
        <p class="text-sm text-stone-400 mt-1">「城市漫游者」· 山野徒步爱好者</p>
        <div class="mt-3 flex items-center justify-center gap-3">
          <span class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs
                       bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <Zap :size="12" /> Agent
          </span>
          <span class="text-xs text-stone-500">加入于 {{ joinedDate }}</span>
        </div>
      </div>
    </div>

    <!-- ===== STATS ROW ===== -->
    <div class="glass-card p-4 grid grid-cols-4 gap-2">
      <div class="stat-item text-center">
        <div class="text-xl font-bold text-amber-400">{{ stats.trips }}</div>
        <div class="text-[10px] text-stone-500 mt-0.5 flex items-center justify-center gap-1">
          <MapPin :size="10" /> 出行
        </div>
      </div>
      <div class="stat-item text-center">
        <div class="text-xl font-bold text-amber-400">{{ stats.checkins }}</div>
        <div class="text-[10px] text-stone-500 mt-0.5 flex items-center justify-center gap-1">
          <Check :size="10" /> 打卡
        </div>
      </div>
      <div class="stat-item text-center">
        <div class="text-xl font-bold text-amber-400">{{ stats.memories }}</div>
        <div class="text-[10px] text-stone-500 mt-0.5 flex items-center justify-center gap-1">
          <Camera :size="10" /> 回忆录
        </div>
      </div>
      <div class="stat-item text-center">
        <div class="text-xl font-bold text-amber-400">{{ stats.followers }}</div>
        <div class="text-[10px] text-stone-500 mt-0.5 flex items-center justify-center gap-1">
          <Users :size="10" /> 粉丝
        </div>
      </div>
    </div>

    <!-- ===== ACHIEVEMENTS ===== -->
    <section>
      <h2 class="text-sm font-semibold text-stone-400 uppercase tracking-wider mb-3 flex items-center gap-2">
        <Trophy :size="14" class="text-amber-400" /> 成就
      </h2>
      <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
        <div
          v-for="ach in achievements"
          :key="ach.key"
          class="achievement-card glass-card p-3 flex flex-col items-center gap-2 transition-all duration-300"
          :class="ach.unlocked ? 'ring-1 ring-amber-500/40 amber-breathe' : 'opacity-50'"
        >
          <component :is="ach.icon" :size="22" :class="ach.unlocked ? 'text-amber-400' : 'text-stone-600'" />
          <span class="text-xs font-medium" :class="ach.unlocked ? 'text-stone-200' : 'text-stone-500'">
            {{ ach.name }}
          </span>
          <span class="text-[10px] text-stone-500 text-center leading-tight">{{ ach.desc }}</span>
          <!-- Progress bar -->
          <div class="w-full h-1.5 rounded-full bg-stone-800 overflow-hidden">
            <div
              class="h-full rounded-full transition-all duration-500"
              :class="ach.unlocked ? 'bg-amber-500' : 'bg-amber-600/50'"
              :style="{ width: `${Math.min(100, (ach.progress / ach.target) * 100)}%` }"
            />
          </div>
          <span class="text-[10px] text-stone-500">{{ ach.progress }}/{{ ach.target }}</span>
        </div>
      </div>
    </section>

    <!-- ===== PRIVACY SETTINGS ===== -->
    <section>
      <h2 class="text-sm font-semibold text-stone-400 uppercase tracking-wider mb-3 flex items-center gap-2">
        <Shield :size="14" class="text-amber-400" /> 隐私设置
      </h2>
      <div class="glass-card p-4 space-y-4">
        <!-- Achievement visibility -->
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium flex items-center gap-1.5">
              <Lock :size="12" class="text-stone-400" /> 成就可见性
            </p>
            <p class="text-[11px] text-stone-500 mt-0.5">控制谁可以看到你的成就</p>
          </div>
          <select
            v-model="privacyVisibility"
            class="bg-stone-800 text-stone-200 text-xs rounded-lg px-3 py-1.5 border border-stone-700
                   focus:outline-none focus:border-amber-500/50 cursor-pointer"
          >
            <option value="private">仅自己</option>
            <option value="friends_only">仅好友</option>
            <option value="public">公开</option>
          </select>
        </div>
        <div class="border-t border-white/5" />
        <!-- Location retention -->
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium flex items-center gap-1.5">
              <MapPinned :size="12" class="text-stone-400" /> 位置保留
            </p>
            <p class="text-[11px] text-stone-500 mt-0.5">历史位置数据的保留时长</p>
          </div>
          <select
            v-model="privacyRetention"
            class="bg-stone-800 text-stone-200 text-xs rounded-lg px-3 py-1.5 border border-stone-700
                   focus:outline-none focus:border-amber-500/50 cursor-pointer"
          >
            <option :value="7">7 天</option>
            <option :value="14">14 天</option>
            <option :value="30">30 天</option>
          </select>
        </div>
        <div class="border-t border-white/5" />
        <button
          class="glass-button px-5 py-2 text-sm font-medium text-amber-400
                 hover:bg-amber-500/20 transition-colors disabled:opacity-50"
          :disabled="privacySaving"
          @click="savePrivacy"
        >
          {{ privacySaving ? '保存中...' : '保存设置' }}
        </button>
      </div>
    </section>

    <!-- ===== DANGER ZONE ===== -->
    <section>
      <h2 class="text-sm font-semibold text-red-400/80 uppercase tracking-wider mb-3 flex items-center gap-2">
        <AlertTriangle :size="14" /> 危险操作
      </h2>
      <div class="glass-card border-red-500/10 p-4 space-y-3">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium flex items-center gap-1.5">
              <Download :size="12" class="text-stone-400" /> 导出我的数据
            </p>
            <p class="text-[11px] text-stone-500 mt-0.5">下载所有出行数据与回忆录</p>
          </div>
          <button
            class="px-4 py-1.5 rounded-full text-xs font-medium border border-stone-700 text-stone-300
                   hover:border-stone-500 transition-colors disabled:opacity-50"
            :disabled="exporting"
            @click="handleExport"
          >
            <Upload :size="12" class="inline mr-1" />
            {{ exporting ? '请求中...' : '导出' }}
          </button>
        </div>
        <div class="border-t border-white/5" />
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-red-400 flex items-center gap-1.5">
              <Trash2 :size="12" /> 注销账号
            </p>
            <p class="text-[11px] text-stone-500 mt-0.5">删除账号及所有数据，不可恢复</p>
          </div>
          <button
            class="px-4 py-1.5 rounded-full text-xs font-medium border border-red-500/30 text-red-400
                   hover:bg-red-500/10 transition-colors"
            @click="showDeleteModal = true"
          >
            注销
          </button>
        </div>
      </div>
    </section>

    <!-- ===== DELETE CONFIRMATION MODAL ===== -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showDeleteModal" class="fixed inset-0 z-[9998] flex items-center justify-center p-4">
          <div class="glass-overlay absolute inset-0" @click="showDeleteModal = false" />
          <div class="relative glass-card p-6 max-w-sm w-full space-y-4 z-10">
            <div class="flex items-center gap-2 text-red-400">
              <AlertTriangle :size="18" />
              <h3 class="font-semibold">确认注销账号</h3>
            </div>
            <p class="text-sm text-stone-400 leading-relaxed">
              此操作将永久删除你的账号、出行记录、回忆录及所有关联数据。<strong>此操作不可恢复。</strong>
            </p>
            <div class="flex gap-3 justify-end">
              <button
                class="px-4 py-2 rounded-full text-xs font-medium border border-stone-700 text-stone-300
                       hover:border-stone-500 transition-colors"
                @click="showDeleteModal = false"
              >
                取消
              </button>
              <button
                class="px-4 py-2 rounded-full text-xs font-medium bg-red-600 text-white
                       hover:bg-red-500 transition-colors disabled:opacity-50"
                :disabled="deleting"
                @click="handleDelete"
              >
                {{ deleting ? '注销中...' : '确认注销' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<style scoped>
.stat-item {
  position: relative;
}
.stat-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 15%;
  height: 70%;
  width: 1px;
  background: rgba(255, 255, 255, 0.06);
}

.achievement-card {
  min-height: 120px;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -12px);
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.25s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
