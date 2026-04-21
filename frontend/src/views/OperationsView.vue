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
  customers:
    'M17 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2M9 7a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm10 3a3 3 0 1 1 0-6',
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
    {
      id: 'customer-total',
      label: '등록 고객',
      value: String(store.customers.length),
      accent: '#2f80ed',
    },
    {
      id: 'customer-ai',
      label: 'AI 제안 대기',
      value: String(store.pendingCustomerSuggestions.length),
      accent: '#7c4dff',
    },
    { id: 'customer-stable', label: '안정 고객', value: String(stableCount), accent: '#59c36d' },
    { id: 'customer-risk', label: '리스크 고객', value: String(riskCount), accent: '#df5f75' },
  ]
})

const tasknoteMetrics = computed(() => {
  const completedCount = store.personalTaskNotes.filter((task) => task.isMarkedComplete).length

  return [
    {
      id: 'tasknote-today',
      label: '내 업무',
      value: String(store.personalTaskNotes.length),
      accent: '#2f80ed',
    },
    {
      id: 'tasknote-complete',
      label: '완료 표시',
      value: String(completedCount),
      accent: '#59c36d',
    },
    {
      id: 'tasknote-requests',
      label: '상태 변경 요청',
      value: String(store.pendingStatusRequests.length),
      accent: '#f5b64e',
    },
    {
      id: 'tasknote-team',
      label: '팀 일정 보드',
      value: String(store.teamTaskGroups.length),
      accent: '#7c4dff',
    },
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

const pinnedMetric = computed(() => {
  return activeMetrics.value.find((metric) => metric.id === store.activeMetricKey) ?? null
})

const previewMetric = computed(() => {
  return pinnedMetric.value ?? hoveredMetric.value ?? null
})

const previewMetricItems = computed(() => {
  return previewMetric.value ? store.getMetricPreview(previewMetric.value.id) : []
})

const previewGridStyle = computed(() => {
  if (previewMetric.value?.id === 'metric-balance') {
    return {
      gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
    }
  }

  return {
    gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
  }
})

const panelClass =
  'rounded-[24px] border border-[color:var(--border-color)] bg-[var(--panel-color)] shadow-[var(--shadow-soft)]'
const compactPanelClass =
  'rounded-[24px] border border-[color:var(--border-color)] bg-[var(--panel-color)] shadow-[var(--shadow-soft)]'
const primaryButtonClass =
  'inline-flex min-h-[2.7rem] items-center justify-center gap-2 rounded-xl border border-[color:var(--accent-color)] bg-[var(--accent-color)] px-4 text-sm font-semibold text-white shadow-[var(--shadow-soft)] transition duration-200 hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-45 disabled:hover:translate-y-0'
const segmentedButtonClass =
  'min-h-[2.15rem] rounded-full px-4 text-sm font-bold text-[color:var(--muted-text)] transition duration-200'
const filterChipClass =
  'inline-flex min-h-[1.95rem] items-center justify-center rounded-full border border-[color:var(--border-color)] px-3 text-[0.78rem] font-bold text-[color:var(--muted-text)] transition duration-200'
const metricCardClass =
  'relative z-0 flex min-h-[74px] cursor-pointer flex-col items-center justify-center gap-2 overflow-hidden rounded-[16px] border border-[color:var(--border-color)] px-3.5 py-3 text-center shadow-[0_4px_14px_rgba(19,35,68,0.04)] transition duration-200 hover:-translate-y-px hover:shadow-[0_10px_22px_rgba(19,35,68,0.08)] focus-visible:-translate-y-px focus-visible:shadow-[0_10px_22px_rgba(19,35,68,0.08)]'
const hoverPreviewActionClass =
  'inline-flex min-h-8 items-center justify-center rounded-full border border-[color:var(--border-color)] bg-white/80 px-3 text-xs font-bold text-[color:var(--text-primary)] transition duration-200 hover:-translate-y-px hover:shadow-[var(--shadow-soft)]'
const metricPreviewItemClass =
  'grid min-w-0 min-h-[92px] content-start gap-2 overflow-hidden rounded-[18px] border border-[color:var(--border-color)] px-4 py-3.5 text-left shadow-[0_8px_18px_rgba(19,35,68,0.05)] transition duration-200 hover:-translate-y-px hover:shadow-[0_14px_28px_rgba(19,35,68,0.10)] focus-visible:-translate-y-px focus-visible:shadow-[0_14px_28px_rgba(19,35,68,0.10)]'

const pendingPersonalTask = computed(() => {
  return (
    store.personalTaskNotes.find((task) => !task.isMarkedComplete) ??
    store.personalTaskNotes[0] ??
    null
  )
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

  if (
    currentTarget instanceof HTMLElement &&
    nextTarget instanceof Node &&
    currentTarget.contains(nextTarget)
  ) {
    return
  }

  clearMetricPreview()
}

function handleMetricClick(metricId) {
  clearMetricPreview()
  store.activateMetric(metricId)
}

function applyMetric(metricId) {
  clearMetricPreview()

  if (store.activeMetricKey !== metricId) {
    store.activateMetric(metricId)
  }
}

function metricCardStyle(metric) {
  if (store.activeMetricKey === metric.id) {
    return {
      borderColor: `color-mix(in srgb, ${metric.accent} 22%, var(--border-color))`,
      background: `linear-gradient(180deg, color-mix(in srgb, ${metric.accent} 10%, white), color-mix(in srgb, ${metric.accent} 3%, var(--panel-color)))`,
      boxShadow: `0 10px 24px rgba(15, 23, 42, 0.07), 0 0 0 1px color-mix(in srgb, ${metric.accent} 10%, white)`,
    }
  }

  return {
    borderColor: `color-mix(in srgb, ${metric.accent} 12%, var(--border-color))`,
    background: `linear-gradient(180deg, color-mix(in srgb, ${metric.accent} 6%, white), color-mix(in srgb, ${metric.accent} 2%, var(--panel-color)))`,
    boxShadow: '0 4px 14px rgba(19, 35, 68, 0.04)',
  }
}

function metricLabelStyle(metric) {
  return {
    color: `color-mix(in srgb, ${metric.accent} 56%, var(--text-secondary))`,
  }
}

function metricDotStyle(metric) {
  return {
    background: metric.accent,
    boxShadow: `0 0 0 3px color-mix(in srgb, ${metric.accent} 10%, white)`,
  }
}

function metricValueStyle(metric) {
  return {
    color:
      store.activeMetricKey === metric.id
        ? `color-mix(in srgb, ${metric.accent} 52%, var(--text-primary))`
        : `color-mix(in srgb, ${metric.accent} 44%, var(--text-primary))`,
  }
}

function previewItemAccent(metric, item, index) {
  if (metric.id === 'metric-balance') {
    const weekdayPalette = {
      월: '#2f80ed',
      화: '#28b57a',
      수: '#7c4dff',
      목: '#f5b64e',
      금: '#df5f75',
    }

    return weekdayPalette[item.title] ?? metric.accent
  }

  const palette = ['#2f80ed', '#59c36d', '#f5b64e', '#7c4dff', '#df5f75']
  return palette[index % palette.length] ?? metric.accent
}

function previewPanelStyle(metric) {
  if (!metric) return null

  return {
    borderColor: `color-mix(in srgb, ${metric.accent} 28%, var(--border-color))`,
    background: `linear-gradient(180deg, color-mix(in srgb, ${metric.accent} 12%, white), color-mix(in srgb, ${metric.accent} 4%, var(--panel-color)))`,
    boxShadow: `0 18px 36px rgba(19, 35, 68, 0.08)`,
  }
}

function previewItemStyle(metric, item, index) {
  if (!metric) return null

  const accent = previewItemAccent(metric, item, index)

  return {
    borderColor: `color-mix(in srgb, ${accent} 26%, var(--border-color))`,
    background: `linear-gradient(180deg, color-mix(in srgb, ${accent} 16%, white), color-mix(in srgb, ${accent} 8%, var(--panel-muted)))`,
    boxShadow: `0 10px 20px color-mix(in srgb, ${accent} 12%, rgba(19, 35, 68, 0.06))`,
  }
}

function previewItemMarkerStyle(metric, item, index) {
  if (!metric) return null

  const accent = previewItemAccent(metric, item, index)

  return {
    background: accent,
    boxShadow: `0 0 0 4px color-mix(in srgb, ${accent} 16%, white)`,
  }
}

function previewItemTitleStyle(metric, item, index) {
  if (!metric) return null

  const accent = previewItemAccent(metric, item, index)

  return {
    color: `color-mix(in srgb, ${accent} 78%, var(--text-primary))`,
  }
}

function previewItemMetaStyle(metric, item, index) {
  if (!metric) return null

  const accent = previewItemAccent(metric, item, index)

  return {
    color: `color-mix(in srgb, ${accent} 58%, var(--muted-text))`,
  }
}
</script>

<template>
  <section class="grid gap-[0.95rem]">
    <header
      :class="panelClass"
      class="sticky top-0 z-[12] grid gap-[1.05rem] px-5 py-4"
      style="
        background:
          linear-gradient(
            180deg,
            color-mix(in srgb, var(--panel-color) 97%, white),
            var(--panel-color)
          ),
          var(--panel-color);
      "
    >
      <div
        class="flex flex-col items-start justify-between gap-[0.95rem] max-[1080px]:items-start min-[1081px]:flex-row min-[1081px]:items-center"
      >
        <div class="grid gap-[0.45rem]">
          <p class="section-eyebrow">운영 허브</p>
          <h2 class="text-[1.35rem] leading-[1.1] text-[color:var(--text-primary)]">
            {{ activeTabTitle }}
          </h2>
        </div>

        <div
          class="flex w-full flex-wrap items-center justify-between gap-[0.65rem] min-[1081px]:w-auto min-[1081px]:justify-end"
        >
          <div
            class="inline-flex items-center gap-1 rounded-full border border-[color:var(--border-color)] bg-[var(--panel-muted)] p-1"
            role="tablist"
            aria-label="역할 전환"
          >
            <button
              v-for="role in store.operationRoleOptions"
              :key="role.value"
              type="button"
              :class="[
                segmentedButtonClass,
                store.activeRole === role.value
                  ? 'bg-[var(--panel-color)] text-[color:var(--text-primary)] shadow-[var(--shadow-soft)]'
                  : '',
              ]"
              @click="store.setActiveRole(role.value)"
            >
              {{ role.label }}
            </button>
          </div>

          <button
            :class="[primaryButtonClass, 'max-[820px]:hidden']"
            type="button"
            :disabled="headerPrimaryAction.disabled"
            @click="handleHeaderPrimaryAction"
          >
            {{ headerPrimaryAction.label }}
          </button>
        </div>
      </div>

      <div
        class="flex flex-col items-start justify-between gap-[0.95rem] border-t border-[color:var(--border-color)] pt-3 min-[1081px]:flex-row min-[1081px]:items-center"
      >
        <PageTabs
          :active="store.activeTab"
          :tabs="domainTabs"
          @select="store.setActiveTab($event)"
        />

        <div v-if="store.activeTab === 'customers'" class="flex flex-wrap gap-2">
          <button
            v-for="option in customerFilterOptions"
            :key="option.value"
            type="button"
            :class="[
              filterChipClass,
              store.customerHealthFilter === option.value
                ? 'border-[color:var(--accent-color)] bg-[var(--panel-muted)] text-[color:var(--text-primary)]'
                : '',
            ]"
            @click="store.setCustomerHealthFilter(option.value)"
          >
            {{ option.label }}
          </button>
        </div>

        <div v-else-if="store.activeTab === 'workload'" class="flex flex-wrap gap-2">
          <button
            v-for="option in workloadFilterOptions"
            :key="option.value"
            type="button"
            :class="[
              filterChipClass,
              store.workInboxFilter === option.value
                ? 'border-[color:var(--accent-color)] bg-[var(--panel-muted)] text-[color:var(--text-primary)]'
                : '',
            ]"
            @click="store.setWorkInboxFilter(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </header>

    <div
      class="grid gap-[0.55rem]"
      @mouseleave="clearMetricPreview"
      @focusout="handleMetricGroupFocusOut"
    >
      <div class="grid grid-cols-4 gap-[0.75rem] max-[1080px]:grid-cols-2 max-[820px]:grid-cols-1">
        <button
          v-for="metric in activeMetrics"
          :key="metric.id"
          type="button"
          :class="metricCardClass"
          :style="metricCardStyle(metric)"
          :aria-pressed="store.activeMetricKey === metric.id"
          @mouseenter="store.setHoverMetricKey(metric.id)"
          @focus="store.setHoverMetricKey(metric.id)"
          @click="handleMetricClick(metric.id)"
        >
          <div class="flex min-w-0 items-center justify-center gap-2">
            <span
              class="inline-block h-2 w-2 shrink-0 rounded-full"
              :style="metricDotStyle(metric)"
            />
            <small
              class="min-w-0 break-keep whitespace-normal text-[0.78rem] font-semibold leading-[1.2] [overflow-wrap:anywhere]"
              :style="metricLabelStyle(metric)"
            >
              {{ metric.label }}
            </small>
          </div>

          <div class="grid min-w-0 justify-items-center gap-1">
            <strong
              class="break-words text-[1.28rem] leading-none tracking-[-0.02em]"
              :style="metricValueStyle(metric)"
            >
              {{ metric.value }}
            </strong>

            <span class="text-[0.66rem] font-medium leading-4 text-[color:var(--muted-text)]">
              {{ store.activeMetricKey === metric.id ? '적용 중' : '클릭해 보기' }}
            </span>
          </div>
        </button>
      </div>

      <div
        v-if="previewMetric && previewMetricItems.length"
        :class="[compactPanelClass, 'grid gap-[0.9rem] px-4 py-4 max-[820px]:hidden']"
        :style="previewPanelStyle(previewMetric)"
      >
        <div class="flex items-center justify-between gap-[0.85rem]">
          <div class="grid gap-[0.18rem]">
            <small class="text-[0.76rem] text-[color:var(--muted-text)]">{{
              previewMetric.label
            }}</small>
            <strong class="text-[1.05rem] leading-tight text-[color:var(--text-primary)]">{{
              previewMetric.value
            }}</strong>
          </div>

          <button
            type="button"
            :class="hoverPreviewActionClass"
            @click="applyMetric(previewMetric.id)"
          >
            보기 적용
          </button>
        </div>

        <div class="grid gap-[0.75rem]" :style="previewGridStyle">
          <button
            v-for="(item, index) in previewMetricItems"
            :key="`${previewMetric.id}-${item.title}`"
            type="button"
            :class="metricPreviewItemClass"
            :style="previewItemStyle(previewMetric, item, index)"
            @click="applyMetric(previewMetric.id)"
          >
            <div class="flex min-w-0 items-start gap-3">
              <span
                class="mt-1 inline-block h-2.5 w-2.5 shrink-0 rounded-full"
                :style="previewItemMarkerStyle(previewMetric, item, index)"
              />

              <div class="grid min-w-0 gap-1.5">
                <span
                  class="break-keep whitespace-normal text-[0.92rem] font-bold leading-[1.35] [overflow-wrap:anywhere]"
                  :style="previewItemTitleStyle(previewMetric, item, index)"
                >
                  {{ item.title }}
                </span>
                <small
                  class="break-words text-[0.74rem] leading-5"
                  :style="previewItemMetaStyle(previewMetric, item, index)"
                >
                  {{ item.meta }}
                </small>
              </div>
            </div>
          </button>
        </div>
      </div>
    </div>

    <OperationsCustomersBoard v-if="store.activeTab === 'customers'" />
    <OperationsWorkloadBoard v-else-if="store.activeTab === 'workload'" />
    <OperationsTasknotesBoard v-else />

    <div
      :class="[
        compactPanelClass,
        'fixed bottom-4 left-4 right-4 z-30 hidden items-center justify-between gap-3 rounded-[22px] bg-[color:color-mix(in_srgb,var(--panel-color)_92%,white)] px-4 py-3 shadow-[var(--shadow-soft)] backdrop-blur-[16px] max-[820px]:flex',
      ]"
    >
      <strong class="text-[color:var(--text-primary)]">{{ headerPrimaryAction.label }}</strong>
      <button
        :class="primaryButtonClass"
        type="button"
        :disabled="headerPrimaryAction.disabled"
        @click="handleHeaderPrimaryAction"
      >
        실행
      </button>
    </div>
  </section>
</template>
