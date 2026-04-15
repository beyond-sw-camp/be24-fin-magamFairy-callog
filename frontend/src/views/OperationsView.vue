<script setup>
import { computed } from 'vue'
import PageTabs from '@/components/common/PageTabs.vue'
import OperationsCustomersBoard from '@/components/operations/OperationsCustomersBoard.vue'
import OperationsTasknotesBoard from '@/components/operations/OperationsTasknotesBoard.vue'
import OperationsWorkloadBoard from '@/components/operations/OperationsWorkloadBoard.vue'
import { useOperationsStore } from '@/stores/operations'

const store = useOperationsStore()

const domainIcons = {
  workload: 'M4 6h16M4 12h16M4 18h10M18 16l2 2 4-4',
  customers: 'M17 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2M9 7a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm10 3a3 3 0 1 1 0-6',
  tasknotes: 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01',
}

const domainTabs = computed(() =>
  store.operationDomainTabs.map((tab) => ({
    value: tab.value,
    label: tab.label,
    icon: domainIcons[tab.value],
  })),
)

const activeTabTitle = computed(() => {
  if (store.activeTab === 'customers') return '고객관리'
  if (store.activeTab === 'tasknotes') return '업무일지'
  return '업무관리'
})

const customerMetrics = computed(() => {
  const riskCount = store.customers.filter((customer) => customer.health === 'risk').length
  const stableCount = store.customers.filter((customer) => customer.health === 'stable').length

  return [
    { id: 'customer-total', label: '등록 고객', value: String(store.customers.length), accent: '#2f80ed' },
    { id: 'customer-ai', label: 'AI 제안 대기', value: String(store.pendingCustomerSuggestions.length), accent: '#7c4dff' },
    { id: 'customer-stable', label: '안정 고객', value: String(stableCount), accent: '#59c36d' },
    { id: 'customer-risk', label: '리스크 고객', value: String(riskCount), accent: '#df5f75' },
  ]
})

const tasknoteMetrics = computed(() => {
  const completedCount = store.personalTaskNotes.filter((task) => task.isMarkedComplete).length

  return [
    { id: 'tasknote-today', label: '내 업무', value: String(store.personalTaskNotes.length), accent: '#2f80ed' },
    { id: 'tasknote-complete', label: '완료 표시', value: String(completedCount), accent: '#59c36d' },
    { id: 'tasknote-requests', label: '상태 변경 요청', value: String(store.pendingStatusRequests.length), accent: '#f5b64e' },
    { id: 'tasknote-team', label: '팀 일정 보드', value: String(store.teamTaskGroups.length), accent: '#7c4dff' },
  ]
})

const activeMetrics = computed(() => {
  if (store.activeTab === 'customers') return customerMetrics.value
  if (store.activeTab === 'tasknotes') return tasknoteMetrics.value
  return store.workloadMetrics
})

const hoveredMetric = computed(() => {
  return activeMetrics.value.find((metric) => metric.id === store.hoverMetricKey) ?? null
})

const hoveredMetricPreviewItems = computed(() => {
  return hoveredMetric.value ? store.getMetricPreview(hoveredMetric.value.id) : []
})

const pendingPersonalTask = computed(() => {
  return store.personalTaskNotes.find((task) => !task.isMarkedComplete) ?? store.personalTaskNotes[0] ?? null
})

const headerPrimaryAction = computed(() => {
  if (store.activeTab === 'customers') {
    return { label: '신규 고객', disabled: false }
  }

  if (store.activeTab === 'tasknotes') {
    return store.activeRole === 'admin'
      ? { label: '요청 반영', disabled: !store.pendingStatusRequests[0] }
      : { label: '다음 단계 요청', disabled: !pendingPersonalTask.value }
  }

  const item = store.selectedWorkItemDetail

  if (!item) {
    return { label: '대기 업무 없음', disabled: true }
  }

  if (item.type === 'generator') {
    return {
      label: store.activeRole === 'admin' ? '기본 생성' : '관리자 전용',
      disabled: store.activeRole !== 'admin',
    }
  }

  if (item.type === 'qa') {
    return {
      label: store.activeRole === 'admin' ? '승인 후 생성' : '초안 저장',
      disabled: false,
    }
  }

  return {
    label: store.activeRole === 'admin' ? '카드 요청 생성' : '승인 진행 중',
    disabled: store.activeRole !== 'admin' || !item.approvalsCompleted,
  }
})

const customerFilterOptions = [
  { value: 'all', label: '전체' },
  { value: 'stable', label: '안정' },
  { value: 'watch', label: '관찰' },
  { value: 'risk', label: '위험' },
]

const workloadFilterOptions = [
  { value: 'all', label: '전체' },
  { value: 'approvals', label: '승인 대기' },
  { value: 'drafts', label: '초안' },
  { value: 'generator', label: '기본 생성' },
]

function handleHeaderPrimaryAction() {
  if (store.activeTab === 'customers') {
    store.createCustomer()
    return
  }

  if (store.activeTab === 'tasknotes') {
    if (store.activeRole === 'admin') {
      if (store.pendingStatusRequests[0]) {
        store.applyStatusRequest(store.pendingStatusRequests[0].id)
      }
      return
    }

    if (pendingPersonalTask.value) {
      store.requestNextStatus(pendingPersonalTask.value.id)
    }
    return
  }

  const item = store.selectedWorkItemDetail

  if (!item) return

  if (item.type === 'generator' && store.activeRole === 'admin') {
    store.createGeneratedContentBatch()
    return
  }

  if (item.type === 'qa') {
    if (store.activeRole === 'admin') {
      store.approveQaRequest(item.id)
      return
    }

    store.saveQaDraft(item.id)
    return
  }

  if (item.type === 'meeting' && store.activeRole === 'admin') {
    store.createMeetingTask(item.id)
  }
}

function clearMetricPreview() {
  store.setHoverMetricKey(null)
}

function handleMetricGroupFocusOut(event) {
  const nextTarget = event.relatedTarget
  const currentTarget = event.currentTarget

  if (currentTarget instanceof HTMLElement && nextTarget instanceof Node && currentTarget.contains(nextTarget)) {
    return
  }

  clearMetricPreview()
}

function handleMetricClick(metricId) {
  clearMetricPreview()
  store.activateMetric(metricId)
}
</script>

<template>
  <section class="operations-view">
    <header class="surface-card operations-header">
      <div class="operations-header__top">
        <div class="operations-header__headline">
          <p class="section-eyebrow">운영 허브</p>
          <h2>{{ activeTabTitle }}</h2>
        </div>

        <div class="operations-header__actions">
          <div class="operations-segmented" role="tablist" aria-label="역할 전환">
            <button
              v-for="role in store.operationRoleOptions"
              :key="role.value"
              type="button"
              class="operations-segmented__button"
              :class="{ 'operations-segmented__button--active': store.activeRole === role.value }"
              @click="store.setActiveRole(role.value)"
            >
              {{ role.label }}
            </button>
          </div>

          <button
            class="primary-button operations-header__primary"
            type="button"
            :disabled="headerPrimaryAction.disabled"
            @click="handleHeaderPrimaryAction"
          >
            {{ headerPrimaryAction.label }}
          </button>
        </div>
      </div>

      <div class="operations-header__bottom">
        <PageTabs :active="store.activeTab" :tabs="domainTabs" @select="store.setActiveTab($event)" />

        <div v-if="store.activeTab === 'customers'" class="operations-context-filters">
          <button
            v-for="option in customerFilterOptions"
            :key="option.value"
            type="button"
            class="operations-filter-chip"
            :class="{ 'operations-filter-chip--active': store.customerHealthFilter === option.value }"
            @click="store.setCustomerHealthFilter(option.value)"
          >
            {{ option.label }}
          </button>
        </div>

        <div v-else-if="store.activeTab === 'workload'" class="operations-context-filters">
          <button
            v-for="option in workloadFilterOptions"
            :key="option.value"
            type="button"
            class="operations-filter-chip"
            :class="{ 'operations-filter-chip--active': store.workInboxFilter === option.value }"
            @click="store.setWorkInboxFilter(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </header>

    <div
      class="operations-metrics-block"
      @mouseleave="clearMetricPreview"
      @focusout="handleMetricGroupFocusOut"
    >
      <div class="operations-metrics">
        <button
          v-for="metric in activeMetrics"
          :key="metric.id"
          type="button"
          class="surface-card operations-metric"
          :class="{ 'operations-metric--active': store.activeMetricKey === metric.id }"
          :style="{ '--metric-accent': metric.accent }"
          :aria-pressed="store.activeMetricKey === metric.id"
          @mouseenter="store.setHoverMetricKey(metric.id)"
          @focus="store.setHoverMetricKey(metric.id)"
          @click="handleMetricClick(metric.id)"
        >
          <small>{{ metric.label }}</small>
          <strong>{{ metric.value }}</strong>
        </button>
      </div>

      <div
        v-if="hoveredMetric && hoveredMetricPreviewItems.length"
        class="surface-card operations-metric-preview-panel"
      >
        <div class="operations-metric-preview-panel__header">
          <div class="operations-metric-preview-panel__summary">
            <small>{{ hoveredMetric.label }}</small>
            <strong>{{ hoveredMetric.value }}</strong>
          </div>

          <button
            type="button"
            class="operations-metric-preview-panel__action"
            @click="handleMetricClick(hoveredMetric.id)"
          >
            보기 적용
          </button>
        </div>

        <div class="operations-metric-preview-panel__list">
          <button
            v-for="item in hoveredMetricPreviewItems"
            :key="`${hoveredMetric.id}-${item.title}`"
            type="button"
            class="operations-metric-preview-panel__item"
            @click="handleMetricClick(hoveredMetric.id)"
          >
            <span>{{ item.title }}</span>
            <small>{{ item.meta }}</small>
          </button>
        </div>
      </div>
    </div>

    <OperationsCustomersBoard v-if="store.activeTab === 'customers'" />
    <OperationsWorkloadBoard v-else-if="store.activeTab === 'workload'" />
    <OperationsTasknotesBoard v-else />

    <div class="surface-card operations-mobile-bar">
      <strong>{{ headerPrimaryAction.label }}</strong>
      <button
        class="primary-button"
        type="button"
        :disabled="headerPrimaryAction.disabled"
        @click="handleHeaderPrimaryAction"
      >
        실행
      </button>
    </div>
  </section>
</template>

<style scoped>
.operations-view {
  display: grid;
  gap: 0.8rem;
}

.operations-header {
  position: sticky;
  top: 0;
  z-index: 12;
  padding: 0.85rem 1rem;
  display: grid;
  gap: 0.7rem;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--panel-color) 97%, white), var(--panel-color)),
    var(--panel-color);
}

.operations-header__top,
.operations-header__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
}

.operations-header__headline {
  display: grid;
  gap: 0.15rem;
}

.operations-header__headline h2 {
  font-size: 1.35rem;
  line-height: 1.1;
}

.operations-header__actions {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.operations-segmented {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-muted);
}

.operations-segmented__button {
  min-height: 2.15rem;
  padding: 0 0.95rem;
  border-radius: 999px;
  color: var(--muted-text);
  font-weight: 700;
}

.operations-segmented__button--active {
  background: var(--panel-color);
  color: var(--text-primary);
  box-shadow: var(--shadow-soft);
}

.operations-context-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.operations-filter-chip {
  min-height: 1.95rem;
  padding: 0 0.75rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  font-weight: 700;
  background: transparent;
  color: var(--muted-text);
}

.operations-filter-chip--active {
  background: var(--panel-muted);
  color: var(--text-primary);
  border-color: color-mix(in srgb, var(--accent-color) 24%, var(--border-color));
}

.operations-metrics-block {
  display: grid;
  gap: 0.55rem;
}

.operations-metrics {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
  overflow: visible;
}

.operations-metric {
  position: relative;
  z-index: 0;
  padding: 0.9rem 1rem;
  border-top: 4px solid var(--metric-accent);
  display: grid;
  gap: 0.15rem;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.operations-metric:hover,
.operations-metric:focus-visible {
  transform: translateY(-1px);
  box-shadow: var(--shadow-soft);
}

.operations-metric:hover,
.operations-metric:focus-within {
  z-index: 22;
}

.operations-metric--active {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--metric-accent) 76%, white);
  background: color-mix(in srgb, var(--metric-accent) 8%, var(--panel-color));
  box-shadow:
    0 18px 32px rgba(15, 23, 42, 0.08),
    0 0 0 1px color-mix(in srgb, var(--metric-accent) 30%, white);
}

.operations-metric small {
  color: var(--muted-text);
  font-size: 0.8rem;
}

.operations-metric > strong {
  font-size: 1.55rem;
}

.operations-metric-preview-panel {
  padding: 0.8rem 0.95rem;
  display: grid;
  gap: 0.7rem;
}

.operations-metric-preview-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.operations-metric-preview-panel__summary {
  display: grid;
  gap: 0.08rem;
}

.operations-metric-preview-panel__summary small,
.operations-metric-preview-panel__item small {
  color: var(--muted-text);
  font-size: 0.74rem;
}

.operations-metric-preview-panel__summary strong {
  font-size: 1rem;
  line-height: 1.1;
}

.operations-metric-preview-panel__action {
  min-height: 2rem;
  padding: 0 0.8rem;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--accent-color) 16%, var(--border-color));
  background: color-mix(in srgb, var(--panel-color) 92%, white);
  font-size: 0.76rem;
  font-weight: 700;
  color: var(--text-primary);
}

.operations-metric-preview-panel__list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.55rem;
}

.operations-metric-preview-panel__item {
  min-width: 0;
  padding: 0.7rem 0.75rem;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--panel-muted) 76%, white);
  display: grid;
  gap: 0.2rem;
  text-align: left;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.operations-metric-preview-panel__item:hover,
.operations-metric-preview-panel__item:focus-visible,
.operations-metric-preview-panel__action:hover,
.operations-metric-preview-panel__action:focus-visible {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--accent-color) 24%, var(--border-color));
  box-shadow: var(--shadow-soft);
}

.operations-metric-preview-panel__item span {
  font-size: 0.92rem;
  font-weight: 700;
  line-height: 1.25;
  color: var(--text-primary);
}

.operations-mobile-bar {
  display: none;
}

@media (max-width: 1080px) {
  .operations-header__top,
  .operations-header__bottom {
    flex-direction: column;
    align-items: flex-start;
  }

  .operations-header__actions {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .operations-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .operations-metric-preview-panel__list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .operations-view {
    padding-bottom: 6rem;
  }

  .operations-header__primary {
    display: none;
  }

  .operations-metrics {
    grid-template-columns: 1fr;
  }

  .operations-metric-preview-panel {
    display: none;
  }

  .operations-mobile-bar {
    position: fixed;
    left: 1rem;
    right: 1rem;
    bottom: 1rem;
    z-index: 30;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.85rem;
    padding: 0.85rem 0.95rem;
    border-radius: 22px;
    background: color-mix(in srgb, var(--panel-color) 92%, white);
    backdrop-filter: blur(16px);
    box-shadow: var(--shadow-soft);
  }
}
</style>
