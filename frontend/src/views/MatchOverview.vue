<script setup>
import { computed, ref } from 'vue'
import { usePlannerStore } from '@/stores/planner'

import AssetBenefitManagement from '@/components/matchengine/AssetBenefitManagement.vue'
import CampaignMatching from '@/components/matchengine/CampaignMatching.vue'
import MatchDashboard from '@/components/matchengine/MatchDashboard.vue'
import PartnerEvaluation from '@/components/matchengine/PartnerEvaluation.vue'

const store = usePlannerStore()

const isDark = computed(() => store.theme === 'dark')
const currentTab = ref('dashboard')

const tabs = [
  {
    id: 'dashboard',
    name: '현황',
    caption: '요약',
    count: 8,
    component: MatchDashboard,
    icon: 'M4 13h6V4H4v9Zm10 7h6V4h-6v16ZM4 20h6v-3H4v3Z',
  },
  {
    id: 'matching',
    name: '추천 조합',
    caption: '3건',
    count: 3,
    component: CampaignMatching,
    icon: 'M10 13a5 5 0 0 1 0-7l1.5-1.5a5 5 0 0 1 7 7L17 13M14 11a5 5 0 0 1 0 7l-1.5 1.5a5 5 0 0 1-7-7L7 11',
  },
  {
    id: 'assets',
    name: '자산/혜택',
    caption: '관리',
    count: 6,
    component: AssetBenefitManagement,
    icon: 'M4 7h16M4 12h16M4 17h10',
  },
  {
    id: 'evaluation',
    name: '파트너 평가',
    caption: '2건',
    count: 2,
    component: PartnerEvaluation,
    icon: 'M12 3l2.7 5.47 6.03.88-4.36 4.25 1.03 6-5.4-2.84L6.1 19.6l1.03-6L2.77 9.35l6.03-.88L12 3Z',
  },
]

const currentComponent = computed(
  () => tabs.find((tab) => tab.id === currentTab.value)?.component ?? tabs[0].component,
)

function resolveTabCount(tab) {
  return typeof tab.count === 'object' ? tab.count.value : tab.count
}

</script>

<template>
  <section class="match-view" :class="{ 'match-view--dark': isDark }">
    <nav class="match-tabs" aria-label="제휴 매칭 메뉴">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        class="match-tabs__button"
        :class="{ 'match-tabs__button--active': currentTab === tab.id }"
        :aria-current="currentTab === tab.id ? 'page' : undefined"
        @click="currentTab = tab.id"
      >
        <strong>{{ tab.name }}</strong>
        <span class="match-tabs__count">{{ resolveTabCount(tab) }}</span>
      </button>
    </nav>

    <main class="match-view__body">
      <transition name="match-fade" mode="out-in">
        <KeepAlive>
          <component :is="currentComponent" :isDark="isDark" />
        </KeepAlive>
      </transition>
    </main>
  </section>
</template>

<style scoped>
.match-view {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 0.65rem;
  height: calc(100vh - var(--header-height) - 48px);
  min-height: 34rem;
}

.match-tabs {
  display: flex;
  align-items: flex-end;
  gap: 1.9rem;
  min-height: 2.45rem;
  border-bottom: 1px solid var(--border-color);
  padding: 0 0.15rem;
}

.match-tabs__button {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.38rem;
  min-height: 2.45rem;
  border: 0;
  background: transparent;
  padding: 0 0 0.72rem;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  transition:
    color var(--transition-fast),
    opacity var(--transition-fast);
}

.match-tabs__button::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 2px;
  background: transparent;
}

.match-tabs__button:hover {
  color: var(--text-primary);
}

.match-tabs__button--active {
  color: var(--text-primary);
}

.match-tabs__button--active::after {
  background: var(--text-primary);
}

.match-tabs__button > strong {
  color: inherit;
  font-size: 0.78rem;
  font-weight: 900;
  line-height: 1;
}

.match-tabs__count {
  display: inline-flex;
  min-width: 1.02rem;
  height: 1.02rem;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: var(--panel-muted);
  color: var(--muted-text);
  font-size: 0.62rem;
  font-weight: 900;
  padding: 0 0.24rem;
}

.match-tabs__button--active .match-tabs__count {
  background: var(--text-primary);
  color: #fff;
}

.match-view__body {
  display: grid;
  gap: 0;
  min-width: 0;
  min-height: 0;
}

.match-fade-enter-active,
.match-fade-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.match-fade-enter-from,
.match-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

@media (max-width: 1200px) {
  .match-tabs {
    gap: 1.2rem;
    overflow-x: auto;
  }
}

@media (max-width: 820px) {
  .match-tabs {
    gap: 1rem;
  }
}
</style>
