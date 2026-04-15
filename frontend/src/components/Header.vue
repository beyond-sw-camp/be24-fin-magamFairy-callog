<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'

const route = useRoute()
const store = usePlannerStore()

const profile = computed(() => store.findMember(store.currentUserId))
</script>

<template>
  <header class="header">
    <div class="header__left">
      <button
        class="header__menu"
        type="button"
        @click="store.toggleSidebar"
        aria-label="사이드바 토글"
      >
        <span />
        <span />
        <span />
      </button>

      <div class="header__page">
        <p class="header__eyebrow">{{ route.meta.section ?? '워크스페이스' }}</p>
        <h1>{{ route.meta.title ?? '보드' }}</h1>
      </div>
    </div>

    <div class="header__right">
      <label class="search-field">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M21 21l-4.35-4.35M10.75 18a7.25 7.25 0 1 1 0-14.5 7.25 7.25 0 0 1 0 14.5Z" />
        </svg>
        <input
          :value="store.searchQuery"
          type="search"
          placeholder="검색"
          @input="store.setSearchQuery($event.target.value)"
        />
      </label>

      <button class="header__icon header__icon--notification" type="button" aria-label="알림">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M12 4a5 5 0 0 0-5 5v2.39c0 .53-.21 1.04-.59 1.41L5 14.21V16h14v-1.79l-1.41-1.41A2 2 0 0 1 17 11.39V9a5 5 0 0 0-5-5Zm0 16a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 20Z"
          />
        </svg>
        <span class="header__dot" />
      </button>

      <button class="header__profile" type="button" aria-label="계정 프로필">
        {{ profile?.initials ?? 'U1' }}
      </button>

      <button class="header__icon" type="button" aria-label="설정" @click="store.toggleTheme">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M12 8.6A3.4 3.4 0 1 0 12 15.4 3.4 3.4 0 1 0 12 8.6Zm8.4 3.4-.98-.57a7.9 7.9 0 0 0-.37-.9l.56-.99a1 1 0 0 0-.14-1.22l-1.73-1.73a1 1 0 0 0-1.22-.14l-.99.56c-.29-.14-.59-.26-.9-.37L14 3.6a1 1 0 0 0-.98-.6h-2.04a1 1 0 0 0-.98.6l-.28 1.07c-.31.1-.61.23-.9.37l-.99-.56a1 1 0 0 0-1.22.14L4.88 6.35a1 1 0 0 0-.14 1.22l.56.99c-.14.29-.26.59-.37.9l-1.07.28a1 1 0 0 0-.6.98v2.04a1 1 0 0 0 .6.98l1.07.28c.1.31.23.61.37.9l-.56.99a1 1 0 0 0 .14 1.22l1.73 1.73a1 1 0 0 0 1.22.14l.99-.56c.29.14.59.26.9.37l.28 1.07a1 1 0 0 0 .98.6h2.04a1 1 0 0 0 .98-.6l.28-1.07c.31-.1.61-.23.9-.37l.99.56a1 1 0 0 0 1.22-.14l1.73-1.73a1 1 0 0 0 .14-1.22l-.56-.99c.14-.29.26-.59.37-.9l1.07-.28a1 1 0 0 0 .6-.98v-2.04a1 1 0 0 0-.6-.98Z"
          />
        </svg>
      </button>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1.1rem;
  border: 1px solid var(--border-color);
  border-radius: 22px;
  background: color-mix(in srgb, var(--panel-color) 96%, white);
  box-shadow: var(--shadow-soft);
}

.header__left,
.header__right {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.header__page {
  min-width: 0;
}

.header__eyebrow {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 0.15rem;
}

.header__page h1 {
  font-size: 1.2rem;
  line-height: 1.1;
}

.header__menu,
.header__icon,
.header__profile {
  flex-shrink: 0;
}

.header__menu,
.header__icon {
  width: 2.5rem;
  height: 2.5rem;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-muted);
  display: grid;
  place-items: center;
  cursor: pointer;
}

.header__menu {
  gap: 0.16rem;
}

.header__menu span {
  display: block;
  width: 0.95rem;
  height: 2px;
  border-radius: 999px;
  background: var(--text-primary);
}

.header__icon {
  position: relative;
}

.header__icon svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.header__icon--notification svg:last-child,
.header__icon svg path[fill] {
  fill: currentColor;
  stroke: none;
}

.header__dot {
  position: absolute;
  top: 0.55rem;
  right: 0.55rem;
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 999px;
  background: #ff5a5f;
  box-shadow: 0 0 0 2px var(--panel-color);
}

.header__profile {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent-color), var(--purple-color));
  color: #fff;
  font-weight: 800;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.search-field {
  width: 260px;
  height: 2.5rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-muted);
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0 0.9rem;
}

.search-field svg {
  width: 0.95rem;
  height: 0.95rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  color: var(--muted-text);
}

.search-field input {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--text-primary);
}

@media (max-width: 1120px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header__right {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}

@media (max-width: 760px) {
  .header__right {
    justify-content: flex-start;
  }

  .search-field {
    width: 100%;
  }
}
</style>
