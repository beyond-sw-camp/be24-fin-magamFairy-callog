<script setup>
import { computed, ref } from 'vue'
import { usePlannerStore } from '@/stores/planner'

import AssetBenefitManagement from '@/components/matchengine/AssetBenefitManagement.vue'
import CampaignMatching from '@/components/matchengine/CampaignMatching.vue'
import MatchDashboard from '@/components/matchengine/MatchDashboard.vue'
import OperationBoard from '@/components/matchengine/OperationBoard.vue'
import PartnerEvaluation from '@/components/matchengine/PartnerEvaluation.vue'

const store = usePlannerStore()

const isDark = computed(() => store.theme === 'dark')
const currentTab = ref('dashboard')
const operationHandoff = ref(null)

const tabs = [
  {
    id: 'dashboard',
    name: '현황',
    caption: '요약',
    count: 4,
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
  {
    id: 'operation',
    name: '운영 전환',
    caption: '보드',
    count: computed(() => (operationHandoff.value ? 1 : 0)),
    component: OperationBoard,
    icon: 'M9 11l3 3L22 4M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11',
  },
]

const currentComponent = computed(
  () => tabs.find((tab) => tab.id === currentTab.value)?.component ?? tabs[0].component,
)

function resolveTabCount(tab) {
  return typeof tab.count === 'object' ? tab.count.value : tab.count
}

function handleOperationHandoff(combo) {
  operationHandoff.value = {
    ...combo,
    source: `추천 조합 #M-${String(combo.id).padStart(3, '0')}`,
  }
  currentTab.value = 'operation'
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
        <span class="match-tabs__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path :d="tab.icon" />
          </svg>
        </span>
        <span class="match-tabs__copy">
          <strong>{{ tab.name }}</strong>
          <small>{{ tab.caption }}</small>
        </span>
        <span class="match-tabs__count">{{ resolveTabCount(tab) }}</span>
      </button>
    </nav>

    <main class="match-view__body">
      <transition name="match-fade" mode="out-in">
        <KeepAlive>
          <component
            :is="currentComponent"
            :isDark="isDark"
            :handoff="operationHandoff"
            @handoff="handleOperationHandoff"
            @request-matching="currentTab = 'matching'"
          />
        </KeepAlive>
      </transition>
    </main>
  </section>
</template>

<style scoped>
.match-view {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 0.75rem;
  height: calc(100vh - var(--header-height) - 48px);
  min-height: 34rem;
}

.match-tabs {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.5rem;
}

.match-tabs__button {
  position: relative;
  display: grid;
  grid-template-columns: 2rem minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.55rem;
  min-height: 3.25rem;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.55rem 0.65rem;
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
  box-shadow: 0 3px 10px rgba(19, 35, 68, 0.04);
  transition:
    transform var(--transition-fast),
    border-color var(--transition-fast),
    background-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.match-tabs__button::after {
  content: '';
  position: absolute;
  inset: auto 0.65rem 0.38rem;
  height: 2px;
  border-radius: 999px;
  background: transparent;
}

.match-tabs__button:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--accent-color) 28%, var(--border-strong));
  background: color-mix(in srgb, var(--accent-color) 4%, var(--panel-color));
  box-shadow: 0 8px 18px rgba(19, 35, 68, 0.07);
}

.match-tabs__button--active {
  border-color: color-mix(in srgb, var(--accent-color) 58%, var(--border-strong));
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--accent-color) 10%, var(--panel-color)),
      var(--panel-color)
    );
  color: var(--text-primary);
  box-shadow:
    0 10px 22px rgba(19, 35, 68, 0.08),
    inset 0 0 0 1px color-mix(in srgb, var(--accent-color) 12%, transparent);
}

.match-tabs__button--active::after {
  background: var(--accent-color);
}

.match-tabs__icon {
  display: grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  border-radius: 6px;
  background: var(--panel-muted);
  color: var(--muted-text);
}

.match-tabs__button--active .match-tabs__icon {
  background: var(--accent-soft);
  color: var(--accent-color);
}

.match-tabs__icon svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.9;
}

.match-tabs__copy {
  display: grid;
  min-width: 0;
  gap: 0.08rem;
}

.match-tabs__copy strong {
  overflow: hidden;
  color: inherit;
  font-size: 0.9rem;
  font-weight: 800;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-tabs__copy small {
  overflow: hidden;
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 700;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-tabs__count {
  display: inline-flex;
  min-width: 1.45rem;
  height: 1.45rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.match-tabs__button--active .match-tabs__count {
  background: var(--accent-color);
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
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .match-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
