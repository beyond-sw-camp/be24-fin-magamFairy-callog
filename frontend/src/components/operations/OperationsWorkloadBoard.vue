<script setup>
import { useRouter } from 'vue-router'
import { useOperationsStore } from '@/stores/operations'
import { usePlannerStore } from '@/stores/planner'
import { formatLongDate, formatShortDate } from '@/utils/calendar'

const store = useOperationsStore()
const planner = usePlannerStore()
const router = useRouter()

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

const panelClass =
  'grid gap-[0.9rem] rounded-[24px] border border-[color:var(--border-color)] bg-[var(--panel-color)] px-4 py-4 shadow-[var(--shadow-soft)]'
const headerClass =
  'flex items-start justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start'
const itemCardClass =
  'flex items-start justify-between gap-[0.75rem] rounded-[20px] border border-[color:var(--border-color)] px-[0.95rem] py-[0.95rem] text-left transition duration-200 hover:-translate-y-px hover:shadow-[var(--shadow-soft)] max-[980px]:flex-col max-[980px]:items-start'
const infoCardClass =
  'grid gap-[0.4rem] rounded-[20px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_74%,white)] px-[0.95rem] py-[0.95rem]'
const mutedPanelClass =
  'grid gap-[0.8rem] rounded-[18px] border border-[color:var(--border-color)] bg-[var(--panel-color)] px-[0.95rem] py-[0.95rem]'
const emptyCardClass =
  'rounded-[20px] border border-[color:var(--border-color)] bg-[var(--panel-muted)] px-4 py-4 text-sm text-[color:var(--muted-text)]'
const badgeBaseClass =
  'inline-flex min-h-8 items-center justify-center rounded-full border border-[color:var(--border-color)] px-3 text-[0.78rem] font-bold'
const chipClass =
  'inline-flex min-h-8 items-center justify-center rounded-full border border-[color:var(--border-color)] bg-[var(--panel-muted)] px-3 text-[0.78rem] font-bold text-[color:var(--muted-text)]'
const primaryButtonClass =
  'inline-flex min-h-[2.6rem] items-center justify-center gap-2 rounded-xl border border-[color:var(--accent-color)] bg-[var(--accent-color)] px-4 text-sm font-semibold text-white transition duration-200 hover:-translate-y-px hover:shadow-[var(--shadow-soft)] disabled:cursor-not-allowed disabled:opacity-45 disabled:hover:translate-y-0'
const softButtonClass =
  'inline-flex min-h-[2.45rem] items-center justify-center gap-2 rounded-xl border border-[color:var(--border-color)] bg-[var(--panel-muted)] px-4 text-sm font-semibold text-[color:var(--text-primary)] transition duration-200 hover:-translate-y-px hover:shadow-[var(--shadow-soft)]'
const iconButtonClass =
  'inline-flex h-9 w-9 items-center justify-center rounded-xl border border-[color:var(--border-color)] bg-[var(--panel-muted)] text-base font-semibold text-[color:var(--text-primary)] transition duration-200 hover:-translate-y-px hover:shadow-[var(--shadow-soft)]'
const linkButtonClass =
  'text-sm font-semibold text-[color:var(--accent-color)] transition duration-200 hover:opacity-80'
const focusStyle = {
  borderColor: 'color-mix(in srgb, var(--accent-color) 30%, var(--border-color))',
  boxShadow: '0 0 0 3px color-mix(in srgb, var(--accent-color) 10%, transparent)',
}
const activeItemStyle = {
  borderColor: 'color-mix(in srgb, var(--accent-color) 30%, var(--border-color))',
  background: 'color-mix(in srgb, var(--accent-color) 10%, var(--panel-color))',
}

function workItemTone(status) {
  return workItemStateTone[status] ?? 'neutral'
}

function workItemLabel(status) {
  return workItemStateLabel[status] ?? status
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

function meetingProgress(note) {
  return `${note.approvalCount}/${note.attendees.length} 승인`
}

function openPlannerTask(taskId) {
  if (!taskId) {
    return
  }

  planner.openTask(taskId)
  router.push('/calendar')
}

function currentAction() {
  const item = store.selectedWorkItemDetail

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
  const item = store.selectedWorkItemDetail

  if (!item) return '대기 업무 없음'

  if (item.type === 'generator') {
    return store.activeRole === 'admin' ? '캘린더에 기본 패널 생성' : '기본 생성은 관리자 전용'
  }

  if (item.type === 'qa') {
    return store.activeRole === 'admin' ? '승인 후 카드 생성' : '요청 초안 저장'
  }

  return store.activeRole === 'admin' ? '카드 추가 요청 생성' : '승인 진행 중'
}

function actionDisabled() {
  const item = store.selectedWorkItemDetail

  if (!item) return true
  if (item.type === 'generator') return store.activeRole !== 'admin'
  if (item.type === 'qa') return false
  return store.activeRole !== 'admin' || !item.approvalsCompleted
}

function badgeStyle(tone) {
  if (tone === 'healthy') {
    return {
      background: 'color-mix(in srgb, var(--success-color) 12%, white)',
      borderColor: 'color-mix(in srgb, var(--success-color) 30%, var(--border-color))',
      color: '#287b47',
    }
  }

  if (tone === 'watch') {
    return {
      background: 'color-mix(in srgb, var(--warning-color) 16%, white)',
      borderColor: 'color-mix(in srgb, var(--warning-color) 30%, var(--border-color))',
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
    color: 'var(--muted-text)',
  }
}

function generatorCardStyle(item) {
  return {
    background: `linear-gradient(180deg, color-mix(in srgb, ${item.palette.accent} 10%, white), var(--panel-color)), var(--panel-color)`,
    borderColor: `color-mix(in srgb, ${item.palette.accent} 20%, var(--border-color))`,
  }
}

function pipelineStepStyle(active) {
  if (!active) return null

  return {
    borderStyle: 'solid',
    borderColor: 'color-mix(in srgb, var(--accent-color) 24%, var(--border-color))',
    background: 'color-mix(in srgb, var(--accent-color) 10%, var(--panel-color))',
    color: 'var(--text-primary)',
  }
}

function attendeeCardStyle(active) {
  if (!active) return null

  return {
    borderColor: 'color-mix(in srgb, var(--success-color) 30%, var(--border-color))',
    background: 'color-mix(in srgb, var(--success-color) 12%, white)',
  }
}
</script>

<template>
  <section class="grid items-start gap-4 [grid-template-columns:minmax(280px,320px)_minmax(0,1.2fr)_minmax(280px,320px)] max-[1380px]:grid-cols-1">
    <aside
      :class="[panelClass, 'sticky top-[7.1rem] max-[1380px]:static']"
      :style="store.focusTarget === 'workload-inbox' ? focusStyle : null"
    >
      <header :class="headerClass">
        <h3 class="text-base font-semibold text-[color:var(--text-primary)]">업무 인박스</h3>

        <span :class="chipClass">{{ store.workInboxItems.length }}건</span>
      </header>

      <div class="grid gap-[0.75rem]">
        <div class="grid gap-[0.8rem]">
          <button
            v-for="item in store.workInboxItems"
            :key="`${item.type}-${item.id}`"
            type="button"
            :class="itemCardClass"
            :style="isWorkItemActive(item) ? activeItemStyle : { background: 'color-mix(in srgb, var(--panel-muted) 74%, white)' }"
            @click="selectWorkItem(item)"
          >
            <div class="grid gap-1">
              <small class="font-bold text-[color:var(--accent-color)]">{{ item.section }}</small>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ item.title }}</strong>
              <p class="text-sm text-[color:var(--muted-text)]">{{ item.customer ?? item.subtitle }}</p>
            </div>

            <div class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start">
              <span :class="badgeBaseClass" :style="badgeStyle(workItemTone(item.status))">
                {{ workItemLabel(item.status) }}
              </span>
              <small class="text-xs text-[color:var(--muted-text)]">{{ formatShortDate(item.dueDate) }}</small>
            </div>
          </button>

          <div v-if="!store.workInboxItems.length" :class="emptyCardClass">
            현재 필터에 맞는 업무 인박스가 없습니다.
          </div>
        </div>
      </div>
    </aside>

    <article :class="panelClass">
      <header :class="headerClass">
        <h3 class="text-base font-semibold text-[color:var(--text-primary)]">
          {{
            store.selectedWorkItemDetail?.type === 'qa'
              ? 'QA 요청'
              : store.selectedWorkItemDetail?.type === 'meeting'
                ? '내부 회의'
                : '기본 생성'
          }}
        </h3>
      </header>

      <div v-if="store.selectedWorkItemDetail?.type === 'qa'" class="grid gap-[0.8rem]">
        <section :class="[mutedPanelClass]" :style="store.focusTarget === 'workload-meeting' ? focusStyle : null">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">카카오톡 요청 원문</h4>
            <span :class="chipClass">{{ store.selectedWorkItemDetail.source }}</span>
          </div>
          <div class="rounded-[18px] border border-[color:var(--border-color)] bg-[var(--panel-color)] px-4 py-4">
            <p class="text-sm leading-6 text-[color:var(--text-secondary)]">{{ store.selectedWorkItemDetail.sourceMessage }}</p>
          </div>
        </section>

        <section :class="[mutedPanelClass]" :style="store.focusTarget === 'workload-generator' ? focusStyle : null">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">AI 요약</h4>
            <span :class="badgeBaseClass" :style="badgeStyle(workItemTone(store.selectedWorkItemDetail.status))">
              {{ workItemLabel(store.selectedWorkItemDetail.status) }}
            </span>
          </div>
          <div class="rounded-[18px] border border-[color:var(--border-color)] bg-[var(--panel-color)] px-4 py-4">
            <p class="text-sm leading-6 text-[color:var(--text-secondary)]">{{ store.selectedWorkItemDetail.aiSummary }}</p>
          </div>
        </section>

        <div class="grid grid-cols-2 gap-3 max-[980px]:grid-cols-1">
          <article :class="infoCardClass">
            <small class="text-xs text-[color:var(--muted-text)]">수정 대상</small>
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ store.selectedWorkItemDetail.targetTaskLabel }}</strong>
            <p class="text-sm text-[color:var(--muted-text)]">{{ store.selectedWorkItemDetail.targetTaskId }}</p>
          </article>

          <article :class="infoCardClass">
            <small class="text-xs text-[color:var(--muted-text)]">추천 담당자</small>
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ store.selectedWorkItemDetail.recommendedAssignee?.name }}</strong>
            <p class="text-sm text-[color:var(--muted-text)]">{{ store.selectedWorkItemDetail.recommendedAssignee?.role }}</p>
          </article>
        </div>

        <section :class="mutedPanelClass">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">추출 요청사항</h4>
          </div>
          <ul class="grid gap-2 pl-5 text-sm text-[color:var(--text-secondary)]">
            <li v-for="change in store.selectedWorkItemDetail.requestedChanges" :key="change" class="list-disc">
              {{ change }}
            </li>
          </ul>
        </section>
      </div>

      <div v-else-if="store.selectedWorkItemDetail?.type === 'meeting'" class="grid gap-[0.8rem]">
        <div class="grid grid-cols-2 gap-3 max-[980px]:grid-cols-1">
          <article :class="infoCardClass">
            <small class="text-xs text-[color:var(--muted-text)]">회의일</small>
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ formatLongDate(store.selectedWorkItemDetail.meetingDate) }}</strong>
            <p class="text-sm text-[color:var(--muted-text)]">{{ store.selectedWorkItemDetail.customer }}</p>
          </article>

          <article :class="infoCardClass">
            <small class="text-xs text-[color:var(--muted-text)]">진행 상태</small>
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ meetingProgress(store.selectedWorkItemDetail) }}</strong>
            <p class="text-sm text-[color:var(--muted-text)]">{{ store.selectedWorkItemDetail.approvalsCompleted ? '카드 생성 가능' : '승인 수집 중' }}</p>
          </article>
        </div>

        <section :class="mutedPanelClass">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">회의록 원문</h4>
          </div>
          <div class="rounded-[18px] border border-[color:var(--border-color)] bg-[var(--panel-color)] px-4 py-4">
            <p class="text-sm leading-6 text-[color:var(--text-secondary)]">{{ store.selectedWorkItemDetail.transcript }}</p>
          </div>
        </section>

        <section :class="mutedPanelClass">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">AI 정리 결과</h4>
          </div>
          <div class="rounded-[18px] border border-[color:var(--border-color)] bg-[var(--panel-color)] px-4 py-4">
            <p class="text-sm leading-6 text-[color:var(--text-secondary)]">{{ store.selectedWorkItemDetail.aiSummary }}</p>
          </div>
        </section>

        <section :class="mutedPanelClass">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">승인 파이프라인</h4>
          </div>
          <div class="flex flex-wrap gap-2">
            <div
              class="grid min-h-11 place-items-center rounded-2xl border border-dashed border-[color:var(--border-color)] px-4 text-[0.82rem] font-bold text-[color:var(--muted-text)]"
              :style="pipelineStepStyle(true)"
            >
              회의록 작성
            </div>
            <div
              class="grid min-h-11 place-items-center rounded-2xl border border-dashed border-[color:var(--border-color)] px-4 text-[0.82rem] font-bold text-[color:var(--muted-text)]"
              :style="pipelineStepStyle(true)"
            >
              참석자 승인
            </div>
            <div
              class="grid min-h-11 place-items-center rounded-2xl border border-dashed border-[color:var(--border-color)] px-4 text-[0.82rem] font-bold text-[color:var(--muted-text)]"
              :style="pipelineStepStyle(store.selectedWorkItemDetail.approvalsCompleted)"
            >
              AI 정리 완료
            </div>
            <div
              class="grid min-h-11 place-items-center rounded-2xl border border-dashed border-[color:var(--border-color)] px-4 text-[0.82rem] font-bold text-[color:var(--muted-text)]"
              :style="pipelineStepStyle(store.selectedWorkItemDetail.approvalsCompleted)"
            >
              카드 추가 요청
            </div>
          </div>
        </section>

        <section :class="mutedPanelClass">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">참석자 승인</h4>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="attendee in store.selectedWorkItemDetail.attendees"
              :key="attendee.memberId"
              type="button"
              class="grid min-w-32 justify-items-center gap-1 rounded-[20px] border border-[color:var(--border-color)] bg-white/70 px-4 py-4 text-center transition duration-200 hover:-translate-y-px hover:shadow-[var(--shadow-soft)]"
              :style="attendeeCardStyle(attendee.approved)"
              @click="store.toggleMeetingApproval(store.selectedWorkItemDetail.id, attendee.memberId)"
            >
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ store.findMember(attendee.memberId)?.initials }}</strong>
              <span class="text-sm text-[color:var(--text-secondary)]">{{ store.findMember(attendee.memberId)?.name }}</span>
            </button>
          </div>
        </section>
      </div>

      <div v-else class="grid gap-[0.8rem]">
        <section :class="mutedPanelClass">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">요일 분배 규칙</h4>
            <div class="inline-flex items-center gap-2">
              <button :class="iconButtonClass" type="button" @click="store.setGeneratedBatchSize(store.generatedBatchSize - 1)">-</button>
              <strong class="min-w-8 text-center text-sm font-semibold text-[color:var(--text-primary)]">{{ store.generatedBatchSize }}</strong>
              <button :class="iconButtonClass" type="button" @click="store.setGeneratedBatchSize(store.generatedBatchSize + 1)">+</button>
            </div>
          </div>

          <div class="rounded-[18px] border border-[color:var(--border-color)] bg-[var(--panel-color)] px-4 py-4">
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">월 → 수 → 금 → 화 → 목</strong>
          </div>
        </section>

        <section :class="mutedPanelClass" :style="store.focusTarget === 'workload-balance' ? focusStyle : null">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">생성 미리보기</h4>
          </div>
          <div class="grid grid-cols-2 gap-3 max-[980px]:grid-cols-1">
            <article
              v-for="item in store.generatorPreview.items"
              :key="item.previewId"
              class="grid gap-2 rounded-[20px] border px-4 py-4"
              :style="generatorCardStyle(item)"
            >
              <small class="text-xs text-[color:var(--muted-text)]">{{ item.customer }}</small>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ item.title }}</strong>
              <p class="text-sm text-[color:var(--muted-text)]">{{ item.summary }}</p>
              <div class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start">
                <span class="text-sm text-[color:var(--muted-text)]">{{ item.weekdayLabel }}요일 배정</span>
                <small class="text-xs text-[color:var(--muted-text)]">{{ formatShortDate(item.dueDate) }}</small>
              </div>
            </article>
          </div>
        </section>

        <section :class="mutedPanelClass">
          <div :class="headerClass">
            <h4 class="text-sm font-semibold text-[color:var(--text-primary)]">주간 균형</h4>
            <span :class="chipClass">편차 {{ store.generatorPreview.spread }}</span>
          </div>
          <div class="grid grid-cols-5 gap-3 max-[980px]:grid-cols-1">
            <div
              v-for="(count, index) in store.generatorPreview.counts"
              :key="index"
              class="grid justify-items-center gap-2"
            >
              <small class="text-xs text-[color:var(--muted-text)]">{{ ['월', '화', '수', '목', '금'][index] }}</small>
              <div class="flex min-h-[84px] w-full items-end rounded-2xl bg-[var(--panel-muted)] p-2.5">
                <span
                  class="w-full rounded-xl bg-[linear-gradient(180deg,var(--accent-color),var(--accent-strong))]"
                  :style="{ height: `${Math.max(18, count * 12)}px` }"
                />
              </div>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ count }}</strong>
            </div>
          </div>
        </section>
      </div>
    </article>

    <aside :class="[panelClass, 'sticky top-[7.1rem] max-[1380px]:static']">
      <header :class="headerClass">
        <h3 class="text-base font-semibold text-[color:var(--text-primary)]">최종 액션</h3>

        <button
          v-if="store.selectedWorkItemDetail?.linkedTaskId"
          :class="linkButtonClass"
          type="button"
          @click="openPlannerTask(store.selectedWorkItemDetail.linkedTaskId)"
        >
          연결 카드 보기
        </button>
      </header>

      <div v-if="store.selectedWorkItemDetail" class="grid gap-[0.8rem]">
        <article :class="infoCardClass">
          <small class="text-xs text-[color:var(--muted-text)]">현재 항목</small>
          <strong class="text-sm font-semibold text-[color:var(--text-primary)]">
            {{
              store.selectedWorkItemDetail.type === 'generator'
                ? '기본 콘텐츠 생성'
                : store.selectedWorkItemDetail.title
            }}
          </strong>
          <p class="text-sm text-[color:var(--muted-text)]">
            {{
              store.selectedWorkItemDetail.type === 'generator'
                ? '자동 배분 규칙을 적용한 생성 배치'
                : store.selectedWorkItemDetail.customer?.name ??
                  store.selectedWorkItemDetail.customer ??
                  '내부 보드'
            }}
          </p>
        </article>

        <article :class="mutedPanelClass">
          <div :class="headerClass">
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">관련 정보</strong>
          </div>
          <div class="grid gap-3">
            <div :class="headerClass">
              <span class="text-sm text-[color:var(--muted-text)]">마감일</span>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">
                {{
                  formatShortDate(
                    store.selectedWorkItemDetail.type === 'meeting'
                      ? store.selectedWorkItemDetail.meetingDate
                      : store.selectedWorkItemDetail.type === 'generator'
                        ? store.generatorPreview.items[0]?.dueDate
                        : store.selectedWorkItemDetail.dueDate,
                  )
                }}
              </strong>
            </div>

            <div v-if="store.selectedWorkItemDetail.type === 'qa'" :class="headerClass">
              <span class="text-sm text-[color:var(--muted-text)]">추천 담당자</span>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ store.selectedWorkItemDetail.recommendedAssignee?.name }}</strong>
            </div>

            <div v-if="store.selectedWorkItemDetail.type === 'meeting'" :class="headerClass">
              <span class="text-sm text-[color:var(--muted-text)]">승인 현황</span>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ meetingProgress(store.selectedWorkItemDetail) }}</strong>
            </div>

            <div v-if="store.selectedWorkItemDetail.type === 'generator'" :class="headerClass">
              <span class="text-sm text-[color:var(--muted-text)]">생성 건수</span>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ store.generatorPreview.items.length }}건</strong>
            </div>
          </div>
        </article>

        <article :class="mutedPanelClass">
          <div :class="headerClass">
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ actionLabel() }}</strong>
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

      <div v-else :class="emptyCardClass">선택한 업무가 없습니다.</div>
    </aside>
  </section>
</template>
