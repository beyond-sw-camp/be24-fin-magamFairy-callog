<script setup>
import { computed, ref } from 'vue'
import { usePlannerStore } from '@/stores/planner'
import { startEvaluation } from '@/api/evaluation'

import BenefitProposalInbox from '@/components/matchengine/BenefitProposalInbox.vue'
import MatchDashboard from '@/components/matchengine/MatchDashboard.vue'
import PartnerEvaluation from '@/components/matchengine/PartnerEvaluation.vue'

const props = defineProps({
  // 캠페인별 한정 보기 모드용. 없으면 전체 글로벌 뷰 (옵션 1).
  campaignId: { type: [String, Number], default: null },
})

const store = usePlannerStore()

const isDark = computed(() => store.theme === 'dark')
const currentTab = ref('dashboard')
const matchingCriteria = ref(null)
const evaluationCandidate = ref(null)

const tabs = computed(() => [
  {
    id: 'dashboard',
    name: '홈',
    caption: '요약',
    count: 8,
    component: MatchDashboard,
    icon: 'M4 13h6V4H4v9Zm10 7h6V4h-6v16ZM4 20h6v-3H4v3Z',
  },
  {
    id: 'benefits',
    name: '혜택 목록',
    caption: '검토',
    count: 4,
    component: BenefitProposalInbox,
    icon: 'M20 12v8H4v-8M22 7H2v5h20V7ZM12 22V7M12 7H7.5a2.5 2.5 0 1 1 0-5C11 2 12 7 12 7Zm0 0h4.5a2.5 2.5 0 1 0 0-5C13 2 12 7 12 7Z',
  },
  {
    id: 'evaluation',
    name: '파트너 평가',
    caption: '3건',
    count: 3,
    component: PartnerEvaluation,
    icon: 'M12 3l2.7 5.47 6.03.88-4.36 4.25 1.03 6-5.4-2.84L6.1 19.6l1.03-6L2.77 9.35l6.03-.88L12 3Z',
  },
])

const currentComponent = computed(
  () => tabs.value.find((tab) => tab.id === currentTab.value)?.component ?? tabs.value[0].component,
)

function resolveTabCount(tab) {
  return typeof tab.count === 'object' ? tab.count.value : tab.count
}

function moveToMatchingTab(criteria) {
  matchingCriteria.value = criteria ?? {
    goalType: 'PURCHASE_BOOKING',
    campaignMethods: [],
    benefitIds: [],
    sortType: 'HIGH_SCORE',
  }
  currentTab.value = 'evaluation'
}

function requestEvaluation(candidate) {
  evaluationCandidate.value = candidate ?? null
  console.log(candidate)
  console.log("ㅎㅇ")
  startEvaluation(evaluationCandidate.value);
  currentTab.value = 'evaluation'
}

function handleDashboardNavigation(target) {
  if (target?.tab === 'benefits' || target?.tab === 'evaluation') {
    currentTab.value = target.tab
  }
}

function handleDashboardAction(action) {
  handleDashboardNavigation(action?.target)
}

function handleMatchingComplete(target) {
  handleDashboardNavigation(target)
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
        <span v-if="resolveTabCount(tab) != null" class="match-tabs__count">
          {{ resolveTabCount(tab) }}
        </span>
      </button>
    </nav>

    <main class="match-view__body">
      <component
        :is="currentComponent"
        :isDark="isDark"
        :recommendationCriteria="matchingCriteria"
        :evaluationCandidate="evaluationCandidate"
        @request-matching="moveToMatchingTab"
        @request-evaluation="requestEvaluation"
        @navigate="handleDashboardNavigation"
        @action="handleDashboardAction"
        @matching-complete="handleMatchingComplete"
      />
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
  display: inline-flex;
  width: fit-content;
  max-width: 100%;
  overflow-x: auto;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 6px;
}

.match-tabs__button {
  display: inline-flex;
  align-items: center;
  gap: 0.38rem;
  min-height: 34px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0 14px;
  white-space: nowrap;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast),
    opacity var(--transition-fast);
}

.match-tabs__button:hover {
  color: var(--text-primary);
}

.match-tabs__button--active {
  border-color: color-mix(in srgb, var(--color-primary-500) 34%, var(--border-color));
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.match-tabs__button > strong {
  color: inherit;
  font-size: 13px;
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
  background: var(--color-primary-500);
  color: #fff;
}

.match-view__body {
  display: grid;
  gap: 0;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 1200px) {
  .match-tabs {
    overflow-x: auto;
  }
}

@media (max-width: 820px) {
  .match-tabs {
    width: 100%;
  }
}
</style>
