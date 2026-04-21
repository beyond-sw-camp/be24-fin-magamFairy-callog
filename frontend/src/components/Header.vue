<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { useAuthStore } from '@/stores/useAuthStore'
import { getNoti } from '@/api/notifications/index.js'
import { formatRelativeTime } from '@/utils/datechange.js'

const notifications = ref({})

const getNotifications = async () => {
  try{
    const res = getNoti(3)
    notifications.value = res;
  }
  catch(e){
    console.error(e);
  }
  finally{
    const res = {
      success:"true",
      status:2000,
      message:"통신 성공",
      data:
      [
        {
        id: 34897,
        type: "qa",
        created_at: "2026-04-21T10:04:13Z",
        title: "알림 제목 1",
        message: "알림 내용 1",
        isRead: false
        },
        {
        id: 78354,
        type: "ai",
        created_at: "2026-04-12T12:04:13Z",
        title: "알림 제목2",
        message: "알림 내용 2",
        isRead: false
        },
        {
        id: 54876,
        type: "task",
        created_at: "2026-04-05T12:04:13Z",
        title: "알림 제목 3",
        message: "알림 내용 3",
        isRead: false
        }
      ]
    }
    notifications.value = res.data;
    console.log(notifications.value)
  }
}

const route = useRoute()
const router = useRouter()
const store = usePlannerStore()
const authStore = useAuthStore()
const reportsMenuOpen = ref(false)
const notificationsOpen = ref(false)
const appsMenuOpen = ref(false)
let reportsMenuTimer = null

const navItems = [
  { id: 'dashboard', to: '/dashboard', label: '대시보드' },
  { id: 'calendar', to: '/calendar', label: '캘린더' },
  { id: 'tasks', to: '/tasks', label: '업무 보드' },
  { id: 'operations', to: '/operations', label: '운영 허브' },
  { id: 'templates', to: '/templates', label: '템플릿' },
  { id: 'reports', to: '/reports', label: '리포트' },
]

const reportsDropdown = [
  { label: '템플릿 생성하기', to: '/templates' },
  { label: '레퍼런스 생성하기', to: '/reports' },
]

const appMenuItems = [
  {
    key: 'account',
    label: '내 계정',
    kind: 'route',
    to: { name: 'mypage' },
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>`,
  },
  {
    key: 'theme',
    label: '다크 테마',
    kind: 'action',
    action: 'theme',
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" /></svg>`,
  },
  {
    key: 'team-management',
    label: '팀관리',
    kind: 'action',
    action: 'teamManagement',
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20a5 5 0 00-10 0m10 0H7m10 0h3a2 2 0 002-2v-1a4 4 0 00-4-4h-1m-6 7H4a2 2 0 01-2-2v-1a4 4 0 014-4h1m0 0a4 4 0 100-8 4 4 0 000 8zm10-4a3 3 0 11-6 0 3 3 0 016 0z" /></svg>`,
  },
  {
    key: 'notifications',
    label: '최근 알림',
    kind: 'action',
    action: 'notifications',
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" /></svg>`,
  },
  {
    key: 'logout',
    label: '로그아웃',
    kind: 'route',
    to: { name: 'login' },
    danger: true,
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>`,
  },
  {
    key: 'settings',
    label: '환경설정',
    kind: 'route',
    to: { name: 'settings' },
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>`,
  },
]

const profile = computed(() => store.findMember(store.currentUserId))
const activeNav = computed(
  () =>
    navItems.find((item) => route.path === item.to || route.path.startsWith(`${item.to}/`)) ??
    navItems[0],
)

function isActive(item) {
  return route.path === item.to || route.path.startsWith(`${item.to}/`)
}

function clearReportsMenuTimer() {
  if (reportsMenuTimer !== null) {
    window.clearTimeout(reportsMenuTimer)
    reportsMenuTimer = null
  }
}

function openReportsMenu() {
  clearReportsMenuTimer()
  reportsMenuOpen.value = true
}

function scheduleCloseReportsMenu() {
  clearReportsMenuTimer()
  reportsMenuTimer = window.setTimeout(() => {
    reportsMenuOpen.value = false
  }, 140)
}

function closeFloatingMenus() {
  clearReportsMenuTimer()
  reportsMenuOpen.value = false
  notificationsOpen.value = false
  appsMenuOpen.value = false
}

function toggleNotifications() {
  notificationsOpen.value = !notificationsOpen.value
  appsMenuOpen.value = false
  reportsMenuOpen.value = false
  clearReportsMenuTimer()
}

function toggleAppsMenu() {
  appsMenuOpen.value = !appsMenuOpen.value
  notificationsOpen.value = false
  reportsMenuOpen.value = false
  clearReportsMenuTimer()
}

function handleSearchInput(event) {
  store.setSearchQuery(event.target.value)
}

function openNotificationsCenter() {
  closeFloatingMenus()
  notificationsOpen.value = true
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

    if (item.action === 'teamManagement') {
      return
    }

    if (item.action === 'notifications') {
      openNotificationsCenter()
      return
    }

    return
  }

  router.push(item.to)
}

function handleDocumentClick(event) {
  const path = typeof event.composedPath === 'function' ? event.composedPath() : []
  const clickedInsideHeader = path.some(
    (node) => node instanceof HTMLElement && node.dataset?.headerRoot === 'true',
  )

  if (!clickedInsideHeader) {
    closeFloatingMenus()
  }
}

onMounted(() => {
  window.addEventListener('click', handleDocumentClick)
  getNotifications();
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleDocumentClick)
  clearReportsMenuTimer()
})
</script>

<template>
  <header data-header-root="true" class="sticky top-4 z-40 overflow-visible px-4 sm:px-6">
    <div
      class="mx-auto flex min-h-[72px] w-full flex-col gap-4 overflow-visible rounded-[24px] border border-[var(--border-color)] bg-[var(--panel-color)] px-4 py-3 shadow-[0_4px_15px_rgba(0,0,0,0.03)] backdrop-blur-xl lg:flex-row lg:items-center lg:gap-2.5 lg:px-6"
    >
      <div class="flex w-[176px] shrink-0 items-center gap-3 lg:w-[190px]">
        <RouterLink
          to="/dashboard"
          class="flex w-full min-w-0 items-center gap-3 text-[var(--text-primary)] no-underline"
          aria-label="대시보드로 이동"
        >
          <span
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-[linear-gradient(135deg,var(--accent-color),var(--purple-color))] text-base font-extrabold text-white shadow-[0_8px_18px_rgba(79,70,229,0.2)]"
          >
            M
          </span>

          <div class="min-w-0 flex-1">
            <p class="mb-1 text-[10px] font-bold leading-none text-[var(--muted-text)]">
              운영 플래너
            </p>
            <h1 class="truncate text-sm font-bold leading-none text-[var(--text-primary)]">
              {{ activeNav.label }}
            </h1>
          </div>
        </RouterLink>
      </div>

      <nav
        class="flex min-w-0 flex-1 items-center gap-0.5 overflow-visible lg:justify-start lg:pl-1.5"
        aria-label="섹션 이동"
      >
        <template v-for="item in navItems" :key="item.id">
          <div
            v-if="item.id === 'templates'"
            class="relative mt-1 pb-1"
            @mouseenter="openReportsMenu"
            @mouseleave="scheduleCloseReportsMenu"
          >
            <RouterLink
              :to="item.to"
              class="relative flex flex-col items-center whitespace-nowrap rounded-2xl px-3.5 py-2 text-sm font-semibold transition-all duration-200 after:absolute after:-bottom-2 after:left-1/2 after:h-1 after:w-1 after:-translate-x-1/2 after:rounded-full after:bg-[var(--text-primary)] after:content-['']"
              :class="
                isActive(item)
                  ? 'bg-[var(--accent-soft)] text-[var(--text-primary)] shadow-[0_10px_20px_rgba(0,0,0,0.04)] after:opacity-100'
                  : 'text-[var(--muted-text)] after:opacity-0 hover:bg-[var(--panel-muted)] hover:text-[var(--text-primary)]'
              "
              @click="closeFloatingMenus"
            >
              {{ item.label }}
            </RouterLink>

            <Transition
              enter-active-class="transition duration-200 ease-out"
              enter-from-class="opacity-0 translate-y-2 scale-[0.98]"
              enter-to-class="opacity-100 translate-y-0 scale-100"
              leave-active-class="transition duration-150 ease-in"
              leave-from-class="opacity-100 translate-y-0 scale-100"
              leave-to-class="opacity-0 translate-y-2 scale-[0.98]"
            >
              <div
                v-if="reportsMenuOpen"
                class="absolute left-1/2 top-full z-50 mt-2 w-56 -translate-x-1/2 rounded-2xl border border-[var(--border-color)] bg-[var(--panel-color)] p-2 shadow-[0_10px_25px_rgba(0,0,0,0.1)]"
                @mouseenter="openReportsMenu"
                @mouseleave="scheduleCloseReportsMenu"
              >
                <RouterLink
                  v-for="action in reportsDropdown"
                  :key="action.label"
                  :to="action.to"
                  class="block rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold text-[var(--text-secondary)] transition hover:bg-[var(--panel-muted)] hover:text-[var(--accent-color)]"
                  @click="closeFloatingMenus"
                >
                  {{ action.label }}
                </RouterLink>
              </div>
            </Transition>
          </div>

          <RouterLink
            v-else
            :to="item.to"
            class="relative flex flex-col items-center whitespace-nowrap rounded-2xl px-3.5 py-2 text-sm font-semibold transition-all duration-200 after:absolute after:-bottom-2 after:left-1/2 after:h-1 after:w-1 after:-translate-x-1/2 after:rounded-full after:bg-[var(--text-primary)] after:content-['']"
            :class="
                isActive(item)
                  ? 'bg-[var(--accent-soft)] text-[var(--text-primary)] shadow-[0_10px_20px_rgba(0,0,0,0.04)] after:opacity-100'
                  : 'text-[var(--muted-text)] after:opacity-0 hover:bg-[var(--panel-muted)] hover:text-[var(--text-primary)]'
            "
            @click="closeFloatingMenus"
          >
            {{ item.label }}
          </RouterLink>
        </template>
      </nav>

      <div class="flex items-center justify-end gap-2">
        <label
          class="group relative w-40 shrink-0 transition-all duration-300 focus-within:w-56"
          aria-label="검색"
        >
          <span class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[var(--muted-text)]">
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </span>
          <input
            :value="store.searchQuery"
            type="search"
            placeholder="검색"
            class="h-10 w-full rounded-2xl border border-[var(--border-color)] bg-[var(--panel-muted)] pl-10 pr-4 text-sm text-[var(--text-primary)] outline-none transition-all duration-300 placeholder:text-[var(--muted-text)] focus:border-[var(--border-strong)] focus:bg-[var(--panel-color)]"
            @input="handleSearchInput"
          />
        </label>

        <div class="relative">
          <button
            type="button"
            class="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--panel-muted)] text-[var(--text-secondary)] transition-all duration-200 hover:-translate-y-0.5 hover:bg-[var(--accent-soft)] hover:text-[var(--text-primary)]"
            aria-label="알림"
            :aria-expanded="notificationsOpen"
            @click.stop="toggleNotifications"
          >
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
              />
            </svg>
            <span class="absolute right-[10px] top-[10px] h-1.5 w-1.5 rounded-full bg-[#fa5252] ring-2 ring-[var(--panel-color)]" />
          </button>

          <Transition
            enter-active-class="transition duration-200 ease-out"
            enter-from-class="opacity-0 translate-y-2 scale-[0.98]"
            enter-to-class="opacity-100 translate-y-0 scale-100"
            leave-active-class="transition duration-150 ease-in"
            leave-from-class="opacity-100 translate-y-0 scale-100"
            leave-to-class="opacity-0 translate-y-2 scale-[0.98]"
          >
            <div
              v-if="notificationsOpen"
              class="
                /* 공통 스타일 */
                fixed z-50 mt-3 flex flex-col border border-[var(--border-color)] bg-[var(--panel-color)] shadow-[0_18px_40px_rgba(0,0,0,0.14)]
                
                /* 모바일: 화면 하단 고정 또는 중앙 배치 */
                bottom-4 left-4 right-4 w-auto rounded-3xl 
                
                /* 데스크톱: 버튼 아래 우측 정렬 */
                md:absolute md:bottom-auto md:left-auto md:right-0 md:top-full md:w-96 md:rounded-2xl
              "
            >
              <div class="flex items-center justify-between p-4 pb-2">
                <div class="flex items-center gap-2 px-1">
                  <strong class="text-base font-bold tracking-tight text-[var(--text-primary)]">
                    최근 알림
                  </strong>
                </div>
                <button class="group flex items-center gap-1 rounded-full px-3 py-1.5 text-xs font-medium text-[var(--muted-text)] transition-all hover:bg-[var(--panel-hover)] hover:text-[var(--text-primary)]">
                  <span>알림 센터</span>
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 transition-transform group-hover:translate-x-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>

              <div class="max-h-[60vh] overflow-y-auto p-4 pt-2 md:max-h-[450px] custom-scrollbar">
                <div v-if="notifications.length" class="space-y-2">
                  <div 
                    v-for="(item, index) in notifications" 
                    :key="index"
                    class="rounded-2xl bg-[var(--panel-muted)] px-4 py-3 transition-transform active:scale-[0.98] md:active:scale-100"
                  >
                    <div class="flex items-center justify-between gap-3">
                      <div class="min-w-0 flex-1">
                        <p class="text-sm font-semibold text-[var(--text-primary)] truncate">
                          {{ item.title }}
                        </p>
                      </div>
                      <div class="inline-flex shrink-0 items-center gap-1 rounded-lg bg-[var(--panel-muted)] border border-[var(--border-subtle)] px-2.5 py-1 text-[11px] font-medium text-[var(--text-secondary)] transition-colors hover:bg-[var(--panel-hover)] cursor-pointer">
                        <span>자세히 보기</span>
                        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
                      </div>
                    </div>

                    <p class="mt-1 text-xs leading-relaxed text-[var(--muted-text)] line-clamp-2">
                      {{ formatRelativeTime(item.created_at) }} · {{ item.message }}
                    </p>
                  </div>
                </div>

                <div v-else class="py-10 text-center">
                  <p class="text-sm text-[var(--muted-text)]">새로운 알림이 없습니다.</p>
                </div>
              </div>
            </div>
          </Transition>
        </div>

        <RouterLink
          to="/mypage"
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-white/80 bg-[var(--panel-muted)] text-sm font-bold text-[var(--text-secondary)] shadow-[0_2px_4px_rgba(0,0,0,0.05)] no-underline transition hover:-translate-y-0.5 hover:bg-[var(--accent-soft)] hover:text-[var(--text-primary)]"
          aria-label="프로필"
          @click="closeFloatingMenus"
        >
          {{ profile?.initials ?? 'U1' }}
        </RouterLink>

        <div class="relative">
          <button
            type="button"
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--panel-muted)] text-[var(--text-secondary)] transition-all duration-200 hover:-translate-y-0.5 hover:bg-[var(--accent-soft)] hover:text-[var(--text-primary)]"
            aria-label="전체 메뉴"
            :aria-expanded="appsMenuOpen"
            @click.stop="toggleAppsMenu"
          >
            <div class="grid h-4 w-4 grid-cols-3 gap-[2px]">
              <span v-for="dot in 9" :key="dot" class="h-1 w-1 rounded-sm bg-current" />
            </div>
          </button>

          <Transition
            enter-active-class="transition duration-200 ease-out"
            enter-from-class="opacity-0 translate-y-2 scale-[0.98]"
            enter-to-class="opacity-100 translate-y-0 scale-100"
            leave-active-class="transition duration-150 ease-in"
            leave-from-class="opacity-100 translate-y-0 scale-100"
            leave-to-class="opacity-0 translate-y-2 scale-[0.98]"
          >
            <div
              v-if="appsMenuOpen"
              class="absolute right-0 top-full z-50 mt-3 w-[310px] rounded-[24px] border border-[var(--border-color)] bg-[var(--panel-color)] p-4 shadow-[0_20px_50px_rgba(0,0,0,0.12)]"
            >
              <div class="mb-4 flex items-center justify-between px-2">
                <strong class="text-sm font-bold text-[var(--muted-text)]">전체 메뉴</strong>
                <span class="h-1.5 w-1.5 rounded-full bg-[var(--accent-color)]"></span>
              </div>
              <div class="grid grid-cols-2 gap-x-2 gap-y-1.5">
                <button
                  v-for="item in appMenuItems"
                  :key="item.key"
                  type="button"
                  class="group flex w-full items-center gap-3 rounded-2xl px-4 py-3.5 text-left text-[15px] font-semibold transition-all duration-200"
                  :class="
                    item.danger
                      ? 'text-red-500 hover:bg-red-50 hover:text-red-600'
                      : 'text-slate-600 hover:bg-blue-50 hover:text-blue-600'
                  "
                  @click="handleAppMenuItem(item)"
                >
                  <span
                    class="flex h-5 w-5 shrink-0 items-center justify-center transition-colors"
                    :class="
                      item.danger
                        ? 'text-red-400 group-hover:text-red-500'
                        : 'text-slate-400 group-hover:text-blue-500'
                    "
                    v-html="item.icon"
                  ></span>
                  <span class="truncate">{{ item.label }}</span>
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </div>
  </header>
</template>
