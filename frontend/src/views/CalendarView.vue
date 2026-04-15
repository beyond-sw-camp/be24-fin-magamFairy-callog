<script setup>
import { computed } from 'vue'
import CalendarBoard from '@/components/calendar/CalendarBoard.vue'
import GanttChart from '@/components/calendar/GanttChart.vue'
import TableBoard from '@/components/calendar/TableBoard.vue'
import PageTabs from '@/components/common/PageTabs.vue'
import { usePlannerStore } from '@/stores/planner'
import { formatLongDate, formatMonthLabel, formatShortDate, todayKey } from '@/utils/calendar'

const store = usePlannerStore()

const tabs = [
  {
    value: 'calendar',
    label: '캘린더',
    icon: 'M7 2v3M17 2v3M3 8h18M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z',
  },
  {
    value: 'table',
    label: '메인 테이블',
    icon: 'M3 3h8v8H3V3Zm10 0h8v5h-8V3ZM13 10h8v11h-8V10ZM3 13h8v8H3v-8Z',
  },
  {
    value: 'gantt',
    label: '간트차트',
    icon: 'M4 18h4V9H4v9Zm6 0h4V5h-4v13Zm6 0h4v-7h-4v7Z',
  },
]

const statusPalette = {
  done: '#59c36d',
  in_progress: '#5b8def',
  review: '#f5b64e',
  at_risk: '#df5f75',
  planned: '#9aa7bd',
}

const filterLabel = computed(() => {
  if (store.statusFilter === 'all') {
    return '전체'
  }

  return store.statusLabels[store.statusFilter] ?? store.statusFilter
})

const sortLabel = computed(() => {
  const map = {
    due: '마감일',
    priority: '우선순위',
    assignee: '담당자',
  }

  return map[store.sortMode] ?? store.sortMode
})

const fieldLabel = computed(() => (store.spanMode ? '타임라인' : '기본 보기'))
const toolbarDateLabel = computed(() =>
  store.calendarView === 'month'
    ? formatMonthLabel(store.currentDate)
    : formatLongDate(store.currentDate),
)
const showTodayButton = computed(() => store.currentDate !== todayKey())

const statusSummary = computed(() => {
  const order = ['done', 'in_progress', 'review', 'at_risk', 'planned']
  const total = store.filteredTasks.length || 1

  return order.map((key) => {
    const count = store.filteredTasks.filter((task) => task.status === key).length

    return {
      key,
      label: store.statusLabels[key] ?? key,
      count,
      percentage: Math.round((count / total) * 100),
      color: statusPalette[key],
    }
  })
})

const completionRate = computed(() => {
  if (!store.filteredTasks.length) {
    return 0
  }

  const done = store.filteredTasks.filter((task) => task.status === 'done').length
  return Math.round((done / store.filteredTasks.length) * 100)
})

const donutStyle = computed(() => {
  let currentAngle = 0
  const total = store.filteredTasks.length || 1
  const stops = statusSummary.value
    .filter((segment) => segment.count > 0)
    .map((segment) => {
      const start = currentAngle
      currentAngle += (segment.count / total) * 360
      return `${segment.color} ${start}deg ${currentAngle}deg`
    })

  return {
    background: `conic-gradient(${stops.length ? stops.join(', ') : '#dbe3ef 0deg 360deg'})`,
  }
})

const upcomingTasks = computed(() => {
  return [...store.filteredTasks]
    .filter((task) => task.status !== 'done')
    .sort((left, right) => left.dueDate.localeCompare(right.dueDate))
    .slice(0, 4)
})

function moveTask(payload) {
  store.moveTask(payload.taskId, payload.dateKey)
}
</script>

<template>
  <section class="board-page">
    <div class="board-page__hero surface-card">
      <div class="board-page__intro">
        <div class="board-page__title-row">
          <h2>캘린더</h2>

          <button class="board-page__title-icon" type="button" aria-label="보드 정보">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 16v-4m0-4h.01M22 12A10 10 0 1 1 2 12a10 10 0 0 1 20 0Z" />
            </svg>
          </button>

          <button class="board-page__title-icon" type="button" aria-label="보드 즐겨찾기">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M11.48 3.5a.6.6 0 0 1 1.04 0l2.55 5.16 5.69.83a.58.58 0 0 1 .32.99l-4.12 4.01.97 5.66a.58.58 0 0 1-.84.61L12 18.06l-5.09 2.7a.58.58 0 0 1-.84-.61l.97-5.66-4.12-4.01a.58.58 0 0 1 .32-.99l5.69-.83 2.55-5.16Z"
              />
            </svg>
          </button>
        </div>

        <p class="board-page__description">보드 설명을 추가하세요</p>
      </div>

      <div class="board-page__topline">
        <PageTabs :active="store.calendarTab" :tabs="tabs" @select="store.setCalendarTab($event)" />

        <div class="board-page__mode-switch">
          <button
            type="button"
            :class="{ 'board-page__mode-switch-item--active': store.activeMode === 'personal' }"
            @click="store.setActiveMode('personal')"
          >
            개인
          </button>
          <button
            type="button"
            :class="{ 'board-page__mode-switch-item--active': store.activeMode === 'team' }"
            @click="store.setActiveMode('team')"
          >
            팀
          </button>
        </div>
      </div>

      <div class="board-page__controls">
        <div class="board-page__controls-left">
          <div v-if="store.calendarTab !== 'table'" class="board-page__date-nav">
            <button
              class="board-page__arrow"
              type="button"
              aria-label="이전 기간"
              @click="store.shiftPeriod(-1)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M15 18l-6-6 6-6" />
              </svg>
            </button>

            <strong>{{ toolbarDateLabel }}</strong>

            <button
              class="board-page__arrow"
              type="button"
              aria-label="다음 기간"
              @click="store.shiftPeriod(1)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M9 18l6-6-6-6" />
              </svg>
            </button>
          </div>

          <div v-if="store.calendarTab !== 'table'" class="board-page__view-switch">
            <button
              type="button"
              :class="{ 'board-page__view-switch-item--active': store.calendarView === 'week' }"
              @click="store.setCalendarView('week')"
            >
              주간
            </button>
            <button
              type="button"
              :class="{ 'board-page__view-switch-item--active': store.calendarView === 'month' }"
              @click="store.setCalendarView('month')"
            >
              월간
            </button>
          </div>

          <button
            v-if="showTodayButton && store.calendarTab !== 'table'"
            class="board-page__today-button"
            type="button"
            @click="store.setToday()"
          >
            오늘
          </button>
        </div>

        <div class="board-page__controls-right">
          <button class="board-page__tool-chip" type="button" @click="store.cycleStatusFilter">
            필터
            <strong>{{ filterLabel }}</strong>
          </button>

          <button class="board-page__tool-chip" type="button" @click="store.cycleSortMode">
            정렬
            <strong>{{ sortLabel }}</strong>
          </button>

          <button
            v-if="store.calendarTab === 'calendar'"
            class="board-page__tool-chip"
            type="button"
            @click="store.toggleSpanMode"
          >
            필드
            <strong>{{ fieldLabel }}</strong>
          </button>
        </div>
      </div>
    </div>

    <div
      class="board-page__content"
      :class="{ 'board-page__content--single': store.calendarTab !== 'calendar' }"
    >
      <div class="board-page__canvas surface-card">
        <CalendarBoard
          v-if="store.calendarTab === 'calendar'"
          :tasks="store.filteredTasks"
          :current-date="store.currentDate"
          :view-mode="store.calendarView"
          :span-mode="store.spanMode"
          @create-task="store.openCreateModal($event)"
          @move-task="moveTask"
          @open-task="store.openTask($event)"
        />

        <TableBoard
          v-else-if="store.calendarTab === 'table'"
          :tasks="store.filteredTasks"
          :current-date="store.currentDate"
          @create-task="store.openCreateModal($event)"
          @open-task="store.openTask($event)"
        />

        <GanttChart
          v-else
          :tasks="store.filteredTasks"
          :current-date="store.currentDate"
          :view-mode="store.calendarView"
          @move-task="moveTask"
          @open-task="store.openTask($event)"
        />
      </div>

      <aside v-if="store.calendarTab === 'calendar'" class="board-page__widgets">
        <article class="widget-card">
          <div class="widget-card__head">
            <h3>콘텐츠 상태</h3>
          </div>

          <div class="widget-card__stack">
            <div
              v-for="segment in statusSummary"
              :key="segment.key"
              class="widget-card__segment"
              :style="{
                flexBasis: `${Math.max(segment.percentage, 8)}%`,
                background: segment.color,
              }"
            />
          </div>

          <div class="widget-card__legend">
            <div
              v-for="segment in statusSummary"
              :key="`${segment.key}-legend`"
              class="widget-card__legend-item"
            >
              <span :style="{ backgroundColor: segment.color }" />
              <strong>{{ segment.label }}</strong>
              <small>{{ segment.count }}</small>
            </div>
          </div>
        </article>

        <article class="widget-card">
          <div class="widget-card__head">
            <h3>진행 현황</h3>
            <span>{{ completionRate }}% 완료</span>
          </div>

          <div class="widget-card__donut-shell">
            <div class="widget-card__donut" :style="donutStyle">
              <div class="widget-card__donut-inner">
                <strong>{{ completionRate }}%</strong>
                <small>완료</small>
              </div>
            </div>
          </div>
        </article>

        <article class="widget-card">
          <div class="widget-card__head">
            <h3>다음 일정</h3>
          </div>

          <div class="widget-card__list">
            <button
              v-for="task in upcomingTasks"
              :key="task.id"
              class="widget-card__list-item"
              type="button"
              @click="store.openTask(task.id)"
            >
              <div>
                <strong>{{ task.title }}</strong>
                <small>{{ task.customer }}</small>
              </div>
              <span>{{ formatShortDate(task.dueDate) }}</span>
            </button>
          </div>
        </article>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.board-page {
  display: grid;
  gap: 1rem;
}

.board-page__hero {
  padding: 1.35rem 1.45rem 1rem;
}

.board-page__title-row,
.board-page__topline,
.board-page__controls,
.board-page__controls-left,
.board-page__controls-right,
.board-page__mode-switch,
.widget-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.board-page__intro {
  margin-bottom: 0.7rem;
}

.board-page__title-row {
  justify-content: flex-start;
  gap: 0.45rem;
  margin-bottom: 0.5rem;
}

.board-page__title-row h2 {
  font-size: 1.9rem;
  line-height: 1.05;
}

.board-page__title-icon {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: var(--muted-text);
  cursor: pointer;
}

.board-page__title-icon svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.board-page__title-icon:last-of-type svg path {
  fill: none;
}

.board-page__description {
  color: var(--muted-text);
  font-size: 0.95rem;
}

.board-page__topline {
  margin-bottom: 0.9rem;
}

.board-page__mode-switch {
  padding: 0.2rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-muted);
  gap: 0.2rem;
}

.board-page__mode-switch button {
  min-width: 76px;
  min-height: 2.2rem;
  border-radius: 999px;
  color: var(--muted-text);
  cursor: pointer;
}

.board-page__mode-switch-item--active {
  background: var(--panel-color);
  color: var(--text-primary) !important;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
}

.board-page__controls {
  flex-wrap: wrap;
}

.board-page__controls-left,
.board-page__controls-right {
  flex-wrap: wrap;
}

.board-page__date-nav,
.board-page__view-switch {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.board-page__date-nav {
  padding: 0.15rem 0.2rem;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-subtle);
}

.board-page__date-nav strong {
  min-width: 118px;
  text-align: center;
  font-size: 0.98rem;
}

.board-page__arrow {
  width: 2rem;
  height: 2rem;
  border-radius: 10px;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.board-page__arrow svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.board-page__view-switch {
  padding: 0.2rem;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-muted);
}

.board-page__view-switch button {
  min-width: 74px;
  min-height: 2.2rem;
  border-radius: 10px;
  color: var(--muted-text);
  cursor: pointer;
}

.board-page__view-switch-item--active {
  background: var(--panel-color);
  color: var(--text-primary) !important;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.08);
}

.board-page__today-button {
  color: #ff5a5f;
  font-weight: 700;
  cursor: pointer;
}

.board-page__tool-chip {
  min-height: 2.45rem;
  padding: 0 0.85rem;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-muted);
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  cursor: pointer;
  color: var(--muted-text);
}

.board-page__tool-chip strong {
  color: var(--text-primary);
  font-size: 0.82rem;
}

.board-page__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 1rem;
  align-items: start;
}

.board-page__content--single {
  grid-template-columns: 1fr;
}

.board-page__canvas {
  min-width: 0;
  padding: 0.8rem;
  overflow-x: auto;
}

.board-page__widgets {
  display: grid;
  gap: 1rem;
  width: 340px;
  min-width: 340px;
}

.widget-card {
  border: 1px solid var(--border-color);
  border-radius: 22px;
  background: var(--panel-color);
  padding: 1rem;
  box-shadow: var(--shadow-soft);
}

.widget-card h3 {
  font-size: 1.05rem;
}

.widget-card__head span {
  color: var(--muted-text);
}

.widget-card__stack {
  height: 90px;
  border-radius: 18px;
  border: 1px solid var(--border-color);
  overflow: hidden;
  display: flex;
  background: var(--panel-muted);
  margin-top: 0.85rem;
}

.widget-card__segment {
  min-width: 12%;
}

.widget-card__legend {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem;
  margin-top: 0.9rem;
}

.widget-card__legend-item {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.82rem;
}

.widget-card__legend-item span {
  width: 0.7rem;
  height: 0.7rem;
  border-radius: 999px;
  flex-shrink: 0;
}

.widget-card__legend-item small {
  color: var(--muted-text);
  margin-left: auto;
}

.widget-card__donut-shell {
  display: grid;
  place-items: center;
  padding: 1rem 0;
}

.widget-card__donut {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  display: grid;
  place-items: center;
}

.widget-card__donut-inner {
  width: 112px;
  height: 112px;
  border-radius: 50%;
  background: var(--panel-color);
  display: grid;
  place-items: center;
  box-shadow: inset 0 0 0 1px var(--border-color);
  text-align: center;
}

.widget-card__donut-inner strong {
  font-size: 1.7rem;
  line-height: 1;
}

.widget-card__donut-inner small {
  color: var(--muted-text);
}

.widget-card__list {
  display: grid;
  gap: 0.65rem;
  margin-top: 0.8rem;
}

.widget-card__list-item {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: var(--panel-muted);
  padding: 0.85rem 0.9rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  cursor: pointer;
  text-align: left;
}

.widget-card__list-item small,
.widget-card__list-item span {
  color: var(--muted-text);
}

@media (max-width: 1260px) {
  .board-page__content {
    grid-template-columns: 1fr;
  }

  .board-page__widgets {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: auto;
    min-width: 0;
  }
}

@media (max-width: 980px) {
  .board-page__topline,
  .board-page__controls {
    flex-direction: column;
    align-items: flex-start;
  }

  .board-page__widgets {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .board-page__hero {
    padding-inline: 1rem;
  }

  .board-page__date-nav strong {
    min-width: auto;
  }
}
</style>
