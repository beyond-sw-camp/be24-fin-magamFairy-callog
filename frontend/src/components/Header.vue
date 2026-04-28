<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { useAuthStore } from '@/stores/useAuthStore'
import { getNoti } from '@/api/notifications/index.js'
import { formatRelativeTime } from '@/utils/datechange.js'

const notifications = ref([])

const getNotifications = async () => {
  try {
    const res = getNoti(3)
    notifications.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    notifications.value = [
      { id: 34897, type: 'qa',   created_at: '2026-04-21T10:04:13Z', title: '알림 제목 1', message: '알림 내용 1', isRead: false },
      { id: 78354, type: 'ai',   created_at: '2026-04-12T12:04:13Z', title: '알림 제목2',  message: '알림 내용 2', isRead: false },
      { id: 54876, type: 'task', created_at: '2026-04-05T12:04:13Z', title: '알림 제목 3', message: '알림 내용 3', isRead: false },
    ]
  }
}

const route = useRoute()
const router = useRouter()
const store = usePlannerStore()
const authStore = useAuthStore()

const notificationsOpen = ref(false)
const appsMenuOpen = ref(false)

const pageRoutes = [
  { id: 'dashboard',  to: '/dashboard',  label: '메인',  section: '본사 통합 대시보드' },
  { id: 'calendar',   to: '/calendar',   label: '캘린더',    section: '운영 플래너' },
  { id: 'tasks',      to: '/tasks',      label: '업무 보드', section: '실행 보드' },
  { id: 'operations', to: '/operations', label: '운영 허브', section: '고객 및 업무 오케스트레이션' },
  { id: 'templates',  to: '/templates',  label: '템플릿',    section: '콘텐츠 라이브러리' },
  { id: 'reports',    to: '/reports',    label: '리포트',    section: '성과 리뷰' },
  { id: 'references', to: '/references', label: '레퍼런스',  section: '콘텐츠 라이브러리' },
]

const activeRoute = computed(
  () => pageRoutes.find((r) => route.path === r.to || route.path.startsWith(`${r.to}/`)) ?? pageRoutes[0],
)
const pageTitle   = computed(() => route.meta?.title   ?? activeRoute.value.label)
const sectionTitle = computed(() => route.meta?.section ?? activeRoute.value.section)

const profile = computed(() => store.findMember(store.currentUserId))

const appMenuItems = computed(() => [
  {
    key: 'account',
    label: '내 계정',
    kind: 'route',
    to: { name: 'mypage' },
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>`,
  },
  {
    key: 'notifications',
    label: '최근 알림',
    kind: 'action',
    action: 'notifications',
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>`,
  },
  {
    key: 'theme',
    label: store.theme === 'dark' ? '라이트모드' : '다크모드',
    kind: 'action',
    action: 'theme',
    active: store.theme === 'dark',
    icon:
      store.theme === 'dark'
        ? `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><circle cx="12" cy="12" r="4" stroke-width="2"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>`
        : `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>`,
  },
  {
    key: 'settings',
    label: '환경설정',
    kind: 'route',
    to: { name: 'settings' },
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>`,
  },
  {
    key: 'logout',
    label: '로그아웃',
    kind: 'route',
    to: { name: 'login' },
    danger: true,
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg>`,
  },
])

function closeFloatingMenus() {
  notificationsOpen.value = false
  appsMenuOpen.value = false
}

function toggleNotifications() {
  notificationsOpen.value = !notificationsOpen.value
  appsMenuOpen.value = false
}

function toggleAppsMenu() {
  appsMenuOpen.value = !appsMenuOpen.value
  notificationsOpen.value = false
}

function handleSearchInput(event) {
  store.setSearchQuery(event.target.value)
}

async function handleAppMenuItem(item) {
  closeFloatingMenus()

  if (item.key === 'logout') {
    await authStore.logout()
    router.push({ name: 'login' })
    return
  }

  if (item.kind === 'action') {
    if (item.action === 'theme') {
      store.toggleTheme()
      return
    }
    if (item.action === 'notifications') {
      notificationsOpen.value = true
      return
    }
    return
  }

  router.push(item.to)
}

function handleDocumentClick(event) {
  const path = typeof event.composedPath === 'function' ? event.composedPath() : []
  const inside = path.some(
    (node) => node instanceof HTMLElement && node.dataset?.headerRoot === 'true',
  )
  if (!inside) {
    closeFloatingMenus()
  }
}

onMounted(() => {
  window.addEventListener('click', handleDocumentClick)
  getNotifications()
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleDocumentClick)
})
</script>

<template>
  <header data-header-root="true" class="callog-header">
    <div class="callog-header__inner">

      <!-- 왼쪽: 토글 버튼 + 브레드크럼 -->
      <div class="callog-header__left">
        <button
          type="button"
          class="callog-header__toggle"
          :aria-label="store.sidebarCollapsed ? '사이드바 열기' : '사이드바 닫기'"
          :aria-expanded="!store.sidebarCollapsed"
          @click="store.toggleSidebar"
        >
          <!-- 열림 상태: PanelLeftClose -->
          <svg v-if="!store.sidebarCollapsed" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect width="18" height="18" x="3" y="3" rx="2"/>
            <path d="M9 3v18"/>
            <path d="m16 15-3-3 3-3"/>
          </svg>
          <!-- 닫힘 상태: PanelLeftOpen -->
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect width="18" height="18" x="3" y="3" rx="2"/>
            <path d="M9 3v18"/>
            <path d="m14 9 3 3-3 3"/>
          </svg>
        </button>

        <div class="callog-header__breadcrumb">
          <p class="callog-header__section">{{ sectionTitle }}</p>
          <h1 class="callog-header__title">{{ pageTitle }}</h1>
        </div>
      </div>

      <!-- 오른쪽: 기능 버튼들 -->
      <div class="callog-header__right">

        <!-- 검색 -->
        <label class="callog-header__search" aria-label="검색">
          <svg class="callog-header__search-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
          </svg>
          <input
            :value="store.searchQuery"
            type="search"
            placeholder="Search"
            class="callog-header__search-input"
            @input="handleSearchInput"
          />
        </label>

        <!-- 알림 -->
        <div class="callog-header__dropdown-wrap">
          <button
            type="button"
            class="callog-header__icon-btn callog-header__icon-btn--notif"
            aria-label="알림"
            :aria-expanded="notificationsOpen"
            @click.stop="toggleNotifications"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
            </svg>
            <span class="callog-header__notif-dot" />
          </button>

          <Transition name="callog-dropdown">
            <div v-if="notificationsOpen" class="callog-header__dropdown callog-header__dropdown--notif">
              <div class="callog-dropdown__head">
                <strong class="callog-dropdown__title">최근 알림</strong>
                <RouterLink to="/notifications" class="callog-dropdown__more" @click="closeFloatingMenus">
                  알림 센터
                  <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
                </RouterLink>
              </div>
              <div class="callog-dropdown__body">
                <div v-if="notifications.length" class="callog-notif-list">
                  <div v-for="item in notifications" :key="item.id" class="callog-notif-item">
                    <div class="callog-notif-item__top">
                      <p class="callog-notif-item__title">{{ item.title }}</p>
                      <button type="button" class="callog-notif-item__btn">자세히 보기</button>
                    </div>
                    <p class="callog-notif-item__meta">{{ formatRelativeTime(item.created_at) }} · {{ item.message }}</p>
                  </div>
                </div>
                <div v-else class="callog-dropdown__empty">새로운 알림이 없습니다.</div>
              </div>
            </div>
          </Transition>
        </div>

        <!-- 프로필 -->
        <RouterLink
          to="/mypage"
          class="callog-header__avatar"
          aria-label="프로필"
          @click="closeFloatingMenus"
        >
          {{ profile?.initials ?? 'U' }}
        </RouterLink>

        <!-- 전체 메뉴 -->
        <div class="callog-header__dropdown-wrap">
          <button
            type="button"
            class="callog-header__icon-btn"
            aria-label="전체 메뉴"
            :aria-expanded="appsMenuOpen"
            @click.stop="toggleAppsMenu"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="5" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="12" cy="19" r="1"/>
            </svg>
          </button>

          <Transition name="callog-dropdown">
            <div v-if="appsMenuOpen" class="callog-header__dropdown callog-header__dropdown--menu">
              <div class="callog-dropdown__head">
                <strong class="callog-dropdown__title">전체 메뉴</strong>
              </div>
              <div class="callog-appmenu-list">
                <button
                  v-for="item in appMenuItems"
                  :key="item.key"
                  type="button"
                  class="callog-appmenu-item"
                  :class="{
                    'callog-appmenu-item--active': item.active,
                    'callog-appmenu-item--danger': item.danger,
                  }"
                  @click="handleAppMenuItem(item)"
                >
                  <span class="callog-appmenu-item__icon" v-html="item.icon" />
                  <span>{{ item.label }}</span>
                </button>
              </div>
            </div>
          </Transition>
        </div>

      </div>
    </div>
  </header>
</template>

<style scoped>
/* ── Header 컨테이너 ───────────────────────────────────── */
.callog-header {
  height: var(--header-height);
  background: var(--header-color);
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
  position: sticky;
  top: 0;
  z-index: 10;
  flex-shrink: 0;
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal),
    color var(--transition-normal);
}

.callog-header__inner {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  gap: 12px;
}

/* ── 왼쪽 ─────────────────────────────────────────────── */
.callog-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.callog-header__toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  color: var(--muted-text);
  background: transparent;
  border: none;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.callog-header__toggle:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.callog-header__breadcrumb {
  min-width: 0;
}

.callog-header__section {
  font-size: 11px;
  color: var(--subtle-text);
  font-weight: 500;
  line-height: 1;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.callog-header__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── 오른쪽 ───────────────────────────────────────────── */
.callog-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 아이콘 버튼 공통 */
.callog-header__icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  color: var(--muted-text);
  background: transparent;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.callog-header__icon-btn:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
  border-color: var(--border-strong);
}

/* 알림 버튼 빨간 점 */
.callog-header__icon-btn--notif {
  position: relative;
}

.callog-header__notif-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 6px;
  height: 6px;
  background: #EF4444;
  border-radius: 50%;
  border: 2px solid var(--header-color);
}

/* 검색 */
.callog-header__search {
  position: relative;
  display: flex;
  align-items: center;
}

.callog-header__search-icon {
  position: absolute;
  left: 10px;
  color: var(--subtle-text);
  pointer-events: none;
}

.callog-header__search-input {
  height: 36px;
  width: 180px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--control-color);
  padding: 0 12px 0 34px;
  font-size: 13px;
  color: var(--text-primary);
  outline: none;
  transition: all var(--transition-fast);
}

.callog-header__search-input::placeholder {
  color: var(--subtle-text);
}

.callog-header__search-input:focus {
  border-color: var(--color-primary-300);
  background: var(--control-focus-color);
  width: 220px;
}

/* 프로필 아바타 */
.callog-header__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.callog-header__avatar:hover {
  background: var(--nav-icon-active-bg);
}

/* ── 드롭다운 공통 ─────────────────────────────────────── */
.callog-header__dropdown-wrap {
  position: relative;
}

.callog-header__dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: var(--dropdown-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-elevated);
  z-index: 50;
  overflow: hidden;
  min-width: 280px;
}

.callog-header__dropdown--notif {
  width: 360px;
}

.callog-header__dropdown--menu {
  width: 220px;
}

.callog-dropdown__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--border-color);
}

.callog-dropdown__title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.callog-dropdown__more {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--muted-text);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.callog-dropdown__more:hover {
  color: var(--color-primary-600);
}

.callog-dropdown__body {
  max-height: 360px;
  overflow-y: auto;
  padding: 8px;
}

.callog-dropdown__empty {
  padding: 24px;
  text-align: center;
  font-size: 13px;
  color: var(--subtle-text);
}

/* 알림 목록 */
.callog-notif-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.callog-notif-item {
  background: var(--panel-muted);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}

.callog-notif-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.callog-notif-item__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.callog-notif-item__btn {
  font-size: 11px;
  color: var(--muted-text);
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 2px 8px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.callog-notif-item__btn:hover {
  border-color: var(--color-primary-300);
  color: var(--color-primary-600);
}

.callog-notif-item__meta {
  font-size: 12px;
  color: var(--subtle-text);
}

/* 전체 메뉴 목록 */
.callog-appmenu-list {
  display: flex;
  flex-direction: column;
  padding: 8px;
  gap: 2px;
}

.callog-appmenu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  width: 100%;
  transition: all var(--transition-fast);
}

.callog-appmenu-item:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.callog-appmenu-item--active {
  background: var(--badge-bg);
  color: var(--badge-text);
}

.callog-appmenu-item--active .callog-appmenu-item__icon {
  color: var(--badge-text);
}

.callog-appmenu-item--danger {
  color: #EF4444;
}

.callog-appmenu-item--danger:hover {
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.callog-appmenu-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  color: var(--subtle-text);
  flex-shrink: 0;
}

.callog-appmenu-item--danger .callog-appmenu-item__icon {
  color: #FCA5A5;
}

/* ── 드롭다운 트랜지션 ──────────────────────────────────── */
.callog-dropdown-enter-active,
.callog-dropdown-leave-active {
  transition: all var(--transition-fast);
  transform-origin: top right;
}

.callog-dropdown-enter-from,
.callog-dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}
</style>
