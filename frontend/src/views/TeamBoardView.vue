<script setup>
import { computed, onMounted, ref } from 'vue'
import { ListAllTasks } from '@/api/teamboard'

const STATUS_MAP = {
  BACKLOG: 'backlog',
  TODO: 'backlog',
  IN_PROGRESS: 'in_progress',
  REVIEW: 'review',
  BLOCKED: 'blocked',
  DONE: 'done',
}

const PRIORITY_MAP = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  CRITICAL: 'critical',
}

function formatDueDateLabel(iso) {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '-'
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const overdue = d < today
  return overdue ? `${mm}.${dd} 지연` : `${mm}.${dd}`
}

const statusColumns = [
  { id: 'backlog', label: '백로그', sub: 'Backlog' },
  { id: 'in_progress', label: '진행 중', sub: 'WIP' },
  { id: 'review', label: '검수/피드백', sub: 'Review' },
  { id: 'blocked', label: '보류/지연', sub: 'Blocked' },
  { id: 'done', label: '완료', sub: 'Done' },
]

const tasks = ref([])

const priorityLabels = {
  low: '낮음',
  medium: '보통',
  high: '높음',
  critical: '긴급',
}

const searchText = ref('')
const selectedPriority = ref('all')

// 팀보드(회사 업무) / 개인보드(미지정 = 개인 업무) 필터
const boardMode = ref('team')   // 'team' | 'personal'
const isPersonalTask = (t) => !t.companyName
const BOARD_MODES = [
  { key: 'team', label: '팀보드' },
  { key: 'personal', label: '개인보드' },
]
const modeTasks = computed(() =>
  tasks.value.filter((t) => (boardMode.value === 'personal' ? isPersonalTask(t) : !isPersonalTask(t))),
)

// tasks에서 고유 회사 목록을 동적으로 추출 (현재 모드 기준)
const companies = computed(() => {
  const seen = new Set()
  const result = []
  for (const t of modeTasks.value) {
    const name = t.companyName || '미지정'
    if (!seen.has(name)) {
      seen.add(name)
      result.push({ id: name, name })
    }
  }
  return result
})

const filteredTasks = computed(() => {
  const query = searchText.value.trim().toLowerCase()

  return modeTasks.value.filter((task) => {
    const matchesQuery =
      !query ||
      [task.title, task.part, task.milestone, task.companyName].some((value) =>
        String(value ?? '').toLowerCase().includes(query),
      )
    const matchesPriority = selectedPriority.value === 'all' || task.priority === selectedPriority.value

    return matchesQuery && matchesPriority
  })
})

// 요약 숫자는 선택한 보드(팀보드/개인보드) 기준
const boardMetrics = computed(() => ({
  active: modeTasks.value.filter((task) => task.status === 'in_progress').length,
  review: modeTasks.value.filter((task) => task.status === 'review').length,
  blocked: modeTasks.value.filter((task) => task.status === 'blocked').length,
}))

function getTasks(companyId, statusId) {
  return filteredTasks.value.filter(
    (task) => (task.companyName || '미지정') === companyId && task.status === statusId,
  )
}

async function loadTasksFromBackend() {
  try {
    const data = await ListAllTasks()
    if (!Array.isArray(data)) return

    tasks.value = data.map((task) => ({
      id: String(task.idx),
      status: STATUS_MAP[task.status] ?? 'backlog',
      title: task.name ?? '',
      part: task.taskPartName ?? '-',
      milestone: task.milestoneName ?? '',
      companyName: task.companyName ?? null,
      dueDate: formatDueDateLabel(task.dueDate),
      ownerInitial: task.assigneeName ? task.assigneeName.charAt(0) : '?',
      priority: PRIORITY_MAP[task.priority] ?? 'medium',
      // 상세 패널용 원본 정보
      assigneeName: task.assigneeName ?? null,
      dueRaw: task.dueDate ?? null,
    }))
  } catch (error) {
    console.error('팀보드 Task 로딩 실패:', error)
  }
}

onMounted(loadTasksFromBackend)

function getStatusTone(statusId) {
  if (statusId === 'done') return 'success'
  if (statusId === 'blocked') return 'warning'
  if (statusId === 'review') return 'primary'
  return 'info'
}

/* ───── 업무 상세 사이드 패널 ───── */
const selectedTask = ref(null)
function openTaskDetail(task) { selectedTask.value = task }
function closeTaskDetail() { selectedTask.value = null }
function statusLabelOf(id) {
  return statusColumns.find((c) => c.id === id)?.label ?? id
}
</script>

<template>
  <section class="team-board-page">
    <header class="team-board-hero">
      <div>
        <span class="eyebrow">Team Board</span>
        <h1>전체 협업 팀보드</h1>
        <p>본사와 모든 참여사의 업무 흐름을 한 화면에서 보고, 회사별 전용 보드로 바로 들어갑니다.</p>
      </div>
      <div class="team-board-summary" aria-label="전체 팀보드 요약">
        <div>
          <span>진행 중</span>
          <strong>{{ boardMetrics.active }}</strong>
        </div>
        <div>
          <span>검수/피드백</span>
          <strong>{{ boardMetrics.review }}</strong>
        </div>
        <div>
          <span>보류/지연</span>
          <strong>{{ boardMetrics.blocked }}</strong>
        </div>
      </div>
    </header>

    <div class="team-board-toolbar">
      <div class="board-mode-tabs" role="tablist" aria-label="보드 종류">
        <button
          v-for="m in BOARD_MODES"
          :key="m.key"
          type="button"
          role="tab"
          class="board-mode-tab"
          :class="{ 'is-on': boardMode === m.key }"
          :aria-selected="boardMode === m.key"
          @click="boardMode = m.key"
        >{{ m.label }}</button>
      </div>
      <input v-model.trim="searchText" type="search" placeholder="캠페인, 업무명, 파트를 검색..." />
      <select v-model="selectedPriority" aria-label="우선순위 필터">
        <option value="all">전체 우선순위</option>
        <option value="critical">긴급</option>
        <option value="high">높음</option>
        <option value="medium">보통</option>
        <option value="low">낮음</option>
      </select>
    </div>

    <div class="main-board" :style="{ '--board-column-count': statusColumns.length }">
      <!-- 헤더 행 -->
      <div class="main-board__head">
        <span class="main-board__company-label">참여사</span>
        <span v-for="column in statusColumns" :key="column.id">
          {{ column.label }} <small>{{ column.sub }}</small>
        </span>
      </div>

      <!-- 데이터가 없을 때 -->
      <div v-if="companies.length === 0" class="main-board__empty">
        등록된 업무가 없습니다.
      </div>

      <!-- 회사별 행 -->
      <div v-for="company in companies" :key="company.id" class="main-board__row">
        <aside class="company-cell">
          <strong>{{ company.name }}</strong>
        </aside>

        <div v-for="column in statusColumns" :key="column.id" class="board-column">
          <p v-if="getTasks(company.id, column.id).length === 0" class="board-column__empty">—</p>
          <article
            v-for="task in getTasks(company.id, column.id)"
            :key="task.id"
            class="board-task board-task--click"
            :class="[`board-task--${getStatusTone(column.id)}`, { 'board-task--urgent': task.priority === 'critical' }]"
            role="button"
            tabindex="0"
            @click="openTaskDetail({ ...task, columnId: column.id })"
            @keydown.enter="openTaskDetail({ ...task, columnId: column.id })"
          >
            <div class="board-task__top">
              <span>{{ task.part }}</span>
              <small>{{ priorityLabels[task.priority] }}</small>
            </div>
            <strong>{{ task.title }}</strong>
            <footer>
              <span :class="{ warning: task.dueDate.includes('지연') }">{{ task.dueDate }}</span>
              <em>{{ task.ownerInitial }}</em>
            </footer>
          </article>
        </div>
      </div>
    </div>

    <!-- 업무 상세 사이드 패널 -->
    <transition name="tb-drawer">
      <div v-if="selectedTask" class="tb-detail" role="dialog" aria-label="업무 상세">
        <div class="tb-detail__backdrop" @click="closeTaskDetail" />
        <aside class="tb-detail__panel">
          <header class="tb-detail__head">
            <span class="tb-detail__eyebrow">업무 상세</span>
            <button class="tb-detail__close" aria-label="닫기" @click="closeTaskDetail">✕</button>
          </header>
          <h2 class="tb-detail__title">{{ selectedTask.title }}</h2>
          <span
            class="tb-detail__prio"
            :class="`tb-detail__prio--${selectedTask.priority}`"
          >우선순위 · {{ priorityLabels[selectedTask.priority] }}</span>

          <dl class="tb-detail__list">
            <div><dt>상태</dt><dd>{{ statusLabelOf(selectedTask.status) }}</dd></div>
            <div><dt>회사</dt><dd>{{ selectedTask.companyName || '개인 업무' }}</dd></div>
            <div><dt>마일스톤</dt><dd>{{ selectedTask.milestone || '—' }}</dd></div>
            <div><dt>업무파트</dt><dd>{{ selectedTask.part || '—' }}</dd></div>
            <div><dt>담당</dt><dd>{{ selectedTask.assigneeName || '—' }}</dd></div>
            <div><dt>마감</dt><dd>{{ selectedTask.dueDate }}</dd></div>
          </dl>
        </aside>
      </div>
    </transition>
  </section>
</template>

<style scoped>
.team-board-page {
  display: grid;
  gap: 18px;
  padding: 24px;
}

/* ───── 팀보드 / 개인보드 탭 ───── */
.board-mode-tabs {
  display: inline-flex;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 3px;
  gap: 2px;
}
.board-mode-tab {
  padding: 7px 18px;
  border: 0;
  background: transparent;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  cursor: pointer;
  font-family: inherit;
  transition: background var(--transition-fast), color var(--transition-fast);
}
.board-mode-tab:hover { color: var(--text-primary); }
.board-mode-tab.is-on {
  background: var(--color-primary-500);
  color: #fff;
}

/* ───── 업무 카드 클릭 ───── */
.board-task--click { cursor: pointer; }
.board-task--click:hover { filter: brightness(0.985); transform: translateY(-1px); transition: transform .12s; }

/* ───── 업무 상세 사이드 패널 ───── */
.tb-detail { position: fixed; inset: 0; z-index: 200; }
.tb-detail__backdrop { position: absolute; inset: 0; background: rgba(15, 23, 42, 0.28); }
.tb-detail__panel {
  position: absolute;
  top: 0;
  right: 0;
  height: 100%;
  width: 360px;
  max-width: 92vw;
  background: var(--panel-color);
  border-left: 1px solid var(--border-color);
  box-shadow: -12px 0 40px rgba(15, 23, 42, 0.16);
  padding: 22px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  box-sizing: border-box;
}
.tb-detail__head { display: flex; align-items: center; justify-content: space-between; }
.tb-detail__eyebrow {
  font-size: 11px; font-weight: 800; letter-spacing: 0.06em; text-transform: uppercase;
  color: var(--color-primary-700, #6F5A9B);
}
.tb-detail__close {
  border: 0; background: transparent; font-size: 16px; cursor: pointer; color: var(--muted-text);
  width: 28px; height: 28px; border-radius: 8px;
}
.tb-detail__close:hover { background: var(--panel-muted); color: var(--text-primary); }
.tb-detail__title { font-size: 18px; font-weight: 800; margin: 0; color: var(--text-primary); }
.tb-detail__prio {
  align-self: flex-start;
  padding: 3px 10px; border-radius: 999px; font-size: 11px; font-weight: 800;
  background: var(--panel-muted); color: var(--text-secondary);
}
.tb-detail__prio--critical { background: #fde2e1; color: #c0392b; }
.tb-detail__prio--high     { background: #ffe7d6; color: #c05621; }
.tb-detail__prio--medium   { background: var(--badge-bg, #DDD2EE); color: var(--badge-text, #3F3463); }
.tb-detail__prio--low      { background: var(--panel-muted); color: var(--muted-text); }
.tb-detail__list { display: flex; flex-direction: column; gap: 0; margin: 6px 0 0; }
.tb-detail__list > div {
  display: grid; grid-template-columns: 84px 1fr; gap: 10px;
  padding: 11px 0; border-bottom: 1px solid var(--border-color);
}
.tb-detail__list dt { font-size: 12px; font-weight: 700; color: var(--muted-text); margin: 0; }
.tb-detail__list dd { font-size: 13px; color: var(--text-primary); margin: 0; word-break: break-word; }

.tb-drawer-enter-active, .tb-drawer-leave-active { transition: opacity .18s; }
.tb-drawer-enter-active .tb-detail__panel, .tb-drawer-leave-active .tb-detail__panel { transition: transform .2s ease; }
.tb-drawer-enter-from, .tb-drawer-leave-to { opacity: 0; }
.tb-drawer-enter-from .tb-detail__panel, .tb-drawer-leave-to .tb-detail__panel { transform: translateX(100%); }

.team-board-hero,
.team-board-toolbar,
.main-board {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.team-board-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
}

.eyebrow {
  color: var(--color-primary-700);
  font-size: 12px;
  font-weight: 850;
}

.team-board-hero h1 {
  margin-top: 6px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 850;
}

.team-board-hero p {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.team-board-summary {
  display: grid;
  grid-template-columns: repeat(3, 110px);
  gap: 10px;
}

.team-board-summary div {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  padding: 12px;
}

.team-board-summary span,
.board-task p,
.board-task footer small,
.main-board__head small {
  color: var(--muted-text);
  font-size: 12px;
}

.team-board-summary strong {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 26px;
  font-weight: 850;
}

.team-board-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px;
}

.team-board-toolbar input,
.team-board-toolbar select {
  min-height: 38px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--control-color);
  color: var(--text-primary);
  font-size: 13px;
  padding: 0 12px;
}

.team-board-toolbar input {
  width: min(360px, 100%);
}

.main-board {
  overflow-x: auto;
}

.main-board__head,
.main-board__row {
  display: grid;
  min-width: 1280px;
  grid-template-columns: 200px repeat(var(--board-column-count, 5), minmax(200px, 1fr));
}

.main-board__head {
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 850;
}

.main-board__head > span {
  padding: 14px;
}

.main-board__company-label {
  padding: 14px;
  border-right: 1px solid var(--border-color);
}

.main-board__row {
  border-top: 1px solid var(--border-color);
}

.main-board__empty {
  padding: 40px;
  text-align: center;
  color: var(--muted-text);
  font-size: 13px;
  border-top: 1px solid var(--border-color);
}

.company-cell {
  display: flex;
  align-items: flex-start;
  padding: 14px;
  border-right: 1px solid var(--border-color);
}

.company-cell strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
}

.board-column {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 120px;
  padding: 14px;
  border-right: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--panel-muted) 52%, var(--panel-color));
}

.board-column:last-child {
  border-right: 0;
}

.board-column__empty {
  color: var(--muted-text);
  font-size: 12px;
  text-align: center;
  padding: 12px 0;
}

.board-task {
  display: grid;
  gap: 8px;
  border: 1px solid var(--border-color);
  border-left: 4px solid var(--color-info);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 12px;
}

.board-task--primary {
  border-left-color: var(--color-primary-500);
}

.board-task--success {
  border-left-color: var(--color-success);
}

.board-task--warning,
.board-task--urgent {
  border-left-color: var(--color-warning);
}

.board-task__top,
.board-task footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.board-task__top span,
.board-task__top small {
  display: inline-flex;
  width: fit-content;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 800;
  padding: 2px 7px;
}

.board-task__top span {
  background: var(--panel-muted);
  color: var(--text-secondary);
}

.board-task__top small {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.board-task strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 850;
  line-height: 1.35;
}

.board-task__risk {
  color: var(--color-warning-dark);
  font-weight: 800;
}

.board-task footer {
  flex-wrap: wrap;
}

.board-task footer span {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 850;
}

.board-task footer span.warning {
  color: var(--color-danger);
}

.board-task footer em {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  font-size: 11px;
  font-style: normal;
  font-weight: 850;
}


@media (max-width: 900px) {
  .team-board-hero,
  .team-board-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .team-board-summary {
    grid-template-columns: repeat(3, 1fr);
  }

  .company-modal__board {
    grid-template-columns: repeat(5, minmax(180px, 1fr));
  }
}
</style>
