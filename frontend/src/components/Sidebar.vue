<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()
const route = useRoute()

const items = [
  {
    name: '대시보드',
    to: '/dashboard',
    icon: 'M3 3h8v8H3V3Zm10 0h8v5h-8V3ZM13 10h8v11h-8V10ZM3 13h8v8H3v-8Z',
  },
  {
    name: '캘린더',
    to: '/calendar',
    icon: 'M7 2v3M17 2v3M3 8h18M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z',
  },
  {
    name: '작업 보드',
    to: '/tasks',
    icon: 'M9 11l3 3L22 4M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11',
  },
  {
    name: '템플릿',
    to: '/templates',
    icon: 'M4 4h16v4H4V4Zm0 6h16v10H4V10Zm4 3h4m-4 3h8',
  },
  {
    name: '리포트',
    to: '/reports',
    icon: 'M4 19h16M7 16V8m5 8V5m5 11v-6',
  },
]

const isCollapsed = computed(() => store.sidebarCollapsed)
const modeLabel = computed(() => (store.activeMode === 'personal' ? '개인' : '팀'))
const themeLabel = computed(() => (store.theme === 'light' ? '라이트 모드' : '다크 모드'))
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': isCollapsed }">
    <div class="sidebar__top">
      <RouterLink class="sidebar__brand" to="/dashboard">
        <span class="sidebar__logo">M</span>

        <div v-if="!isCollapsed" class="sidebar__brand-copy">
          <strong>운영 보드</strong>
          <small>콘텐츠 플래너</small>
        </div>
      </RouterLink>

      <button class="sidebar__toggle" type="button" @click="store.toggleSidebar">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path :d="isCollapsed ? 'M9 18l6-6-6-6' : 'M15 18l-6-6 6-6'" />
        </svg>
      </button>
    </div>

    <nav class="sidebar__nav">
      <RouterLink
        v-for="item in items"
        :key="item.to"
        :to="item.to"
        class="sidebar__link"
        :class="{ 'sidebar__link--active': route.path === item.to }"
        :title="item.name"
      >
        <span class="sidebar__link-icon">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path :d="item.icon" />
          </svg>
        </span>

        <span v-if="!isCollapsed" class="sidebar__link-copy">{{ item.name }}</span>
      </RouterLink>
    </nav>

    <div v-if="!isCollapsed" class="sidebar__footer">
      <div class="sidebar__helper">
        <small>보드 모드</small>
        <strong>{{ modeLabel }}</strong>
      </div>

      <div class="sidebar__helper">
        <small>테마</small>
        <strong>{{ themeLabel }}</strong>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: sticky;
  top: 1rem;
  align-self: flex-start;
  width: 280px;
  min-width: 280px;
  height: calc(100vh - 2rem);
  border: 1px solid var(--border-color);
  border-radius: 30px;
  background:
    radial-gradient(
      circle at top left,
      color-mix(in srgb, var(--accent-color) 16%, transparent),
      transparent 38%
    ),
    var(--sidebar-color);
  backdrop-filter: blur(14px);
  box-shadow: var(--shadow-soft);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition:
    width 0.22s ease,
    min-width 0.22s ease;
}

.sidebar--collapsed {
  width: 92px;
  min-width: 92px;
}

.sidebar__top,
.sidebar__brand,
.sidebar__link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sidebar__top {
  justify-content: space-between;
}

.sidebar__brand {
  text-decoration: none;
  min-width: 0;
}

.sidebar__brand-copy {
  min-width: 0;
}

.sidebar__brand-copy strong,
.sidebar__brand-copy small {
  display: block;
  white-space: nowrap;
}

.sidebar__brand-copy strong {
  font-size: 1rem;
}

.sidebar__brand-copy small {
  color: var(--muted-text);
  font-size: 0.78rem;
}

.sidebar__logo {
  width: 2.6rem;
  height: 2.6rem;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--accent-color), var(--purple-color));
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 800;
  box-shadow: 0 14px 30px color-mix(in srgb, var(--accent-color) 18%, transparent);
}

.sidebar__toggle {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  display: grid;
  place-items: center;
  cursor: pointer;
}

.sidebar__toggle svg,
.sidebar__link svg {
  width: 1.15rem;
  height: 1.15rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.sidebar__nav {
  display: grid;
  gap: 0.35rem;
}

.sidebar__link {
  text-decoration: none;
  color: var(--muted-text);
  padding: 0.8rem 0.9rem;
  border-radius: 18px;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.sidebar__link:hover,
.sidebar__link--active {
  background: color-mix(in srgb, var(--accent-color) 12%, var(--panel-muted));
  color: var(--text-primary);
  transform: translateX(2px);
}

.sidebar__link-icon {
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 12px;
  background: color-mix(in srgb, var(--panel-muted) 70%, var(--panel-color));
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.sidebar__link-copy {
  font-weight: 600;
}

.sidebar__footer {
  margin-top: auto;
  display: grid;
  gap: 0.7rem;
}

.sidebar__helper {
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background: var(--panel-muted);
  padding: 0.9rem 1rem;
}

.sidebar__helper small {
  display: block;
  color: var(--muted-text);
  margin-bottom: 0.25rem;
}

@media (max-width: 1100px) {
  .sidebar {
    position: fixed;
    left: 0.7rem;
    top: 0.7rem;
    z-index: 40;
    height: calc(100vh - 1.4rem);
  }

  .sidebar--collapsed {
    transform: translateX(calc(-100% + 92px));
  }
}
</style>
