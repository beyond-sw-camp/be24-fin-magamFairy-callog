<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useOperationsStore } from '@/stores/operations'
import { usePlannerStore } from '@/stores/planner'
import { formatShortDate } from '@/utils/calendar'

const store = useOperationsStore()
const planner = usePlannerStore()
const router = useRouter()

const selectedTeamTaskMember = computed(() =>
  store.findMember(store.selectedTeamTask?.assigneeId ?? null),
)

function openPlannerTask(taskId) {
  if (!taskId) {
    return
  }

  planner.openTask(taskId)
  router.push('/calendar')
}
</script>

<template>
  <section class="tasknotes-board">
    <div class="tasknotes-toolbar">
      <h3>{{ store.tasknoteMode === 'personal' ? '개인' : '팀' }}</h3>

      <div class="tasknotes-toolbar__controls">
        <div class="tasknotes-segmented" role="tablist" aria-label="업무일지 보기 전환">
          <button
            type="button"
            class="tasknotes-segmented__button"
            :class="{ 'tasknotes-segmented__button--active': store.tasknoteMode === 'personal' }"
            @click="store.setTasknoteMode('personal')"
          >
            개인
          </button>
          <button
            type="button"
            class="tasknotes-segmented__button"
            :class="{ 'tasknotes-segmented__button--active': store.tasknoteMode === 'team' }"
            @click="store.setTasknoteMode('team')"
          >
            팀
          </button>
        </div>

        <div v-if="store.tasknoteMode === 'personal'" class="tasknotes-filter-group">
          <button
            type="button"
            class="tasknotes-filter-chip"
            :class="{ 'tasknotes-filter-chip--active': store.tasknoteListFilter === 'all' }"
            @click="store.setTasknoteListFilter('all')"
          >
            전체
          </button>
          <button
            type="button"
            class="tasknotes-filter-chip"
            :class="{ 'tasknotes-filter-chip--active': store.tasknoteListFilter === 'completed' }"
            @click="store.setTasknoteListFilter('completed')"
          >
            완료
          </button>
        </div>
      </div>
    </div>

    <div v-if="store.tasknoteMode === 'personal'" class="tasknotes-grid">
      <article
        class="surface-card tasknotes-panel"
        :class="{ 'tasknotes-panel--focus': store.focusTarget === 'tasknotes-list' }"
      >
        <header class="tasknotes-panel__head">
          <h4>내 업무 카드</h4>
        </header>

        <div class="tasknotes-panel__body tasknotes-stack">
          <article
            v-for="task in store.filteredPersonalTaskNotes"
            :key="task.id"
            class="tasknotes-card"
            :style="{ '--task-accent': task.palette?.accent ?? '#2f80ed' }"
          >
            <div class="tasknotes-card__head">
              <div>
                <strong>{{ task.title }}</strong>
                <p>{{ task.customer }} · {{ task.contentType }}</p>
              </div>

              <div class="tasknotes-chip-group">
                <span :class="['tasknotes-badge', `tasknotes-badge--${task.isMarkedComplete ? 'healthy' : 'neutral'}`]">
                  {{ task.isMarkedComplete ? '완료 표시' : planner.statusLabels[task.status] }}
                </span>
                <span class="tasknotes-chip">{{ planner.priorityLabels[task.priority] }}</span>
              </div>
            </div>

            <div class="tasknotes-meta">
              <span>{{ formatShortDate(task.dueDate) }}</span>
              <small>{{ task.timeRange }}</small>
            </div>

            <div class="tasknotes-card__footer">
              <button class="soft-button" type="button" @click="openPlannerTask(task.id)">카드 열기</button>

              <template v-if="store.activeRole !== 'admin'">
                <button class="ghost-button" type="button" @click="store.requestNextStatus(task.id)">
                  다음 단계 요청
                </button>
                <button class="primary-button" type="button" @click="store.toggleTaskCompletion(task.id)">
                  {{ task.isMarkedComplete ? '완료 취소' : '완료 표시' }}
                </button>
              </template>
            </div>
          </article>

          <div v-if="!store.filteredPersonalTaskNotes.length" class="tasknotes-empty">
            표시할 업무가 없습니다.
          </div>
        </div>
      </article>

      <aside
        class="surface-card tasknotes-panel tasknotes-panel--compact"
        :class="{ 'tasknotes-panel--focus': store.focusTarget === 'tasknotes-requests' }"
      >
        <header class="tasknotes-panel__head">
          <h4>요청 큐</h4>

          <button
            v-if="store.activeRole === 'admin'"
            class="primary-button"
            type="button"
            :disabled="!store.pendingStatusRequests[0]"
            @click="store.applyStatusRequest(store.pendingStatusRequests[0]?.id)"
          >
            요청 반영
          </button>
        </header>

        <div class="tasknotes-panel__body tasknotes-stack">
          <article
            v-for="request in store.pendingStatusRequests"
            :key="request.id"
            class="tasknotes-request"
            :class="{ 'tasknotes-request--highlight': store.pendingStatusRequests[0]?.id === request.id }"
          >
            <div class="tasknotes-request__head">
              <div>
                <strong>{{ request.task?.title ?? '연결 작업 없음' }}</strong>
                <p>{{ request.createdAt }}</p>
              </div>
              <span class="tasknotes-chip">{{ planner.statusLabels[request.nextStatus] }}</span>
            </div>
          </article>

          <div v-if="!store.pendingStatusRequests.length" class="tasknotes-empty">
            대기 중인 요청이 없습니다.
          </div>
        </div>
      </aside>
    </div>

    <div v-else class="tasknotes-grid tasknotes-grid--team">
      <article
        class="surface-card tasknotes-panel"
        :class="{ 'tasknotes-panel--focus': store.focusTarget === 'tasknotes-team' }"
      >
        <header class="tasknotes-panel__head">
          <h4>팀 일정 보드</h4>
        </header>

        <div class="tasknotes-panel__body tasknotes-team-grid">
          <article
            v-for="group in store.teamTaskGroups"
            :key="group.id"
            class="tasknotes-member-card"
          >
            <header class="tasknotes-member-card__head">
              <div class="tasknotes-member-card__identity">
                <span class="tasknotes-avatar" :style="{ background: group.accent }">{{ group.initials }}</span>
                <div>
                  <strong>{{ group.name }}</strong>
                  <p>{{ group.role }}</p>
                </div>
              </div>
              <span class="tasknotes-chip">{{ group.tasks.length }}건</span>
            </header>

            <div class="tasknotes-team-list">
              <button
                v-for="task in group.tasks"
                :key="task.id"
                type="button"
                class="tasknotes-team-task"
                :class="{ 'tasknotes-team-task--active': store.selectedTeamTask?.id === task.id }"
                @click="store.setSelectedTeamTask(task.id)"
              >
                <strong>{{ task.title }}</strong>
                <small>{{ formatShortDate(task.dueDate) }} · {{ planner.statusLabels[task.status] }}</small>
              </button>
            </div>
          </article>
        </div>
      </article>

      <aside class="surface-card tasknotes-panel tasknotes-panel--compact">
        <header class="tasknotes-panel__head">
          <h4>{{ store.selectedTeamTask?.title ?? '일정 조정' }}</h4>

          <button
            v-if="store.selectedTeamTask"
            class="soft-button"
            type="button"
            @click="openPlannerTask(store.selectedTeamTask.id)"
          >
            캘린더에서 보기
          </button>
        </header>

        <div v-if="store.selectedTeamTask" class="tasknotes-panel__body tasknotes-stack">
          <article class="tasknotes-inline-card">
            <small>담당자</small>
            <strong>{{ selectedTeamTaskMember?.name }}</strong>
            <p>{{ selectedTeamTaskMember?.role }}</p>
          </article>

          <div class="tasknotes-detail-list">
            <div class="tasknotes-detail-row">
              <span>시작일</span>
              <strong>{{ formatShortDate(store.selectedTeamTask.startDate) }}</strong>
            </div>
            <div class="tasknotes-detail-row">
              <span>마감일</span>
              <strong>{{ formatShortDate(store.selectedTeamTask.dueDate) }}</strong>
            </div>
            <div class="tasknotes-detail-row">
              <span>상태</span>
              <strong>{{ planner.statusLabels[store.selectedTeamTask.status] }}</strong>
            </div>
          </div>

          <div v-if="store.activeRole === 'admin'" class="tasknotes-request">
            <div class="tasknotes-card__footer tasknotes-card__footer--stretch">
              <button class="ghost-button" type="button" @click="store.shiftTeamTask(store.selectedTeamTask.id, -1)">
                하루 앞당기기
              </button>
              <button class="ghost-button" type="button" @click="store.shiftTeamTask(store.selectedTeamTask.id, 1)">
                하루 미루기
              </button>
            </div>

            <div class="tasknotes-chip-group">
              <button
                v-for="member in store.members"
                :key="member.id"
                type="button"
                class="tasknotes-chip tasknotes-chip--button"
                :class="{ 'tasknotes-chip--active': store.selectedTeamTask.assigneeId === member.id }"
                @click="store.reassignTeamTask(store.selectedTeamTask.id, member.id)"
              >
                {{ member.name }}
              </button>
            </div>
          </div>

          <div v-else class="tasknotes-empty">
            일정 조정은 관리자 전용입니다.
          </div>
        </div>

        <div v-else class="tasknotes-empty">
          팀 업무를 선택해 주세요.
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.tasknotes-board,
.tasknotes-grid,
.tasknotes-stack,
.tasknotes-panel__body,
.tasknotes-team-grid,
.tasknotes-member-card,
.tasknotes-inline-card,
.tasknotes-request,
.tasknotes-detail-list,
.tasknotes-team-list {
  display: grid;
  gap: 0.85rem;
}

.tasknotes-toolbar,
.tasknotes-panel__head,
.tasknotes-card__head,
.tasknotes-card__footer,
.tasknotes-meta,
.tasknotes-member-card__head,
.tasknotes-member-card__identity,
.tasknotes-request__head,
.tasknotes-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.tasknotes-toolbar {
  align-items: center;
  justify-content: space-between;
}

.tasknotes-toolbar h3,
.tasknotes-panel__head h4 {
  font-size: 1rem;
  line-height: 1.2;
}

.tasknotes-toolbar__controls {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.tasknotes-segmented {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-muted);
}

.tasknotes-segmented__button,
.tasknotes-filter-chip {
  min-height: 2rem;
  padding: 0 0.9rem;
  border-radius: 999px;
  font-weight: 700;
  color: var(--muted-text);
}

.tasknotes-segmented__button--active,
.tasknotes-filter-chip--active {
  background: var(--panel-color);
  color: var(--text-primary);
  box-shadow: var(--shadow-soft);
}

.tasknotes-filter-group {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-muted);
}

.tasknotes-panel {
  padding: 0.95rem;
  display: grid;
  gap: 0.75rem;
}

.tasknotes-panel--compact {
  gap: 0.65rem;
}

.tasknotes-panel--focus {
  border-color: color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 10%, transparent);
}

.tasknotes-grid {
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 340px);
}

.tasknotes-grid--team {
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 340px);
}

.tasknotes-card,
.tasknotes-request,
.tasknotes-member-card,
.tasknotes-inline-card,
.tasknotes-empty,
.tasknotes-team-task {
  padding: 0.9rem;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: color-mix(in srgb, var(--panel-muted) 74%, white);
}

.tasknotes-card {
  display: grid;
  gap: 0.45rem;
  border-color: color-mix(in srgb, var(--task-accent) 22%, var(--border-color));
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--task-accent) 8%, white), var(--panel-color)),
    var(--panel-color);
}

.tasknotes-request--highlight {
  border-color: color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
}

.tasknotes-member-card__identity {
  justify-content: flex-start;
}

.tasknotes-avatar {
  width: 2.35rem;
  height: 2.35rem;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
}

.tasknotes-team-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.tasknotes-team-task {
  width: 100%;
  text-align: left;
  display: grid;
  gap: 0.2rem;
}

.tasknotes-team-task--active {
  border-color: color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
}

.tasknotes-chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.tasknotes-chip,
.tasknotes-badge {
  min-height: 1.95rem;
  padding: 0 0.8rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  font-weight: 700;
}

.tasknotes-chip {
  background: var(--panel-muted);
  color: var(--muted-text);
}

.tasknotes-chip--button {
  background: var(--panel-color);
  color: var(--text-primary);
}

.tasknotes-chip--active {
  border-color: color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
}

.tasknotes-badge--healthy {
  background: color-mix(in srgb, var(--success-color) 12%, white);
  color: #287b47;
  border-color: color-mix(in srgb, var(--success-color) 30%, var(--border-color));
}

.tasknotes-badge--neutral {
  background: var(--panel-muted);
  color: var(--muted-text);
}

.tasknotes-meta,
.tasknotes-request__head p,
.tasknotes-empty {
  color: var(--muted-text);
}

.tasknotes-card__footer {
  justify-content: flex-end;
}

.tasknotes-card__footer--stretch {
  justify-content: stretch;
}

.tasknotes-detail-row strong {
  color: var(--text-primary);
}

@media (max-width: 1380px) {
  .tasknotes-grid,
  .tasknotes-grid--team {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .tasknotes-toolbar,
  .tasknotes-panel__head,
  .tasknotes-card__head,
  .tasknotes-card__footer,
  .tasknotes-meta,
  .tasknotes-member-card__head,
  .tasknotes-member-card__identity,
  .tasknotes-request__head,
  .tasknotes-detail-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .tasknotes-team-grid {
    grid-template-columns: 1fr;
  }
}
</style>
