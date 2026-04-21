<script setup>
import { computed } from 'vue'
import { useOperationsStore } from '@/stores/operations'
import { usePlannerStore } from '@/stores/planner'
import { formatLongDate, formatShortDate } from '@/utils/calendar'

const store = useOperationsStore()
const planner = usePlannerStore()

const workItemStateLabel = {
  draft: '초안',
  saved: '초안 저장',
  submitted: '승인 요청',
  waiting: '승인 수집',
  ready: '생성 가능',
  balanced: '균형 양호',
  watch: '재조정 필요',
}

const workItemStateTone = {
  draft: 'watch',
  saved: 'watch',
  submitted: 'risk',
  waiting: 'watch',
  ready: 'healthy',
  balanced: 'healthy',
  watch: 'risk',
}

const selectedItem = computed(() => store.selectedWorkItemDetail ?? null)

const queueStats = computed(() => {
  const items = store.workInboxItems

  return [
    { id: 'queue-total', label: '대기 업무', value: `${items.length}건`, tone: 'neutral' },
    {
      id: 'queue-qa',
      label: 'QA 요청',
      value: `${items.filter((item) => item.type === 'qa').length}건`,
      tone: 'risk',
    },
    {
      id: 'queue-meeting',
      label: '회의 정리',
      value: `${items.filter((item) => item.type === 'meeting').length}건`,
      tone: 'watch',
    },
    {
      id: 'queue-generator',
      label: '기본 생성',
      value: `${items.filter((item) => item.type === 'generator').length}건`,
      tone: 'healthy',
    },
  ]
})

const detailEyebrow = computed(() => {
  if (selectedItem.value?.type === 'qa') return 'QA Intake'
  if (selectedItem.value?.type === 'meeting') return 'Meeting Alignment'
  if (selectedItem.value?.type === 'generator') return 'Content Generator'
  return 'Operations Hub'
})

const detailTitle = computed(() => {
  if (selectedItem.value?.type === 'qa') return selectedItem.value.title ?? 'QA 요청'
  if (selectedItem.value?.type === 'meeting') return '내부 회의 승인 흐름'
  if (selectedItem.value?.type === 'generator') return '기본 콘텐츠 생성'
  return '업무를 선택해 세부 내용을 확인하세요'
})

const detailDescription = computed(() => {
  if (selectedItem.value?.type === 'qa') {
    return '카카오톡 요청 원문과 AI 브리프를 함께 확인하면서 수정 범위를 빠르게 정리합니다.'
  }

  if (selectedItem.value?.type === 'meeting') {
    return '회의록, 참석자 승인, 후속 카드 생성 요청까지 한 흐름에서 관리합니다.'
  }

  if (selectedItem.value?.type === 'generator') {
    return '주간 균형과 요일 분배 규칙을 보면서 기본 콘텐츠 배치를 운영 보드 기준으로 검토합니다.'
  }

  return '업무 큐에서 항목을 선택하면 세부 정보와 실행 액션이 이 영역에 표시됩니다.'
})

const pipelineSteps = computed(() => {
  if (selectedItem.value?.type !== 'meeting') {
    return []
  }

  return [
    { id: 'meeting-note', label: '회의록 작성', active: true },
    { id: 'meeting-approve', label: '참석자 승인', active: true },
    { id: 'meeting-ai', label: 'AI 정리 완료', active: selectedItem.value.approvalsCompleted },
    { id: 'meeting-task', label: '카드 추가 요청', active: selectedItem.value.approvalsCompleted },
  ]
})

const actionSummaryRows = computed(() => {
  if (!selectedItem.value) {
    return []
  }

  if (selectedItem.value.type === 'qa') {
    return [
      { id: 'summary-date', label: '마감일', value: safeDate(resolveItemDate(selectedItem.value)) },
      {
        id: 'summary-assignee',
        label: '추천 담당자',
        value: selectedItem.value.recommendedAssignee?.name ?? '-',
      },
      { id: 'summary-task', label: '대상 카드', value: selectedItem.value.targetTaskLabel ?? '-' },
    ]
  }

  if (selectedItem.value.type === 'meeting') {
    return [
      { id: 'summary-date', label: '회의일', value: safeLongDate(selectedItem.value.meetingDate) },
      { id: 'summary-progress', label: '승인 현황', value: meetingProgress(selectedItem.value) },
      { id: 'summary-customer', label: '고객', value: resolveCustomerName(selectedItem.value) },
    ]
  }

  return [
    {
      id: 'summary-date',
      label: '첫 배치일',
      value: safeDate(resolveItemDate(selectedItem.value)),
    },
    { id: 'summary-count', label: '생성 건수', value: `${store.generatorPreview.items.length}건` },
    { id: 'summary-spread', label: '편차', value: String(store.generatorPreview.spread) },
  ]
})

const boardShellClass =
  'grid items-start gap-4 [grid-template-columns:minmax(300px,340px)_minmax(0,1fr)_minmax(290px,320px)] max-[1440px]:grid-cols-1'
const railClass =
  'grid gap-4 rounded-[10px] border border-[color:var(--border-color)] bg-[linear-gradient(180deg,color-mix(in_srgb,var(--panel-color)_94%,white),var(--panel-color))] px-5 py-5 shadow-[0_14px_34px_rgba(15,23,42,0.07)]'
const sectionCardClass =
  'grid gap-3 rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-color)_86%,white)] px-4 py-4'
const sectionSoftCardClass =
  'grid gap-3 rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_72%,white)] px-4 py-4'
const summaryCardClass =
  'grid gap-1 rounded-[8px] border px-4 py-3 shadow-[inset_0_1px_0_rgba(255,255,255,0.55)]'
const queueItemClass =
  'grid gap-3 rounded-[8px] border px-4 py-4 text-left transition duration-200 hover:-translate-y-px hover:shadow-[0_10px_24px_rgba(15,23,42,0.08)]'
const badgeBaseClass =
  'inline-flex min-h-7 items-center justify-center rounded-[6px] border px-2.5 text-[0.72rem] font-semibold tracking-[0.01em]'
const pillClass =
  'inline-flex min-h-7 items-center justify-center rounded-[6px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_80%,white)] px-2.5 text-[0.72rem] font-semibold text-[color:var(--muted-text)]'
const ghostButtonClass =
  'inline-flex min-h-[2.5rem] items-center justify-center gap-2 rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-color)_90%,white)] px-3.5 text-sm font-semibold text-[color:var(--text-primary)] transition duration-200 hover:-translate-y-px hover:shadow-[0_8px_22px_rgba(15,23,42,0.08)]'
const primaryButtonClass =
  'inline-flex min-h-[2.8rem] items-center justify-center gap-2 rounded-[8px] border border-[color:var(--accent-color)] bg-[linear-gradient(180deg,color-mix(in_srgb,var(--accent-color)_88%,white),var(--accent-color))] px-4 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(15,23,42,0.16)] transition duration-200 hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-45 disabled:hover:translate-y-0'
const inlineControlButtonClass =
  'inline-flex h-10 w-10 items-center justify-center rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-color)_88%,white)] text-base font-semibold text-[color:var(--text-primary)] transition duration-200 hover:-translate-y-px hover:shadow-[0_8px_20px_rgba(15,23,42,0.08)]'
const emptyStateClass =
  'grid gap-2 rounded-[8px] border border-dashed border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_60%,white)] px-5 py-6 text-sm text-[color:var(--muted-text)]'
const metaRowClass =
  'flex items-start justify-between gap-4 border-b border-[color:var(--border-color)] py-3 last:border-b-0 last:pb-0 first:pt-0'

const focusStyle = {
  borderColor: 'color-mix(in srgb, var(--accent-color) 32%, var(--border-color))',
  boxShadow: '0 0 0 3px color-mix(in srgb, var(--accent-color) 10%, transparent)',
}

function workItemTone(status) {
  return workItemStateTone[status] ?? 'neutral'
}

function workItemLabel(status) {
  return workItemStateLabel[status] ?? status
}

function workTypeLabel(type) {
  if (type === 'qa') return 'QA'
  if (type === 'meeting') return '회의'
  if (type === 'generator') return '생성'
  return '업무'
}

function isWorkItemActive(item) {
  return store.selectedWorkItem?.type === item.type && store.selectedWorkItem?.id === item.id
}

function selectWorkItem(item) {
  store.setSelectedWorkItem({
    type: item.type,
    id: item.id,
  })
}

function resolveItemDate(item) {
  if (!item) return null
  if (item.type === 'meeting') return item.meetingDate
  if (item.type === 'generator') return store.generatorPreview.items[0]?.dueDate ?? null
  return item.dueDate ?? null
}

function resolveCustomerName(item) {
  if (!item) return '-'
  if (typeof item.customer === 'string') return item.customer
  if (item.customer?.name) return item.customer.name
  return item.subtitle ?? '내부 보드'
}

function safeDate(value) {
  return value ? formatShortDate(value) : '-'
}

function safeLongDate(value) {
  return value ? formatLongDate(value) : '-'
}

function meetingProgress(note) {
  return `${note.approvalCount}/${note.attendees.length} 승인`
}

function openPlannerTask(taskId) {
  if (!taskId) {
    return
  }

  planner.openTask(taskId)
}

function currentAction() {
  const item = selectedItem.value

  if (!item) {
    return
  }

  if (item.type === 'generator') {
    if (store.activeRole === 'admin') {
      store.createGeneratedContentBatch()
    }
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

function actionLabel() {
  const item = selectedItem.value

  if (!item) return '대기 업무 없음'

  if (item.type === 'generator') {
    return store.activeRole === 'admin' ? '기본 콘텐츠 생성 실행' : '기본 생성은 관리자 전용'
  }

  if (item.type === 'qa') {
    return store.activeRole === 'admin' ? '승인 후 카드 생성' : '요청 초안 저장'
  }

  return store.activeRole === 'admin' ? '카드 추가 요청 생성' : '승인 진행 중'
}

function actionDisabled() {
  const item = selectedItem.value

  if (!item) return true
  if (item.type === 'generator') return store.activeRole !== 'admin'
  if (item.type === 'qa') return false
  return store.activeRole !== 'admin' || !item.approvalsCompleted
}

function toneSurfaceStyle(tone) {
  if (tone === 'healthy') {
    return {
      borderColor: 'color-mix(in srgb, var(--success-color) 28%, var(--border-color))',
      background:
        'linear-gradient(180deg, color-mix(in srgb, var(--success-color) 14%, white), color-mix(in srgb, var(--success-color) 6%, var(--panel-color)))',
    }
  }

  if (tone === 'watch') {
    return {
      borderColor: 'color-mix(in srgb, var(--warning-color) 28%, var(--border-color))',
      background:
        'linear-gradient(180deg, color-mix(in srgb, var(--warning-color) 15%, white), color-mix(in srgb, var(--warning-color) 7%, var(--panel-color)))',
    }
  }

  if (tone === 'risk') {
    return {
      borderColor: 'color-mix(in srgb, var(--danger-color) 28%, var(--border-color))',
      background:
        'linear-gradient(180deg, color-mix(in srgb, var(--danger-color) 12%, white), color-mix(in srgb, var(--danger-color) 5%, var(--panel-color)))',
    }
  }

  return {
    borderColor: 'var(--border-color)',
    background:
      'linear-gradient(180deg, color-mix(in srgb, var(--panel-muted) 72%, white), var(--panel-color))',
  }
}

function badgeStyle(tone) {
  if (tone === 'healthy') {
    return {
      background: 'color-mix(in srgb, var(--success-color) 12%, white)',
      borderColor: 'color-mix(in srgb, var(--success-color) 28%, var(--border-color))',
      color: '#287b47',
    }
  }

  if (tone === 'watch') {
    return {
      background: 'color-mix(in srgb, var(--warning-color) 15%, white)',
      borderColor: 'color-mix(in srgb, var(--warning-color) 28%, var(--border-color))',
      color: '#9a6c0d',
    }
  }

  if (tone === 'risk') {
    return {
      background: 'color-mix(in srgb, var(--danger-color) 12%, white)',
      borderColor: 'color-mix(in srgb, var(--danger-color) 28%, var(--border-color))',
      color: '#b2455f',
    }
  }

  return {
    background: 'var(--panel-muted)',
    borderColor: 'var(--border-color)',
    color: 'var(--muted-text)',
  }
}

function generatorCardStyle(item) {
  const accent = item.palette?.accent ?? 'var(--accent-color)'

  return {
    borderColor: `color-mix(in srgb, ${accent} 24%, var(--border-color))`,
    background: `linear-gradient(180deg, color-mix(in srgb, ${accent} 12%, white), color-mix(in srgb, ${accent} 5%, var(--panel-color)))`,
  }
}

function generatorBarStyle(count, index) {
  const maxCount = Math.max(...store.generatorPreview.counts, 1)
  const height = 22 + Math.round((count / maxCount) * 74)
  const palette = ['#0f62fe', '#4589ff', '#525252', '#24a148', '#8a3ffc']

  return {
    height: `${height}px`,
    background: `linear-gradient(180deg, ${palette[index] ?? 'var(--accent-color)'}, color-mix(in srgb, ${palette[index] ?? 'var(--accent-color)'} 78%, black))`,
  }
}

function pipelineStepStyle(active) {
  if (!active) {
    return {
      borderColor: 'var(--border-color)',
      background: 'color-mix(in srgb, var(--panel-muted) 74%, white)',
      color: 'var(--muted-text)',
    }
  }

  return {
    borderColor: 'color-mix(in srgb, var(--accent-color) 24%, var(--border-color))',
    background: 'color-mix(in srgb, var(--accent-color) 10%, white)',
    color: 'var(--text-primary)',
  }
}

function attendeeCardStyle(active) {
  if (!active) {
    return null
  }

  return {
    borderColor: 'color-mix(in srgb, var(--success-color) 30%, var(--border-color))',
    background: 'color-mix(in srgb, var(--success-color) 12%, white)',
  }
}
</script>

<template>
  <section :class="boardShellClass">
    <aside
      :class="[railClass, 'sticky top-[7.1rem] max-[1440px]:static']"
      :style="store.focusTarget === 'workload-inbox' ? focusStyle : null"
    >
      <header class="flex items-start justify-between gap-4">
        <div class="grid gap-1">
          <p
            class="text-[0.72rem] font-semibold uppercase tracking-[0.16em] text-[color:var(--accent-color)]"
          >
            Operations Hub
          </p>
          <h3 class="text-[1.05rem] font-semibold text-[color:var(--text-primary)]">업무 큐</h3>
          <p class="text-sm leading-6 text-[color:var(--muted-text)]">
            대기 업무를 유형별로 정리하고 현재 처리 대상을 빠르게 전환합니다.
          </p>
        </div>

        <span :class="pillClass">{{ store.workInboxItems.length }} items</span>
      </header>

      <div class="grid grid-cols-2 gap-3">
        <article
          v-for="stat in queueStats"
          :key="stat.id"
          :class="summaryCardClass"
          :style="toneSurfaceStyle(stat.tone)"
        >
          <span
            class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
            >{{ stat.label }}</span
          >
          <strong class="text-lg font-semibold text-[color:var(--text-primary)]">{{
            stat.value
          }}</strong>
        </article>
      </div>

      <div class="grid gap-2">
        <button
          v-for="item in store.workInboxItems"
          :key="`${item.type}-${item.id}`"
          type="button"
          :class="queueItemClass"
          :style="
            isWorkItemActive(item)
              ? {
                  borderColor: 'color-mix(in srgb, var(--accent-color) 32%, var(--border-color))',
                  background:
                    'linear-gradient(180deg, color-mix(in srgb, var(--accent-color) 12%, white), color-mix(in srgb, var(--accent-color) 5%, var(--panel-color)))',
                  boxShadow: '0 12px 26px rgba(15, 23, 42, 0.09)',
                }
              : {
                  borderColor: 'var(--border-color)',
                  background: 'color-mix(in srgb, var(--panel-muted) 74%, white)',
                }
          "
          @click="selectWorkItem(item)"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="grid gap-1.5">
              <div class="flex flex-wrap items-center gap-2">
                <span
                  class="text-[0.7rem] font-semibold uppercase tracking-[0.12em] text-[color:var(--accent-color)]"
                  >{{ item.section }}</span
                >
                <span :class="pillClass">{{ workTypeLabel(item.type) }}</span>
              </div>
              <strong class="text-sm font-semibold leading-6 text-[color:var(--text-primary)]">{{
                item.title
              }}</strong>
              <p class="text-sm text-[color:var(--muted-text)]">{{ resolveCustomerName(item) }}</p>
            </div>

            <small class="text-xs font-medium text-[color:var(--muted-text)]">{{
              safeDate(resolveItemDate(item))
            }}</small>
          </div>

          <div class="flex items-center justify-between gap-3">
            <span :class="badgeBaseClass" :style="badgeStyle(workItemTone(item.status))">
              {{ workItemLabel(item.status) }}
            </span>
            <span class="text-xs text-[color:var(--muted-text)]">{{
              item.subtitle ?? '운영 대기열'
            }}</span>
          </div>
        </button>

        <div v-if="!store.workInboxItems.length" :class="emptyStateClass">
          <strong class="text-sm font-semibold text-[color:var(--text-primary)]"
            >대기 업무가 없습니다.</strong
          >
          <p>현재 필터 기준으로 표시할 인박스 항목이 없습니다.</p>
        </div>
      </div>
    </aside>

    <article :class="railClass">
      <header
        class="flex items-start justify-between gap-4 max-[980px]:flex-col max-[980px]:items-start"
      >
        <div class="grid gap-2">
          <p
            class="text-[0.72rem] font-semibold uppercase tracking-[0.16em] text-[color:var(--accent-color)]"
          >
            {{ detailEyebrow }}
          </p>
          <h3 class="text-[1.18rem] font-semibold text-[color:var(--text-primary)]">
            {{ detailTitle }}
          </h3>
          <p class="max-w-[62ch] text-sm leading-6 text-[color:var(--muted-text)]">
            {{ detailDescription }}
          </p>
        </div>

        <div v-if="selectedItem" class="flex flex-wrap items-center gap-2">
          <span :class="pillClass">{{ safeDate(resolveItemDate(selectedItem)) }}</span>
          <span :class="pillClass">{{ resolveCustomerName(selectedItem) }}</span>
          <span
            :class="badgeBaseClass"
            :style="badgeStyle(workItemTone(selectedItem.status ?? 'ready'))"
          >
            {{ workItemLabel(selectedItem.status ?? 'ready') }}
          </span>
        </div>
      </header>

      <template v-if="selectedItem?.type === 'qa'">
        <section class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          <article
            :class="sectionCardClass"
            :style="store.focusTarget === 'workload-meeting' ? focusStyle : null"
          >
            <div class="flex items-center justify-between gap-3">
              <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">요청 원문</h4>
              <span :class="pillClass">{{ selectedItem.source }}</span>
            </div>
            <p
              class="rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-color)_90%,white)] px-4 py-4 text-sm leading-7 text-[color:var(--text-secondary)]"
            >
              {{ selectedItem.sourceMessage }}
            </p>
          </article>

          <article
            :class="sectionCardClass"
            :style="store.focusTarget === 'workload-generator' ? focusStyle : null"
          >
            <div class="flex items-center justify-between gap-3">
              <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">AI 브리프</h4>
              <span :class="badgeBaseClass" :style="badgeStyle(workItemTone(selectedItem.status))">
                {{ workItemLabel(selectedItem.status) }}
              </span>
            </div>
            <p
              class="rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-color)_90%,white)] px-4 py-4 text-sm leading-7 text-[color:var(--text-secondary)]"
            >
              {{ selectedItem.aiSummary }}
            </p>
          </article>
        </section>

        <section class="grid gap-4 lg:grid-cols-2">
          <article :class="sectionSoftCardClass">
            <span
              class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
              >수정 대상</span
            >
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
              selectedItem.targetTaskLabel
            }}</strong>
            <p class="text-sm text-[color:var(--muted-text)]">{{ selectedItem.targetTaskId }}</p>
          </article>

          <article :class="sectionSoftCardClass">
            <span
              class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
              >추천 담당자</span
            >
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
              selectedItem.recommendedAssignee?.name ?? '-'
            }}</strong>
            <p class="text-sm text-[color:var(--muted-text)]">
              {{ selectedItem.recommendedAssignee?.role ?? '배정 전' }}
            </p>
          </article>
        </section>

        <article :class="sectionCardClass">
          <div class="flex items-center justify-between gap-3">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">추출 요청사항</h4>
            <span :class="pillClass">{{ selectedItem.requestedChanges.length }} items</span>
          </div>
          <ul class="grid gap-2 text-sm leading-6 text-[color:var(--text-secondary)]">
            <li
              v-for="change in selectedItem.requestedChanges"
              :key="change"
              class="rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_68%,white)] px-3 py-3"
            >
              {{ change }}
            </li>
          </ul>
        </article>
      </template>

      <template v-else-if="selectedItem?.type === 'meeting'">
        <section class="grid gap-4 lg:grid-cols-2">
          <article :class="sectionSoftCardClass">
            <span
              class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
              >회의일</span
            >
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
              safeLongDate(selectedItem.meetingDate)
            }}</strong>
            <p class="text-sm text-[color:var(--muted-text)]">
              {{ resolveCustomerName(selectedItem) }}
            </p>
          </article>

          <article :class="sectionSoftCardClass">
            <span
              class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
              >진행 상태</span
            >
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
              meetingProgress(selectedItem)
            }}</strong>
            <p class="text-sm text-[color:var(--muted-text)]">
              {{ selectedItem.approvalsCompleted ? '카드 생성 가능' : '승인 수집 중' }}
            </p>
          </article>
        </section>

        <section class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          <article :class="sectionCardClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">회의록 원문</h4>
            <p
              class="rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-color)_90%,white)] px-4 py-4 text-sm leading-7 text-[color:var(--text-secondary)]"
            >
              {{ selectedItem.transcript }}
            </p>
          </article>

          <article :class="sectionCardClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">AI 정리 결과</h4>
            <p
              class="rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-color)_90%,white)] px-4 py-4 text-sm leading-7 text-[color:var(--text-secondary)]"
            >
              {{ selectedItem.aiSummary }}
            </p>
          </article>
        </section>

        <article :class="sectionCardClass">
          <div class="flex items-center justify-between gap-3">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">승인 파이프라인</h4>
            <span :class="pillClass">{{
              selectedItem.approvalsCompleted ? '완료 가능' : '수집 중'
            }}</span>
          </div>

          <div class="grid gap-3 lg:grid-cols-4">
            <div
              v-for="step in pipelineSteps"
              :key="step.id"
              class="grid min-h-[74px] content-center gap-1 rounded-[8px] border px-4 py-3 text-left"
              :style="pipelineStepStyle(step.active)"
            >
              <span class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] opacity-70"
                >Step</span
              >
              <strong class="text-sm font-semibold">{{ step.label }}</strong>
            </div>
          </div>
        </article>

        <article :class="sectionCardClass">
          <div class="flex items-center justify-between gap-3">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">참석자 승인</h4>
            <span :class="pillClass">{{ meetingProgress(selectedItem) }}</span>
          </div>

          <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            <button
              v-for="attendee in selectedItem.attendees"
              :key="attendee.memberId"
              type="button"
              class="grid gap-1 rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-color)_90%,white)] px-4 py-4 text-left transition duration-200 hover:-translate-y-px hover:shadow-[0_8px_20px_rgba(15,23,42,0.08)]"
              :style="attendeeCardStyle(attendee.approved)"
              @click="store.toggleMeetingApproval(selectedItem.id, attendee.memberId)"
            >
              <div class="flex items-center justify-between gap-3">
                <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                  store.findMember(attendee.memberId)?.name
                }}</strong>
                <span
                  :class="badgeBaseClass"
                  :style="badgeStyle(attendee.approved ? 'healthy' : 'watch')"
                >
                  {{ attendee.approved ? '승인' : '대기' }}
                </span>
              </div>
              <span class="text-sm text-[color:var(--muted-text)]">{{
                store.findMember(attendee.memberId)?.role ??
                store.findMember(attendee.memberId)?.initials
              }}</span>
            </button>
          </div>
        </article>
      </template>

      <template v-else-if="selectedItem?.type === 'generator'">
        <section class="grid gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
          <article :class="sectionCardClass">
            <div class="flex items-center justify-between gap-3">
              <div class="grid gap-1">
                <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">배치 설정</h4>
                <p class="text-sm text-[color:var(--muted-text)]">
                  운영 허브 기준으로 생성 건수를 조절합니다.
                </p>
              </div>

              <div class="flex items-center gap-2">
                <button
                  :class="inlineControlButtonClass"
                  type="button"
                  @click="store.setGeneratedBatchSize(store.generatedBatchSize - 1)"
                >
                  -
                </button>
                <strong
                  class="min-w-8 text-center text-base font-semibold text-[color:var(--text-primary)]"
                  >{{ store.generatedBatchSize }}</strong
                >
                <button
                  :class="inlineControlButtonClass"
                  type="button"
                  @click="store.setGeneratedBatchSize(store.generatedBatchSize + 1)"
                >
                  +
                </button>
              </div>
            </div>

            <div
              class="grid gap-3 rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_72%,white)] px-4 py-4"
            >
              <span
                class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
                >요일 분배 규칙</span
              >
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]"
                >월 → 수 → 금 → 화 → 목</strong
              >
              <p class="text-sm leading-6 text-[color:var(--muted-text)]">
                주간 편차를 낮추기 위해 분산 순서를 고정해 기본 콘텐츠를 생성합니다.
              </p>
            </div>
          </article>

          <article
            :class="[sectionCardClass]"
            :style="store.focusTarget === 'workload-balance' ? focusStyle : null"
          >
            <div class="flex items-center justify-between gap-3">
              <div class="grid gap-1">
                <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">생성 개요</h4>
                <p class="text-sm text-[color:var(--muted-text)]">
                  실행 전에 생성 볼륨과 분산 상태를 먼저 확인합니다.
                </p>
              </div>
              <span :class="pillClass">편차 {{ store.generatorPreview.spread }}</span>
            </div>

            <div class="grid gap-3 sm:grid-cols-3">
              <article :class="sectionSoftCardClass">
                <span
                  class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
                  >생성 건수</span
                >
                <strong class="text-base font-semibold text-[color:var(--text-primary)]"
                  >{{ store.generatorPreview.items.length }}건</strong
                >
              </article>
              <article :class="sectionSoftCardClass">
                <span
                  class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
                  >첫 배치일</span
                >
                <strong class="text-base font-semibold text-[color:var(--text-primary)]">{{
                  safeDate(store.generatorPreview.items[0]?.dueDate)
                }}</strong>
              </article>
              <article :class="sectionSoftCardClass">
                <span
                  class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
                  >현재 역할</span
                >
                <strong class="text-base font-semibold text-[color:var(--text-primary)]">{{
                  store.activeRole === 'admin' ? '관리자' : '실무자'
                }}</strong>
              </article>
            </div>
          </article>
        </section>

        <article
          :class="[sectionCardClass]"
          :style="store.focusTarget === 'workload-generator' ? focusStyle : null"
        >
          <div class="flex items-center justify-between gap-3">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">생성 미리보기</h4>
            <span :class="pillClass">{{ store.generatorPreview.items.length }} items</span>
          </div>

          <div class="grid gap-3 lg:grid-cols-2">
            <article
              v-for="item in store.generatorPreview.items"
              :key="item.previewId"
              class="grid gap-3 rounded-[8px] border px-4 py-4"
              :style="generatorCardStyle(item)"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="grid gap-1">
                  <span
                    class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
                    >{{ item.customer }}</span
                  >
                  <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                    item.title
                  }}</strong>
                </div>
                <span :class="pillClass">{{ item.weekdayLabel }}요일</span>
              </div>
              <p class="text-sm leading-6 text-[color:var(--muted-text)]">{{ item.summary }}</p>
              <div
                class="flex items-center justify-between gap-3 text-xs text-[color:var(--muted-text)]"
              >
                <span>{{ safeDate(item.dueDate) }}</span>
                <span>{{ item.weekdayLabel }} 분배</span>
              </div>
            </article>
          </div>
        </article>

        <article :class="sectionCardClass">
          <div class="flex items-center justify-between gap-3">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">주간 균형</h4>
            <span :class="pillClass">Mon-Fri Balance</span>
          </div>

          <div class="grid gap-3 sm:grid-cols-5">
            <div
              v-for="(count, index) in store.generatorPreview.counts"
              :key="index"
              class="grid justify-items-center gap-2 rounded-[8px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_70%,white)] px-3 py-4"
            >
              <small
                class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
              >
                {{ ['월', '화', '수', '목', '금'][index] }}
              </small>
              <div
                class="flex h-[118px] w-full items-end rounded-[8px] bg-[color:color-mix(in_srgb,var(--panel-color)_88%,white)] px-2 py-2"
              >
                <span class="w-full rounded-[6px]" :style="generatorBarStyle(count, index)" />
              </div>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                count
              }}</strong>
            </div>
          </div>
        </article>
      </template>

      <div v-else :class="emptyStateClass">
        <strong class="text-sm font-semibold text-[color:var(--text-primary)]"
          >선택된 업무가 없습니다.</strong
        >
        <p>좌측 업무 큐에서 항목을 선택하면 세부 정보와 실행 액션을 확인할 수 있습니다.</p>
      </div>
    </article>

    <aside :class="[railClass, 'sticky top-[7.1rem] max-[1440px]:static']">
      <header class="flex items-start justify-between gap-3">
        <div class="grid gap-1">
          <p
            class="text-[0.72rem] font-semibold uppercase tracking-[0.16em] text-[color:var(--accent-color)]"
          >
            Decision Panel
          </p>
          <h3 class="text-[1.05rem] font-semibold text-[color:var(--text-primary)]">실행 컨트롤</h3>
        </div>

        <button
          v-if="selectedItem?.linkedTaskId"
          :class="ghostButtonClass"
          type="button"
          @click="openPlannerTask(selectedItem.linkedTaskId)"
        >
          연결 카드 보기
        </button>
      </header>

      <div v-if="selectedItem" class="grid gap-4">
        <article :class="sectionSoftCardClass">
          <span
            class="text-[0.72rem] font-semibold uppercase tracking-[0.08em] text-[color:var(--muted-text)]"
            >현재 항목</span
          >
          <strong class="text-base font-semibold leading-6 text-[color:var(--text-primary)]">
            {{ selectedItem.type === 'generator' ? '기본 콘텐츠 생성' : selectedItem.title }}
          </strong>
          <p class="text-sm leading-6 text-[color:var(--muted-text)]">
            {{
              selectedItem.type === 'generator'
                ? '자동 배분 규칙을 적용한 생성 배치'
                : resolveCustomerName(selectedItem)
            }}
          </p>
          <span
            :class="badgeBaseClass"
            :style="badgeStyle(workItemTone(selectedItem.status ?? 'ready'))"
          >
            {{ workItemLabel(selectedItem.status ?? 'ready') }}
          </span>
        </article>

        <article :class="sectionCardClass">
          <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">실행 전 체크</h4>
          <div class="grid gap-1">
            <div v-for="row in actionSummaryRows" :key="row.id" :class="metaRowClass">
              <span class="text-sm text-[color:var(--muted-text)]">{{ row.label }}</span>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                row.value
              }}</strong>
            </div>
          </div>
        </article>

        <article :class="sectionCardClass">
          <div class="grid gap-2">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">Primary Action</h4>
            <p class="text-sm leading-6 text-[color:var(--muted-text)]">
              현재 선택된 업무 기준으로 다음 실행 단계를 즉시 반영합니다.
            </p>
          </div>

          <button
            :class="[primaryButtonClass, 'w-full']"
            type="button"
            :disabled="actionDisabled()"
            @click="currentAction"
          >
            {{ actionLabel() }}
          </button>
        </article>
      </div>

      <div v-else :class="emptyStateClass">
        <strong class="text-sm font-semibold text-[color:var(--text-primary)]">실행 대기 중</strong>
        <p>업무 큐에서 항목을 선택하면 여기에서 관련 액션을 실행할 수 있습니다.</p>
      </div>
    </aside>
  </section>
</template>
