<script setup>
import { useRoute, RouterLink } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'

const route = useRoute()
const store = usePlannerStore()

const navItems = [
  {
    id: 'dashboard',
    to: '/dashboard',
    label: 'Dashboard',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></svg>`,
  },
  {
    id: 'calendar',
    to: '/calendar',
    label: 'Campaign Timeline',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 2v4"/><path d="M16 2v4"/><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M3 10h18"/></svg>`,
  },
  {
    id: 'tasks',
    to: '/tasks',
    label: 'Content Cards',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/></svg>`,
  },
  {
    id: 'reports',
    to: '/reports',
    label: 'Reports',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><path d="m19 9-5 5-4-4-3 3"/></svg>`,
  },
  {
    id: 'operations',
    to: '/operations',
    label: 'Partners',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
  },
  {
    id: 'matching',
    to: '/matching',
    label: '매칭',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 1 0-7l1.5-1.5a5 5 0 0 1 7 7L17 13"/><path d="M14 11a5 5 0 0 1 0 7l-1.5 1.5a5 5 0 0 1-7-7L7 11"/></svg>`,
  },
  {
    id: 'frames',
    to: '/frames',
    label: '캠페인 프레임',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" x2="18" y1="20" y2="10"/><line x1="12" x2="12" y1="20" y2="4"/><line x1="6" x2="6" y1="20" y2="14"/></svg>`,
  },
  {
    id: 'references',
    to: '/references',
    label: '레퍼런스',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>`,
  },
  {
    id: 'resources',
    to: '/resources',
    label: '자료실',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>`,
  },
  {
    id: 'review-approval',
    to: '/review-approval',
    label: '검수/승인',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>`,
  },
]

function isActive(item) {
  return route.path === item.to || route.path.startsWith(`${item.to}/`)
}
</script>

<template>
  <aside class="global-nav" aria-label="글로벌 네비게이션">
    <RouterLink to="/dashboard" class="global-nav__logo" aria-label="대시보드로 이동">
      <span class="global-nav__logo-mark">C</span>
    </RouterLink>

    <nav class="global-nav__nav">
      <div class="global-nav__item-wrap">
      
        <span class="global-nav__tooltip" role="tooltip">
          {{ store.sidebarCollapsed ? '캠페인 목록 열기' : '캠페인 목록 닫기' }}
        </span>
      </div>

      <div class="global-nav__divider" style="margin: 4px 0;" />

      <div
        v-for="item in navItems"
        :key="item.id"
        class="global-nav__item-wrap"
      >
        <RouterLink
          :to="item.to"
          class="global-nav__item"
          :class="{ active: isActive(item) }"
          :aria-label="item.label"
          :aria-current="isActive(item) ? 'page' : undefined"
        >
          <span class="global-nav__indicator" />
          <span class="global-nav__item-icon" v-html="item.icon" />
        </RouterLink>
        <span class="global-nav__tooltip" role="tooltip">{{ item.label }}</span>
      </div>
    </nav>
  </aside>
</template>

<style scoped>
.global-nav {
  width: var(--sidebar-icon-width, 64px);
  flex-shrink: 0;
  background: var(--sidebar-color);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 0;
  height: 100vh;
  position: sticky;
  top: 0;
  z-index: 20;
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal);
}

.global-nav__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-500);
  text-decoration: none;
  flex-shrink: 0;
  transition: background var(--transition-fast);
}

.global-nav__logo:hover {
  background: var(--color-primary-600);
}

.global-nav__logo-mark {
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.global-nav__divider {
  width: 100%;
  height: 1px;
  background: var(--border-color);
  margin: 12px 0;
  flex-shrink: 0;
}

.global-nav__nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
  width: 100%;
  overflow-y: auto;
  padding: 4px 0;
}

.global-nav__nav::-webkit-scrollbar {
  width: 0;
}

.global-nav__item-wrap {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: center;
}

.global-nav__item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-lg);
  color: var(--nav-icon-color, var(--muted-text));
  text-decoration: none;
  flex-shrink: 0;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.global-nav__item:hover {
  background: var(--nav-icon-hover-bg, var(--panel-muted));
  color: var(--nav-icon-hover-color, var(--text-primary));
}

.global-nav__item.active {
  color: var(--color-primary-500);
  background: color-mix(in srgb, var(--color-primary-500) 10%, transparent);
}

.global-nav__indicator {
  position: absolute;
  left: -12px;
  top: 50%;
  width: 4px;
  height: 0;
  border-radius: 0 999px 999px 0;
  background: transparent;
  transform: translateY(-50%);
  transition:
    height var(--transition-fast),
    background var(--transition-fast);
}

.global-nav__item:hover .global-nav__indicator {
  height: 16px;
  background: var(--muted-text);
}

.global-nav__item.active .global-nav__indicator {
  height: 28px;
  background: var(--color-primary-500);
}

.global-nav__item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  pointer-events: none;
}

.global-nav__tooltip {
  position: absolute;
  left: calc(100% + 10px);
  top: 50%;
  transform: translateY(-50%);
  background: var(--text-primary);
  color: var(--surface-page, #fff);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  padding: 5px 10px;
  border-radius: var(--radius-md);
  pointer-events: none;
  opacity: 0;
  transition:
    opacity var(--transition-fast),
    transform var(--transition-fast);
  transform: translateY(-50%) translateX(-4px);
  z-index: 100;
}

.global-nav__tooltip::before {
  content: '';
  position: absolute;
  right: 100%;
  top: 50%;
  transform: translateY(-50%);
  border: 5px solid transparent;
  border-right-color: var(--text-primary);
}

.global-nav__item-wrap:hover .global-nav__tooltip {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}
</style>
