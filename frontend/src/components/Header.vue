<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'

const route = useRoute()
const store = usePlannerStore()
const navCompact = ref(false)

const navItems = [
  {
    to: '/dashboard',
    label: '대시보드',
    icon: 'M4 4h7v7H4V4Zm9 0h7v5h-7V4ZM4 13h7v7H4v-7Zm9 7v-8h7v8h-7Z',
  },
  {
    to: '/calendar',
    label: '캘린더',
    icon: 'M7 2v3M17 2v3M4 8h16M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z',
  },
  {
    to: '/tasks',
    label: '업무 보드',
    icon: 'M5 4h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1Zm3 4h8M8 12h8M8 16h5M8 8l1.25 1.25L11 7.5',
  },
  {
    to: '/operations',
    label: '운영허브',
    icon: 'M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H8l-4 3V6a2 2 0 0 1 2-2Zm3 4h8M8 11h8M8 15h5',
  },
  {
    to: '/templates',
    label: '템플릿',
    icon: 'M4 4h16v4H4V4Zm0 6h16v10H4V10Zm4 3h4m-4 3h8',
  },
  {
    to: '/reports',
    label: '리포트',
    icon: 'M4 19h16M7 16V8m5 8V5m5 11v-6',
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
</script>

<template>
  <header class="header-shell">
    <div class="header-shell__bar">
      <div class="header-shell__brand-group">
        <RouterLink class="header-shell__brand" to="/dashboard" aria-label="대시보드로 이동">
          <span class="header-shell__logo">M</span>
        </RouterLink>

        <button
          class="header-shell__rail-toggle"
          type="button"
          :aria-label="navCompact ? '메뉴 펼치기' : '메뉴 줄이기'"
          @click="navCompact = !navCompact"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10 6l6 6-6 6" />
          </svg>
        </button>

        <div class="header-shell__page">
          <p class="header-shell__eyebrow">운영 플래너</p>
          <h1>{{ activeNav.label }}</h1>
        </div>
      </div>

      <nav
        class="header-shell__nav"
        :class="{ 'header-shell__nav--compact': navCompact }"
        aria-label="섹션 이동"
      >
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="header-shell__nav-link"
          :class="{ 'header-shell__nav-link--active': isActive(item) }"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path :d="item.icon" />
          </svg>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="header-shell__actions">
        <label class="header-shell__search" aria-label="검색">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M21 21l-4.25-4.25M10.75 18a7.25 7.25 0 1 1 0-14.5 7.25 7.25 0 0 1 0 14.5Z" />
          </svg>
          <input
            :value="store.searchQuery"
            type="search"
            placeholder="검색"
            @input="store.setSearchQuery($event.target.value)"
          />
        </label>

        <button class="header-shell__action-button" type="button" aria-label="알림">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M12 4a5 5 0 0 0-5 5v2.2c0 .55-.22 1.08-.6 1.47L5 13.07V15h14v-1.93l-1.4-1.4A2.1 2.1 0 0 1 17 11.2V9a5 5 0 0 0-5-5Zm0 16a2.4 2.4 0 0 0 2.35-2h-4.7A2.4 2.4 0 0 0 12 20Z"
            />
          </svg>
          <span class="header-shell__dot" />
        </button>

        <button class="header-shell__profile" type="button" aria-label="프로필">
          {{ profile?.initials ?? 'U1' }}
        </button>

        <button
          class="header-shell__action-button"
          type="button"
          aria-label="설정"
          @click="store.toggleTheme"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M12 8.5A3.5 3.5 0 1 0 12 15.5 3.5 3.5 0 1 0 12 8.5Zm8 3-.88-.52a7.8 7.8 0 0 0-.35-.84l.5-.9a1 1 0 0 0-.14-1.2l-1.67-1.67a1 1 0 0 0-1.2-.14l-.9.5c-.27-.13-.55-.25-.84-.35L14.5 3.9a1 1 0 0 0-.98-.65h-2.04a1 1 0 0 0-.98.65l-.27 1.03c-.29.1-.57.22-.84.35l-.9-.5a1 1 0 0 0-1.2.14L4.62 6.59a1 1 0 0 0-.14 1.2l.5.9c-.13.27-.25.55-.35.84L3.9 10.5a1 1 0 0 0-.65.98v2.04a1 1 0 0 0 .65.98l1.03.27c.1.29.22.57.35.84l-.5.9a1 1 0 0 0 .14 1.2l1.67 1.67a1 1 0 0 0 1.2.14l.9-.5c.27.13.55.25.84.35l.27 1.03a1 1 0 0 0 .98.65h2.04a1 1 0 0 0 .98-.65l.27-1.03c.29-.1.57-.22.84-.35l.9.5a1 1 0 0 0 1.2-.14l1.67-1.67a1 1 0 0 0 .14-1.2l-.5-.9c.13-.27.25-.55.35-.84l1.03-.27a1 1 0 0 0 .65-.98v-2.04a1 1 0 0 0-.65-.98l-1.03-.27c-.1-.29-.22-.57-.35-.84l.5-.9a1 1 0 0 0-.14-1.2l-1.67-1.67a1 1 0 0 0-1.2-.14l-.9.5c-.27-.13-.55-.25-.84-.35L14.5 3.9a1 1 0 0 0-.98-.65h-2.04a1 1 0 0 0-.98.65l-.27 1.03c-.29.1-.57.22-.84.35l-.9-.5a1 1 0 0 0-1.2.14L4.62 6.59a1 1 0 0 0-.14 1.2l.5.9c-.13.27-.25.55-.35.84L3.9 10.5a1 1 0 0 0-.65.98v2.04a1 1 0 0 0 .65.98l1.03.27c.1.29.22.57.35.84l-.5.9a1 1 0 0 0 .14 1.2l1.67 1.67a1 1 0 0 0 1.2.14l.9-.5c.27.13.55.25.84.35l.27 1.03a1 1 0 0 0 .98.65h2.04a1 1 0 0 0 .98-.65l.27-1.03c.29-.1.57-.22.84-.35l.9.5a1 1 0 0 0 1.2-.14l1.67-1.67a1 1 0 0 0 .14-1.2l-.5-.9c.13-.27.25-.55.35-.84l1.03-.27a1 1 0 0 0 .65-.98v-2.04a1 1 0 0 0-.65-.98Z"
            />
          </svg>
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header-shell {
  position: sticky;
  top: 0.75rem;
  z-index: 40;
  padding: 0.05rem;
}

.header-shell__bar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--border-color);
  border-radius: 28px;
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-color) 98%, var(--panel-muted)),
      var(--panel-color)
    ),
    var(--panel-color);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(20px);
}

.header-shell__brand-group {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
}

.header-shell__brand {
  display: inline-grid;
  place-items: center;
  text-decoration: none;
  flex-shrink: 0;
}

.header-shell__logo {
  width: 2.8rem;
  height: 2.8rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, var(--accent-color), var(--purple-color));
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 1.05rem;
  font-weight: 800;
  letter-spacing: -0.04em;
  box-shadow: 0 14px 30px color-mix(in srgb, var(--accent-color) 18%, transparent);
}

.header-shell__rail-toggle {
  width: 1.9rem;
  height: 1.9rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--muted-text);
  display: grid;
  place-items: center;
  flex-shrink: 0;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.header-shell__rail-toggle:hover {
  transform: translateY(-1px);
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-muted));
  border-color: color-mix(in srgb, var(--accent-color) 24%, var(--border-color));
}

.header-shell__rail-toggle svg {
  width: 0.95rem;
  height: 0.95rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.header-shell__page {
  min-width: 0;
}

.header-shell__eyebrow {
  color: var(--muted-text);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.header-shell__page h1 {
  margin-top: 0.1rem;
  font-size: 1.18rem;
  line-height: 1.1;
  font-weight: 800;
}

.header-shell__nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.header-shell__nav::-webkit-scrollbar {
  display: none;
}

.header-shell__nav-link {
  min-height: 2.55rem;
  padding: 0 0.95rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  color: var(--muted-text);
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
  text-decoration: none;
  white-space: nowrap;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.header-shell__nav-link:hover,
.header-shell__nav-link--active {
  transform: translateY(-1px);
  color: var(--text-primary);
  border-color: color-mix(in srgb, var(--accent-color) 28%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 8%, var(--panel-muted));
  box-shadow: 0 10px 20px color-mix(in srgb, var(--accent-color) 10%, transparent);
}

.header-shell__nav-link svg {
  width: 0.95rem;
  height: 0.95rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex-shrink: 0;
}

.header-shell__actions {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  flex-shrink: 0;
}

.header-shell__search {
  width: min(18rem, 24vw);
  min-width: 12.5rem;
  height: 2.65rem;
  padding: 0 0.95rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.header-shell__search svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  color: var(--muted-text);
  flex-shrink: 0;
}

.header-shell__search input {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--text-primary);
}

.header-shell__search input::placeholder {
  color: var(--muted-text);
}

.header-shell__action-button,
.header-shell__profile {
  width: 2.65rem;
  height: 2.65rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.header-shell__action-button {
  position: relative;
  color: var(--text-primary);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.header-shell__action-button:hover {
  transform: translateY(-1px);
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-muted));
  border-color: color-mix(in srgb, var(--accent-color) 24%, var(--border-color));
}

.header-shell__action-button svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.header-shell__action-button svg path[fill] {
  fill: currentColor;
  stroke: none;
}

.header-shell__dot {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  width: 0.46rem;
  height: 0.46rem;
  border-radius: 999px;
  background: #ff5a5f;
  box-shadow: 0 0 0 2px var(--panel-color);
}

.header-shell__profile {
  color: #fff;
  font-weight: 800;
  background: linear-gradient(135deg, var(--accent-color), var(--purple-color));
}

.header-shell__nav--compact .header-shell__nav-link {
  width: 2.65rem;
  padding: 0;
  justify-content: center;
}

.header-shell__nav--compact .header-shell__nav-link span {
  display: none;
}

@media (max-width: 1200px) {
  .header-shell__bar {
    grid-template-columns: 1fr;
    justify-items: stretch;
  }

  .header-shell__nav {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .header-shell__actions {
    justify-self: end;
    flex-wrap: wrap;
  }

  .header-shell__search {
    width: min(20rem, 100%);
  }
}

@media (max-width: 760px) {
  .header-shell {
    top: 0.5rem;
  }

  .header-shell__bar {
    padding: 0.8rem;
    gap: 0.8rem;
  }

  .header-shell__brand-group {
    gap: 0.6rem;
  }

  .header-shell__search {
    min-width: 0;
    width: 100%;
  }

  .header-shell__actions {
    width: 100%;
  }

  .header-shell__profile,
  .header-shell__action-button {
    width: 2.5rem;
    height: 2.5rem;
  }
}
</style>
