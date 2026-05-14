<script setup>
import { computed, onMounted } from 'vue'
import CalendarBoard from '@/components/calendar/CalendarBoard.vue'
import GanttChart from '@/components/calendar/GanttChart.vue'
import TableBoard from '@/components/calendar/TableBoard.vue'
import editorApi from '@/api/editor/editorApi'
import { usePlannerStore } from '@/stores/planner'
import { formatLongDate, formatMonthLabel, todayKey } from '@/utils/calendar'

const store = usePlannerStore()

const filterLabel = computed(() => {
  if (store.statusFilter === 'all') return '전체'
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

const boardTools = computed(() =>
  [
    {
      label: '필터',
      value: filterLabel.value,
      action: () => store.cycleStatusFilter(),
      show: true,
    },
    {
      label: '정렬',
      value: sortLabel.value,
      action: () => store.cycleSortMode(),
      show: true,
    },
    {
      label: '필드',
      value: fieldLabel.value,
      action: () => store.toggleSpanMode(),
      show: store.calendarTab === 'calendar',
    },
  ].filter((tool) => tool.show !== false),
)

const calendarModes = [
  { value: 'week', label: '주간' },
  { value: 'month', label: '월간' },
]

const scopeModes = [
  { value: 'personal', label: '개인' },
  { value: 'team', label: '팀' },
]

function moveTask(payload) {
  store.moveTask(payload.taskId, payload.dateKey)
}

function extractCalendarTasks(payload) {
  if (Array.isArray(payload)) {
    return payload
  }

  if (!payload || typeof payload !== 'object') {
    return null
  }

  const candidates = [
    payload.tasks,
    payload.items,
    payload.list,
    payload.data,
    payload.result?.body,
    payload.result?.data,
  ]

  return candidates.find((item) => Array.isArray(item)) ?? null
}

async function loadCalendarTasks() {
  try {
    const response = await editorApi.listContent()
    const nextTasks = extractCalendarTasks(response)

    if (nextTasks) {
      store.tasks = [...nextTasks]
    }
  } catch (error) {
    console.warn('listContent failed', error)
  }
}

onMounted(() => {
  void loadCalendarTasks()
})
</script>

<template>
  <section class="cal-shell" data-cycle="lavender-pop">
    <header class="cal-shell__topbar">
      <div class="cal-shell__title-wrap">
        <h2 class="cal-shell__title">캘린더</h2>
        <p class="cal-shell__sub">{{ toolbarDateLabel }} · {{ store.calendarView === 'month' ? '월간' : '주간' }} 뷰</p>
      </div>
    </header>

    <div class="cal-shell__body">
      <main class="cal-shell__main">
        <div class="cal-shell__board-scroll">
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
      </main>

      <aside class="cal-shell__side" aria-label="사이드바">
        <div class="lp-panel">
          <div class="lp-panel-h">
            <h3>스코프</h3>
          </div>
          <div class="lp-seg">
            <button
              v-for="mode in scopeModes"
              :key="mode.value"
              type="button"
              class="lp-seg__btn"
              :class="{ 'is-on': store.activeMode === mode.value }"
              @click="store.setActiveMode(mode.value)"
            >
              {{ mode.label }}
            </button>
          </div>
        </div>

        <div class="lp-panel" v-if="store.calendarTab !== 'table'">
          <div class="lp-panel-h">
            <h3>기간 이동</h3>
            <button
              v-if="showTodayButton"
              type="button"
              class="lp-today-btn"
              @click="store.setToday()"
            >오늘</button>
          </div>
          <div class="lp-range">
            <button class="lp-arrow" @click="store.shiftPeriod(-1)" aria-label="이전">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </button>
            <strong class="lp-range__label">{{ toolbarDateLabel }}</strong>
            <button class="lp-arrow" @click="store.shiftPeriod(1)" aria-label="다음">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>
          <div class="lp-seg lp-seg--full">
            <button
              v-for="mode in calendarModes"
              :key="mode.value"
              type="button"
              class="lp-seg__btn"
              :class="{ 'is-on': store.calendarView === mode.value }"
              @click="store.setCalendarView(mode.value)"
            >
              {{ mode.label }}
            </button>
          </div>
        </div>

        <div class="lp-panel" v-if="boardTools.length">
          <div class="lp-panel-h">
            <h3>도구</h3>
          </div>
          <div class="lp-tools">
            <button
              v-for="tool in boardTools"
              :key="tool.label"
              type="button"
              class="lp-tool"
              @click="tool.action()"
            >
              <span class="lp-tool__lbl">{{ tool.label }}</span>
              <strong class="lp-tool__val">{{ tool.value }}</strong>
            </button>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.cal-shell {
  --lp-bg: #F5F1FA;
  --lp-surface: #FFFFFF;
  --lp-surface-soft: #EEE6F7;
  --lp-primary: #B79BD9;
  --lp-primary-strong: #6F5A9B;
  --lp-primary-deep: #3F3463;
  --lp-violet-deep: #2D2649;
  --lp-lime: #D8EB75;
  --lp-card-lavender-1: #DDD2EE;
  --lp-card-cream: #F5EDD8;
  --lp-text: #2A2440;
  --lp-text-muted: #6B6582;
  --lp-text-faint: #9991AE;
  --lp-border: #E5DDF0;

  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 20px 24px 32px;
  background: var(--lp-bg);
  color: var(--lp-text);
  font-family: 'Pretendard Variable', 'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
  font-feature-settings: 'tnum' 1, 'ss01' 1;
  min-height: 100%;
  box-sizing: border-box;
}

.cal-shell__topbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
}
.cal-shell__title { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.02em; }
.cal-shell__sub   { margin: 4px 0 0; font-size: 12px; color: var(--lp-text-muted); }

.cal-shell__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}
@media (max-width: 1024px) { .cal-shell__body { grid-template-columns: minmax(0, 1fr); } }

.cal-shell__main {
  background: var(--lp-surface);
  border-radius: 22px;
  padding: 22px 24px;
  box-shadow: 0 1px 2px rgba(63,52,99,.04), 0 6px 18px rgba(63,52,99,.06);
  min-width: 0;
}
.cal-shell__board-scroll { overflow-x: auto; min-width: 0; }

.cal-shell__side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 16px;
}
@media (max-width: 1024px) { .cal-shell__side { position: static; } }

.lp-panel {
  background: var(--lp-surface);
  border-radius: 18px;
  padding: 16px 18px;
  box-shadow: 0 1px 2px rgba(63,52,99,.04), 0 6px 18px rgba(63,52,99,.06);
}
.lp-panel-h {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.lp-panel-h h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--lp-text-muted);
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.lp-today-btn {
  border: 0;
  background: var(--lp-primary-deep);
  color: #fff;
  padding: 6px 14px;
  font-size: 11.5px;
  font-weight: 600;
  border-radius: 999px;
  cursor: pointer;
  transition: background .15s ease;
}
.lp-today-btn:hover { background: #4F4275; }

.lp-seg {
  display: inline-flex;
  background: var(--lp-surface-soft);
  border-radius: 999px;
  padding: 3px;
  gap: 2px;
  width: fit-content;
}
.lp-seg--full {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 100%;
  margin-top: 8px;
}
.lp-seg__btn {
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  color: var(--lp-text-muted);
  background: transparent;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  transition: background .15s ease, color .15s ease;
}
.lp-seg__btn:hover { color: var(--lp-text); }
.lp-seg__btn.is-on {
  background: var(--lp-surface);
  color: var(--lp-primary-deep);
  box-shadow: 0 1px 3px rgba(63, 52, 99, 0.10);
}

.lp-range {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: var(--lp-surface-soft);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 8px;
}
.lp-arrow {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: var(--lp-surface);
  color: var(--lp-primary-deep);
  border: 0;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background .15s ease;
}
.lp-arrow:hover { background: var(--lp-card-lavender-1); }
.lp-range__label {
  flex: 1;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--lp-text);
  letter-spacing: -0.01em;
}

.lp-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.lp-tool {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--lp-surface-soft);
  border: 0;
  border-radius: 12px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--lp-text-muted);
  cursor: pointer;
  transition: background .15s ease;
}
.lp-tool:hover { background: var(--lp-card-lavender-1); }
.lp-tool__lbl { font-weight: 500; }
.lp-tool__val { font-weight: 700; color: var(--lp-text); }
</style>
