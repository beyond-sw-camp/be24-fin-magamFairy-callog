<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { useAuthStore } from '@/stores/useAuthStore'

const route = useRoute()
const store = usePlannerStore()
const authStore = useAuthStore()

const SIDEBAR_WIDTH_STORAGE_KEY = 'callog-sidebar2-width'
const SIDEBAR_DEFAULT_WIDTH = 260
const SIDEBAR_MIN_WIDTH = 220
const SIDEBAR_MAX_WIDTH = 420

const sidebarElement = ref(null)
const sidebarWidth = ref(SIDEBAR_DEFAULT_WIDTH)
const sidebarResizeLeft = ref(0)
const isResizing = ref(false)

const sidebarStyle = computed(() => ({
  '--sidebar2-width': `${sidebarWidth.value}px`,
}))

const navItems = [
  {
    id: 'dashboard',
    to: '/dashboard',
    label: 'Dashboard',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></svg>`,
  },
  {
    id: 'calendar',
    to: '/calendar',
    label: 'Campaign Timeline',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 2v4"/><path d="M16 2v4"/><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M3 10h18"/></svg>`,
  },
  {
    id: 'tasks',
    to: '/tasks',
    label: 'Content Cards',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/></svg>`,
    badgeKey: 'contentCards',
  },
  {
    id: 'reports',
    to: '/reports',
    label: 'Review Queue',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="m9 11 3 3L22 4"/></svg>`,
    badgeKey: 'reviewQueue',
  },
  {
    id: 'operations',
    to: '/operations',
    label: 'Partners',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
  },
  {
    id: 'frames',
    to: '/frames',
    label: '캠페인 프레임',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" x2="18" y1="20" y2="10"/><line x1="12" x2="12" y1="20" y2="4"/><line x1="6" x2="6" y1="20" y2="14"/></svg>`,
  },
  {
    id: 'references',
    to: '/references',
    label: 'References',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>`,
  },
]

const badges = computed(() => ({
  contentCards: store.tasks.length || null,
  reviewQueue: store.tasks.filter((t) => t.status === 'review').length || null,
}))

const profile = computed(() => store.findMember(store.currentUserId))

const userInitials = computed(() => {
  if (profile.value?.initials) return profile.value.initials
  const name = authStore.user?.name ?? authStore.user?.id ?? ''
  return name.charAt(0).toUpperCase() || 'U'
})

const userName = computed(() => profile.value?.name ?? authStore.user?.name ?? '사용자')
const userRole = computed(() => profile.value?.role ?? authStore.user?.role ?? 'HQ Admin')

function clampSidebarWidth(value) {
  return Math.min(SIDEBAR_MAX_WIDTH, Math.max(SIDEBAR_MIN_WIDTH, value))
}

function setSidebarWidth(value) {
  sidebarWidth.value = clampSidebarWidth(Math.round(value))
}

function persistSidebarWidth() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(SIDEBAR_WIDTH_STORAGE_KEY, String(sidebarWidth.value))
}

function handleSidebarResize(event) {
  setSidebarWidth(event.clientX - sidebarResizeLeft.value)
}

function stopSidebarResize() {
  if (!isResizing.value) {
    return
  }

  isResizing.value = false
  persistSidebarWidth()

  window.removeEventListener('pointermove', handleSidebarResize)
  window.removeEventListener('pointerup', stopSidebarResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

function startSidebarResize(event) {
  if (store.sidebarCollapsed) {
    return
  }

  event.preventDefault()
  sidebarResizeLeft.value = sidebarElement.value?.getBoundingClientRect().left ?? 0
  isResizing.value = true
  handleSidebarResize(event)

  window.addEventListener('pointermove', handleSidebarResize)
  window.addEventListener('pointerup', stopSidebarResize)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

function handleResizeKeydown(event) {
  if (store.sidebarCollapsed) {
    return
  }

  const step = event.shiftKey ? 24 : 8

  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    setSidebarWidth(sidebarWidth.value - step)
    persistSidebarWidth()
  }

  if (event.key === 'ArrowRight') {
    event.preventDefault()
    setSidebarWidth(sidebarWidth.value + step)
    persistSidebarWidth()
  }

  if (event.key === 'Home') {
    event.preventDefault()
    setSidebarWidth(SIDEBAR_MIN_WIDTH)
    persistSidebarWidth()
  }

  if (event.key === 'End') {
    event.preventDefault()
    setSidebarWidth(SIDEBAR_MAX_WIDTH)
    persistSidebarWidth()
  }
}

function isActive(item) {
  return route.path === item.to || route.path.startsWith(`${item.to}/`)
}

onMounted(() => {
  const storedWidth = window.localStorage.getItem(SIDEBAR_WIDTH_STORAGE_KEY)
  const parsedWidth = Number(storedWidth)

  if (Number.isFinite(parsedWidth)) {
    setSidebarWidth(parsedWidth)
  }
})

onBeforeUnmount(() => {
  stopSidebarResize()
})
</script>

<template>
  <aside
    ref="sidebarElement"
    class="sidebar2"
    :class="{
      'sidebar2--collapsed': store.sidebarCollapsed,
      'sidebar2--resizing': isResizing,
    }"
    :style="sidebarStyle"
  >
    <div class="sidebar2__inner">
      <!-- 로고 -->
      <div class="sidebar2__header">
        <RouterLink to="/dashboard" class="sidebar2__logo" aria-label="대시보드로 이동">
          Callog
        </RouterLink>
      </div>

      <!-- Active Campaign 블록 -->
      <div v-if="store.activeCampaign" class="sidebar2__campaign">
        <p class="sidebar2__campaign-label">Active Campaign</p>
        <p class="sidebar2__campaign-name">{{ store.activeCampaign.name }}</p>
        <p class="sidebar2__campaign-period">{{ store.activeCampaign.period }}</p>
      </div>

      <div class="sidebar2__divider" />

      <!-- 메뉴 -->
      <nav class="sidebar2__nav" aria-label="사이드바 메뉴">
        <RouterLink
          v-for="item in navItems"
          :key="item.id"
          :to="item.to"
          class="sidebar2__item"
          :class="{ active: isActive(item) }"
          :aria-current="isActive(item) ? 'page' : undefined"
        >
          <span class="sidebar2__item-icon" v-html="item.icon" />
          <span class="sidebar2__item-label">{{ item.label }}</span>
          <span
            v-if="item.badgeKey && badges[item.badgeKey]"
            class="sidebar2__item-badge"
          >{{ badges[item.badgeKey] }}</span>
        </RouterLink>
      </nav>

      <!-- 하단 프로필 -->
      <div class="sidebar2__profile">
        <RouterLink to="/mypage" class="sidebar2__profile-link">
          <span class="sidebar2__profile-avatar">{{ userInitials }}</span>
          <div class="sidebar2__profile-info">
            <p class="sidebar2__profile-name">{{ userName }}</p>
            <p class="sidebar2__profile-role">{{ userRole }}</p>
          </div>
        </RouterLink>
      </div>
    </div>
    <div
      class="sidebar2__resize-handle"
      role="separator"
      aria-label="사이드바 너비 조절"
      aria-orientation="vertical"
      tabindex="0"
      :aria-valuemin="SIDEBAR_MIN_WIDTH"
      :aria-valuemax="SIDEBAR_MAX_WIDTH"
      :aria-valuenow="sidebarWidth"
      :aria-valuetext="`${sidebarWidth}px`"
      @pointerdown="startSidebarResize"
      @keydown="handleResizeKeydown"
    />
  </aside>
</template>

<style scoped>
.sidebar2 {
  width: var(--sidebar2-width, var(--sidebar-width));
  flex-shrink: 0;
  background: #ffffff;
  border-right: 1px solid var(--color-gray-200);
  overflow: hidden;
  transition: width var(--transition-normal);
  height: 100vh;
  position: sticky;
  top: 0;
  z-index: 15;
}

.sidebar2--resizing {
  transition: none;
}

.sidebar2--collapsed {
  width: 0;
  border-right-width: 0;
}

.sidebar2__inner {
  width: var(--sidebar2-width, var(--sidebar-width));
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar2__resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  width: 10px;
  height: 100%;
  cursor: col-resize;
  touch-action: none;
  z-index: 3;
}

.sidebar2__resize-handle::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 2px;
  height: 100%;
  background: transparent;
  transition:
    background var(--transition-fast),
    box-shadow var(--transition-fast);
}

.sidebar2__resize-handle:hover::before,
.sidebar2__resize-handle:focus-visible::before,
.sidebar2--resizing .sidebar2__resize-handle::before {
  background: var(--color-primary-300);
  box-shadow: -4px 0 14px rgba(139, 92, 246, 0.18);
}

.sidebar2__resize-handle:focus-visible {
  outline: none;
}

.sidebar2--collapsed .sidebar2__resize-handle {
  opacity: 0;
  pointer-events: none;
}

/* 헤더 */
.sidebar2__header {
  padding: 16px 16px 8px;
  flex-shrink: 0;
}

.sidebar2__logo {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-gray-900);
  text-decoration: none;
  letter-spacing: -0.02em;
}

/* Active Campaign */
.sidebar2__campaign {
  margin: 8px 12px 0;
  background: var(--color-gray-50);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  padding: 12px 14px;
  flex-shrink: 0;
}

.sidebar2__campaign-label {
  font-size: 11px;
  color: var(--color-gray-400);
  margin-bottom: 4px;
  font-weight: 500;
  white-space: nowrap;
}

.sidebar2__campaign-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-gray-900);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar2__campaign-period {
  font-size: 12px;
  color: var(--color-gray-500);
  margin-top: 4px;
  white-space: nowrap;
}

/* 구분선 */
.sidebar2__divider {
  height: 1px;
  background: var(--color-gray-200);
  margin: 16px 12px 8px;
  flex-shrink: 0;
}

/* 메뉴 */
.sidebar2__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 8px;
  flex: 1;
}

.sidebar2__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-gray-600);
  text-decoration: none;
  transition: all var(--transition-fast);
  position: relative;
  white-space: nowrap;
}

.sidebar2__item:hover {
  background: var(--color-gray-50);
  color: var(--color-gray-900);
}

.sidebar2__item.active {
  background: var(--color-primary-500);
  color: white;
}

.sidebar2__item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  pointer-events: none;
}

.sidebar2__item-label {
  flex: 1;
  font-weight: 500;
}

.sidebar2__item-badge {
  margin-left: auto;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.sidebar2__item.active .sidebar2__item-badge {
  background: rgba(255, 255, 255, 0.25);
  color: white;
}

/* 하단 프로필 */
.sidebar2__profile {
  padding: 12px 8px;
  border-top: 1px solid var(--color-gray-200);
  flex-shrink: 0;
}

.sidebar2__profile-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: background var(--transition-fast);
}

.sidebar2__profile-link:hover {
  background: var(--color-gray-50);
}

.sidebar2__profile-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.sidebar2__profile-info {
  min-width: 0;
}

.sidebar2__profile-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-gray-900);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar2__profile-role {
  font-size: 12px;
  color: var(--color-gray-500);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
