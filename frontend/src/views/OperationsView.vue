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

const panelClass =
  'rounded-[8px] border border-[color:var(--border-strong)] bg-[var(--panel-color)] shadow-[0_8px_24px_rgba(19,35,68,0.05)]'
const compactPanelClass =
  'rounded-[8px] border border-[color:var(--border-strong)] bg-[var(--panel-color)] shadow-[0_8px_24px_rgba(19,35,68,0.05)]'
const primaryButtonClass =
  'inline-flex min-h-[2.7rem] items-center justify-center gap-2 rounded-[6px] border border-[color:color-mix(in_srgb,var(--accent-color)_50%,var(--border-strong))] bg-[var(--accent-color)] px-4 text-sm font-semibold text-white shadow-[0_8px_18px_rgba(19,35,68,0.1)] transition duration-200 hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-45 disabled:hover:translate-y-0'
const segmentedButtonClass =
  'min-h-[2.15rem] rounded-[6px] border border-transparent px-4 text-sm font-bold text-[color:var(--muted-text)] transition duration-200'
const filterChipClass =
  'inline-flex min-h-[1.95rem] items-center justify-center rounded-[6px] border border-[color:var(--border-strong)] px-3 text-[0.78rem] font-bold text-[color:var(--muted-text)] transition duration-200'
const metricCardClass =
  'relative z-0 flex min-h-[66px] cursor-pointer flex-col items-center justify-center gap-1.5 overflow-hidden rounded-[6px] border border-[color:var(--border-strong)] px-3 py-2.5 text-center shadow-[inset_0_1px_0_rgba(255,255,255,0.7),0_2px_8px_rgba(19,35,68,0.03)] transition duration-200 hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.7),0_4px_12px_rgba(19,35,68,0.05)] focus-visible:shadow-[inset_0_1px_0_rgba(255,255,255,0.7),0_4px_12px_rgba(19,35,68,0.05)]'

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

function handleMetricClick(metricId) {
  store.activateMetric(metricId)
}

function metricCardStyle(metric) {
  if (store.activeMetricKey === metric.id) {
    return {
      borderColor: `color-mix(in srgb, ${metric.accent} 22%, var(--border-strong))`,
      background: `linear-gradient(180deg, color-mix(in srgb, ${metric.accent} 7%, white), color-mix(in srgb, ${metric.accent} 2%, var(--panel-color)))`,
      boxShadow: `inset 0 1px 0 rgba(255,255,255,0.75), 0 4px 12px rgba(15, 23, 42, 0.05), 0 0 0 1px color-mix(in srgb, ${metric.accent} 10%, white)`,
    }
  }

  return {
    borderColor: `color-mix(in srgb, ${metric.accent} 12%, var(--border-strong))`,
    background: `linear-gradient(180deg, color-mix(in srgb, ${metric.accent} 4%, white), color-mix(in srgb, ${metric.accent} 1%, var(--panel-color)))`,
    boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.7), 0 2px 8px rgba(19, 35, 68, 0.03)',
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
            class="inline-flex items-center gap-1 rounded-[8px] border border-[color:var(--border-strong)] bg-[var(--panel-muted)] p-1"
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
                  ? 'border-[color:var(--border-strong)] bg-[var(--panel-color)] text-[color:var(--text-primary)] shadow-[0_4px_10px_rgba(19,35,68,0.05)]'
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
                ? 'border-[color:var(--border-strong)] bg-[var(--panel-muted)] text-[color:var(--text-primary)]'
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
                ? 'border-[color:var(--border-strong)] bg-[var(--panel-muted)] text-[color:var(--text-primary)]'
                : '',
            ]"
            @click="store.setWorkInboxFilter(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </header>

    <div class="grid gap-[0.45rem]">
      <div class="grid grid-cols-4 gap-[0.65rem] max-[1080px]:grid-cols-2 max-[820px]:grid-cols-1">
        <button
          v-for="metric in activeMetrics"
          :key="metric.id"
          type="button"
          :class="metricCardClass"
          :style="metricCardStyle(metric)"
          :aria-pressed="store.activeMetricKey === metric.id"
          @click="handleMetricClick(metric.id)"
        >
          <div class="flex min-w-0 items-center justify-center gap-1.5">
            <span
              class="inline-block h-1.5 w-1.5 shrink-0 rounded-full"
              :style="metricDotStyle(metric)"
            />
            <small
              class="min-w-0 break-keep whitespace-normal text-[0.72rem] font-semibold leading-[1.15] [overflow-wrap:anywhere]"
              :style="metricLabelStyle(metric)"
            >
              {{ metric.label }}
            </small>
          </div>

          <div class="grid min-w-0 justify-items-center gap-0.5">
            <strong
              class="break-words text-[1.12rem] leading-none tracking-[-0.01em]"
              :style="metricValueStyle(metric)"
            >
              {{ metric.value }}
            </strong>

            <span class="text-[0.62rem] font-medium leading-4 text-[color:var(--muted-text)]">
              {{ store.activeMetricKey === metric.id ? '적용 중' : '클릭해 보기' }}
            </span>
          </div>
        </button>
      </div>
    </div>

    <OperationsCustomersBoard v-if="store.activeTab === 'customers'" />
    <OperationsWorkloadBoard v-else-if="store.activeTab === 'workload'" />
    <OperationsTasknotesBoard v-else />

    <div
      :class="[
        compactPanelClass,
        'fixed bottom-4 left-4 right-4 z-30 hidden items-center justify-between gap-3 rounded-[8px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-color)_92%,white)] px-4 py-3 shadow-[0_10px_24px_rgba(19,35,68,0.08)] backdrop-blur-[16px] max-[820px]:flex',
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
