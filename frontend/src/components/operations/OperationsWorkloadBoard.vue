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

function actionCaption() {
  const item = store.selectedWorkItemDetail

  if (!item) return '현재 처리할 인박스 항목이 없습니다.'

  if (item.type === 'generator') {
    return store.activeRole === 'admin'
      ? '미리보기 배치를 planner 카드로 추가합니다.'
      : '사용자는 생성 결과를 검토만 할 수 있습니다.'
  }

  if (item.type === 'qa') {
    return store.activeRole === 'admin'
      ? 'QA 요청을 승인하고 캘린더 카드로 연결합니다.'
      : '현재 검토 내용을 초안으로 저장해 승인 대기열로 보냅니다.'
  }

  if (item.approvalsCompleted) {
    return store.activeRole === 'admin'
      ? '회의 내용을 후속 작업 카드로 연결합니다.'
      : '관리자 승인 후 카드 생성 단계로 넘어갑니다.'
  }

  return '참석자 전원 승인이 끝나야 생성할 수 있습니다.'
}

function actionDisabled() {
  const item = store.selectedWorkItemDetail

  if (!item) return true
  if (item.type === 'generator') return store.activeRole !== 'admin'
  if (item.type === 'qa') return false
  return store.activeRole !== 'admin' || !item.approvalsCompleted
}
</script>

<template>
  <section class="workload-board">
    <aside
      class="surface-card workload-panel workload-panel--sticky"
      :class="{ 'workload-panel--focus': store.focusTarget === 'workload-inbox' }"
    >
      <header class="workload-panel__head">
        <h3>업무 인박스</h3>

        <span class="workload-chip workload-chip--ghost">{{ store.workInboxItems.length }}건</span>
      </header>

      <div class="workload-panel__body workload-panel__body--compact">
        <div class="workload-list">
          <button
            v-for="item in store.workInboxItems"
            :key="`${item.type}-${item.id}`"
            type="button"
            class="workload-item"
            :class="{ 'workload-item--active': isWorkItemActive(item) }"
            @click="selectWorkItem(item)"
          >
            <div class="workload-item__copy">
              <small class="workload-item__kicker">{{ item.section }}</small>
              <strong>{{ item.title }}</strong>
              <p>{{ item.customer ?? item.subtitle }}</p>
            </div>

            <div class="workload-item__meta">
              <span :class="['workload-badge', `workload-badge--${workItemTone(item.status)}`]">
                {{ workItemLabel(item.status) }}
              </span>
              <small>{{ formatShortDate(item.dueDate) }}</small>
            </div>
          </button>

          <div v-if="!store.workInboxItems.length" class="workload-empty">
            현재 필터에 맞는 업무 인박스가 없습니다.
          </div>
        </div>
      </div>
    </aside>

    <article class="surface-card workload-panel">
      <header class="workload-panel__head">
        <h3>
          {{
            store.selectedWorkItemDetail?.type === 'qa'
              ? 'QA 요청'
              : store.selectedWorkItemDetail?.type === 'meeting'
                ? '내부 회의'
                : '기본 생성'
          }}
        </h3>
      </header>

      <div v-if="store.selectedWorkItemDetail?.type === 'qa'" class="workload-panel__body workload-stack">
        <section
          class="workload-section"
          :class="{ 'workload-section--focus': store.focusTarget === 'workload-meeting' }"
        >
          <div class="workload-section__head">
            <h4>카카오톡 요청 원문</h4>
            <span class="workload-chip workload-chip--ghost">{{ store.selectedWorkItemDetail.source }}</span>
          </div>
          <div class="workload-note">
            <p>{{ store.selectedWorkItemDetail.sourceMessage }}</p>
          </div>
        </section>

        <section
          class="workload-section"
          :class="{ 'workload-section--focus': store.focusTarget === 'workload-generator' }"
        >
          <div class="workload-section__head">
            <h4>AI 요약</h4>
            <span :class="['workload-badge', `workload-badge--${workItemTone(store.selectedWorkItemDetail.status)}`]">
              {{ workItemLabel(store.selectedWorkItemDetail.status) }}
            </span>
          </div>
          <div class="workload-note">
            <p>{{ store.selectedWorkItemDetail.aiSummary }}</p>
          </div>
        </section>

        <div class="workload-inline-grid">
          <article class="workload-inline-card">
            <small>수정 대상</small>
            <strong>{{ store.selectedWorkItemDetail.targetTaskLabel }}</strong>
            <p>{{ store.selectedWorkItemDetail.targetTaskId }}</p>
          </article>

          <article class="workload-inline-card">
            <small>추천 담당자</small>
            <strong>{{ store.selectedWorkItemDetail.recommendedAssignee?.name }}</strong>
            <p>{{ store.selectedWorkItemDetail.recommendedAssignee?.role }}</p>
          </article>
        </div>

        <section class="workload-section">
          <div class="workload-section__head">
            <h4>추출 요청사항</h4>
          </div>
          <ul class="workload-bullets">
            <li v-for="change in store.selectedWorkItemDetail.requestedChanges" :key="change">{{ change }}</li>
          </ul>
        </section>
      </div>

      <div v-else-if="store.selectedWorkItemDetail?.type === 'meeting'" class="workload-panel__body workload-stack">
        <div class="workload-inline-grid">
          <article class="workload-inline-card">
            <small>회의일</small>
            <strong>{{ formatLongDate(store.selectedWorkItemDetail.meetingDate) }}</strong>
            <p>{{ store.selectedWorkItemDetail.customer }}</p>
          </article>

          <article class="workload-inline-card">
            <small>진행 상태</small>
            <strong>{{ meetingProgress(store.selectedWorkItemDetail) }}</strong>
            <p>{{ store.selectedWorkItemDetail.approvalsCompleted ? '카드 생성 가능' : '승인 수집 중' }}</p>
          </article>
        </div>

        <section class="workload-section">
          <div class="workload-section__head">
            <h4>회의록 원문</h4>
          </div>
          <div class="workload-note">
            <p>{{ store.selectedWorkItemDetail.transcript }}</p>
          </div>
        </section>

        <section class="workload-section">
          <div class="workload-section__head">
            <h4>AI 정리 결과</h4>
          </div>
          <div class="workload-note">
            <p>{{ store.selectedWorkItemDetail.aiSummary }}</p>
          </div>
        </section>

        <section class="workload-section">
          <div class="workload-section__head">
            <h4>승인 파이프라인</h4>
          </div>
          <div class="workload-pipeline">
            <div class="workload-step workload-step--active">회의록 작성</div>
            <div class="workload-step workload-step--active">참석자 승인</div>
            <div class="workload-step" :class="{ 'workload-step--active': store.selectedWorkItemDetail.approvalsCompleted }">AI 정리 완료</div>
            <div class="workload-step" :class="{ 'workload-step--active': store.selectedWorkItemDetail.approvalsCompleted }">카드 추가 요청</div>
          </div>
        </section>

        <section class="workload-section">
          <div class="workload-section__head">
            <h4>참석자 승인</h4>
          </div>
          <div class="workload-attendees">
            <button
              v-for="attendee in store.selectedWorkItemDetail.attendees"
              :key="attendee.memberId"
              type="button"
              class="workload-attendee"
              :class="{ 'workload-attendee--active': attendee.approved }"
              @click="store.toggleMeetingApproval(store.selectedWorkItemDetail.id, attendee.memberId)"
            >
              <strong>{{ store.findMember(attendee.memberId)?.initials }}</strong>
              <span>{{ store.findMember(attendee.memberId)?.name }}</span>
            </button>
          </div>
        </section>
      </div>

      <div v-else class="workload-panel__body workload-stack">
        <section class="workload-section">
          <div class="workload-section__head">
            <h4>요일 분배 규칙</h4>
            <div class="workload-counter">
              <button class="icon-button" type="button" @click="store.setGeneratedBatchSize(store.generatedBatchSize - 1)">-</button>
              <strong>{{ store.generatedBatchSize }}</strong>
              <button class="icon-button" type="button" @click="store.setGeneratedBatchSize(store.generatedBatchSize + 1)">+</button>
            </div>
          </div>

          <div class="workload-rule">
            <strong>월 → 수 → 금 → 화 → 목</strong>
          </div>
        </section>

        <section
          class="workload-section"
          :class="{ 'workload-section--focus': store.focusTarget === 'workload-balance' }"
        >
          <div class="workload-section__head">
            <h4>생성 미리보기</h4>
          </div>
          <div class="workload-preview">
            <article
              v-for="item in store.generatorPreview.items"
              :key="item.previewId"
              class="workload-generator-card"
              :style="{ '--card-accent': item.palette.accent }"
            >
              <small>{{ item.customer }}</small>
              <strong>{{ item.title }}</strong>
              <p>{{ item.summary }}</p>
              <div class="workload-meta">
                <span>{{ item.weekdayLabel }}요일 배정</span>
                <small>{{ formatShortDate(item.dueDate) }}</small>
              </div>
            </article>
          </div>
        </section>

        <section class="workload-section">
          <div class="workload-section__head">
            <h4>주간 균형</h4>
            <span class="workload-chip workload-chip--ghost">편차 {{ store.generatorPreview.spread }}</span>
          </div>
          <div class="workload-balance">
            <div
              v-for="(count, index) in store.generatorPreview.counts"
              :key="index"
              class="workload-balance__item"
            >
              <small>{{ ['월', '화', '수', '목', '금'][index] }}</small>
              <div class="workload-balance__bar">
                <span :style="{ height: `${Math.max(18, count * 12)}px` }" />
              </div>
              <strong>{{ count }}</strong>
            </div>
          </div>
        </section>
      </div>
    </article>

    <aside class="surface-card workload-panel workload-panel--sticky">
      <header class="workload-panel__head">
        <h3>최종 액션</h3>

        <button
          v-if="store.selectedWorkItemDetail?.linkedTaskId"
          class="workload-link"
          type="button"
          @click="openPlannerTask(store.selectedWorkItemDetail.linkedTaskId)"
        >
          연결 카드 보기
        </button>
      </header>

      <div v-if="store.selectedWorkItemDetail" class="workload-panel__body workload-stack">
        <article class="workload-inline-card workload-inline-card--full">
          <small>현재 항목</small>
          <strong>
            {{
              store.selectedWorkItemDetail.type === 'generator'
                ? '기본 콘텐츠 생성'
                : store.selectedWorkItemDetail.title
            }}
          </strong>
          <p>
            {{
              store.selectedWorkItemDetail.type === 'generator'
                ? '자동 배분 규칙을 적용한 생성 배치'
                : store.selectedWorkItemDetail.customer?.name ??
                  store.selectedWorkItemDetail.customer ??
                  '내부 보드'
            }}
          </p>
        </article>

        <article class="workload-card">
          <div class="workload-card__head">
            <strong>관련 정보</strong>
          </div>
          <div class="workload-detail-list">
            <div class="workload-detail-row">
              <span>마감일</span>
              <strong>
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

            <div v-if="store.selectedWorkItemDetail.type === 'qa'" class="workload-detail-row">
              <span>추천 담당자</span>
              <strong>{{ store.selectedWorkItemDetail.recommendedAssignee?.name }}</strong>
            </div>

            <div v-if="store.selectedWorkItemDetail.type === 'meeting'" class="workload-detail-row">
              <span>승인 현황</span>
              <strong>{{ meetingProgress(store.selectedWorkItemDetail) }}</strong>
            </div>

            <div v-if="store.selectedWorkItemDetail.type === 'generator'" class="workload-detail-row">
              <span>생성 건수</span>
              <strong>{{ store.generatorPreview.items.length }}건</strong>
            </div>
          </div>
        </article>

        <article class="workload-card">
          <div class="workload-card__head">
            <strong>{{ actionLabel() }}</strong>
          </div>
          <button
            class="primary-button workload-panel__primary"
            type="button"
            :disabled="actionDisabled()"
            @click="currentAction"
          >
            {{ actionLabel() }}
          </button>
        </article>
      </div>

      <div v-else class="workload-empty">선택한 업무가 없습니다.</div>
    </aside>
  </section>
</template>

<style scoped>
.workload-board {
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(0, 1.2fr) minmax(280px, 320px);
  gap: 1rem;
  align-items: start;
}

.workload-panel {
  padding: 1rem;
  display: grid;
  gap: 0.9rem;
}

.workload-panel--sticky {
  position: sticky;
  top: 7.1rem;
}

.workload-panel--focus,
.workload-section--focus {
  border-color: color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 10%, transparent);
}

.workload-panel__head,
.workload-item,
.workload-item__meta,
.workload-card__head,
.workload-meta,
.workload-detail-row,
.workload-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.workload-panel__head {
  align-items: flex-start;
}

.workload-panel__body,
.workload-list,
.workload-stack,
.workload-section,
.workload-inline-grid,
.workload-preview,
.workload-card,
.workload-detail-list {
  display: grid;
  gap: 0.8rem;
}

.workload-panel__body--compact {
  gap: 0.75rem;
}

.workload-item,
.workload-inline-card,
.workload-generator-card,
.workload-card,
.workload-attendee,
.workload-empty {
  padding: 0.95rem;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background: color-mix(in srgb, var(--panel-muted) 74%, white);
}

.workload-item {
  text-align: left;
  align-items: flex-start;
}

.workload-item--active {
  border-color: color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
}

.workload-item__copy,
.workload-inline-card,
.workload-generator-card,
.workload-card,
.workload-note {
  display: grid;
  gap: 0.4rem;
}

.workload-item__copy p,
.workload-muted,
.workload-note p,
.workload-meta,
.workload-empty {
  color: var(--muted-text);
}

.workload-item__kicker {
  color: var(--accent-color);
  font-weight: 700;
}

.workload-chip,
.workload-badge {
  min-height: 2rem;
  padding: 0 0.8rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  font-weight: 700;
}

.workload-chip--ghost {
  background: var(--panel-muted);
  color: var(--muted-text);
}

.workload-badge--healthy {
  background: color-mix(in srgb, var(--success-color) 12%, white);
  color: #287b47;
  border-color: color-mix(in srgb, var(--success-color) 30%, var(--border-color));
}

.workload-badge--watch {
  background: color-mix(in srgb, var(--warning-color) 16%, white);
  color: #9a6c0d;
  border-color: color-mix(in srgb, var(--warning-color) 30%, var(--border-color));
}

.workload-badge--risk {
  background: color-mix(in srgb, var(--danger-color) 12%, white);
  color: #b2455f;
  border-color: color-mix(in srgb, var(--danger-color) 28%, var(--border-color));
}

.workload-rule,
.workload-note {
  padding: 0.95rem;
  border-radius: 18px;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
}

.workload-inline-grid,
.workload-preview {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.workload-counter {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
}

.workload-counter strong {
  min-width: 2rem;
  text-align: center;
}

.workload-bullets {
  display: grid;
  gap: 0.45rem;
  padding-left: 1.15rem;
  color: var(--text-secondary);
}

.workload-generator-card {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--card-accent) 10%, white), var(--panel-color)),
    var(--panel-color);
  border-color: color-mix(in srgb, var(--card-accent) 20%, var(--border-color));
}

.workload-balance {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.75rem;
}

.workload-balance__item {
  display: grid;
  justify-items: center;
  gap: 0.45rem;
}

.workload-balance__bar {
  width: 100%;
  min-height: 84px;
  padding: 0.6rem;
  border-radius: 16px;
  background: var(--panel-muted);
  display: flex;
  align-items: flex-end;
}

.workload-balance__bar span {
  width: 100%;
  border-radius: 12px;
  background: linear-gradient(180deg, var(--accent-color), var(--accent-strong));
}

.workload-pipeline,
.workload-attendees {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.workload-step {
  min-height: 2.75rem;
  padding: 0 0.85rem;
  border-radius: 16px;
  border: 1px dashed var(--border-color);
  display: grid;
  place-items: center;
  color: var(--muted-text);
  font-size: 0.82rem;
  font-weight: 700;
}

.workload-step--active {
  border-style: solid;
  border-color: color-mix(in srgb, var(--accent-color) 24%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
  color: var(--text-primary);
}

.workload-attendee {
  min-width: 8rem;
  display: grid;
  justify-items: center;
  gap: 0.25rem;
}

.workload-attendee--active {
  border-color: color-mix(in srgb, var(--success-color) 30%, var(--border-color));
  background: color-mix(in srgb, var(--success-color) 12%, white);
}

.workload-link {
  color: var(--accent-color);
  font-weight: 700;
}

.workload-panel__primary {
  width: 100%;
}

@media (max-width: 1380px) {
  .workload-board {
    grid-template-columns: 1fr;
  }

  .workload-panel--sticky {
    position: static;
  }
}

@media (max-width: 980px) {
  .workload-panel__head,
  .workload-item,
  .workload-item__meta,
  .workload-card__head,
  .workload-meta,
  .workload-detail-row,
  .workload-section__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .workload-inline-grid,
  .workload-preview,
  .workload-balance {
    grid-template-columns: 1fr;
  }
}
</style>
