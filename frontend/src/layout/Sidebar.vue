<script setup>
import { useRoute, RouterLink } from 'vue-router'

const route = useRoute()

const navItems = [
  {
    id: 'dashboard',
    to: '/dashboard',
    label: '메인',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></svg>`,
  },
  {
    id: 'calendar',
    to: '/calendar',
    label: '캘린더',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 2v4"/><path d="M16 2v4"/><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M3 10h18"/></svg>`,
  },
  {
    id: 'tasks',
    to: '/tasks',
    label: '콘텐츠 카드',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/></svg>`,
  },
  {
    id: 'reports',
    to: '/reports',
    label: '검수 큐',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="m9 11 3 3L22 4"/></svg>`,
  },
  {
    id: 'operations',
    to: '/operations',
    label: '파트너',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
  },
  {
    id: 'templates',
    to: '/templates',
    label: '성과',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" x2="18" y1="20" y2="10"/><line x1="12" x2="12" y1="20" y2="4"/><line x1="6" x2="6" y1="20" y2="14"/></svg>`,
  },
  {
    id: 'references',
    to: '/references',
    label: '레퍼런스',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>`,
  },
]

function isActive(item) {
  return route.path === item.to || route.path.startsWith(`${item.to}/`)
}
</script>

<template>
  <aside class="sidebar-icon-bar">
    <RouterLink to="/dashboard" class="sidebar-icon-bar__logo" aria-label="대시보드로 이동">
      <span class="sidebar-icon-bar__logo-mark">C</span>
    </RouterLink>

    <div class="sidebar-icon-bar__divider" />

    <nav class="sidebar-icon-bar__nav" aria-label="주요 메뉴">
      <RouterLink
        v-for="item in navItems"
        :key="item.id"
        :to="item.to"
        class="sidebar-icon-bar__item"
        :class="{ active: isActive(item) }"
        :title="item.label"
        :aria-label="item.label"
        :aria-current="isActive(item) ? 'page' : undefined"
      >
        <span class="sidebar-icon-bar__icon" v-html="item.icon" />
      </RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar-icon-bar {
  width: var(--sidebar-icon-width);
  flex-shrink: 0;
  background: var(--sidebar-color);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  height: 100vh;
  position: sticky;
  top: 0;
  z-index: 20;
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal);
}

.sidebar-icon-bar__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-500);
  text-decoration: none;
  margin-bottom: 16px;
  flex-shrink: 0;
  transition: background var(--transition-fast);
}

.sidebar-icon-bar__logo:hover {
  background: var(--color-primary-600);
}

.sidebar-icon-bar__logo-mark {
  color: white;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.sidebar-icon-bar__divider {
  width: 32px;
  height: 1px;
  background: var(--border-color);
  margin-bottom: 8px;
}

.sidebar-icon-bar__nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  width: 100%;
  padding: 0 8px;
}

.sidebar-icon-bar__item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  color: var(--nav-icon-color);
  text-decoration: none;
  transition: all var(--transition-fast);
}

.sidebar-icon-bar__item:hover {
  background: var(--nav-icon-hover-bg);
  color: var(--nav-icon-hover-color);
}

.sidebar-icon-bar__item.active {
  background: var(--nav-icon-active-bg);
  color: var(--nav-icon-active-color);
}

.sidebar-icon-bar__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  pointer-events: none;
}
</style>
