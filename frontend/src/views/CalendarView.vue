<script setup>
import { computed } from 'vue'
import CalendarBoard from '@/components/calendar/CalendarBoard.vue'
import GanttChart from '@/components/calendar/GanttChart.vue'
import TableBoard from '@/components/calendar/TableBoard.vue'
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
</script>

<template>
  <section class="grid gap-4 p-4 xl:grid-cols-[minmax(0,1fr)_360px]">
    <div class="min-w-0 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div class="overflow-x-auto min-w-0">
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
    </div>

    <aside class="grid gap-4 xl:sticky xl:top-4">
      <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex flex-wrap items-center gap-2 rounded-full border border-slate-200 bg-slate-50 p-1 w-fit">
            <button
              v-for="mode in scopeModes"
              :key="mode.value"
              type="button"
              class="rounded-full px-4 py-1.5 text-sm font-medium transition-all"
              :class="
                store.activeMode === mode.value
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-500 hover:text-slate-700'
              "
              @click="store.setActiveMode(mode.value)"
            >
              {{ mode.label }}
            </button>
          </div>
        </div>

        <div
          v-if="store.calendarTab !== 'table'"
          class="mt-4 inline-flex w-full items-center justify-between gap-2 rounded-xl border border-slate-200 bg-slate-50 p-1"
        >
          <button
            class="rounded-lg p-1.5 transition-all hover:bg-white hover:shadow-sm"
            @click="store.shiftPeriod(-1)"
            aria-label="이전"
            type="button"
          >
            <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 18l-6-6 6-6" />
            </svg>
          </button>

          <strong class="min-w-[120px] text-center text-sm font-semibold text-slate-900">
            {{ toolbarDateLabel }}
          </strong>

          <button
            class="rounded-lg p-1.5 transition-all hover:bg-white hover:shadow-sm"
            @click="store.shiftPeriod(1)"
            aria-label="다음"
            type="button"
          >
            <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 18l6-6-6-6" />
            </svg>
          </button>
        </div>

        <div
          v-if="store.calendarTab !== 'table'"
          class="mt-3 inline-flex w-full items-center gap-1 rounded-xl border border-slate-200 bg-slate-50 p-1"
        >
          <button
            v-for="mode in calendarModes"
            :key="mode.value"
            type="button"
            class="flex-1 rounded-lg px-4 py-1.5 text-sm font-medium transition-all"
            :class="
              store.calendarView === mode.value
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            "
            @click="store.setCalendarView(mode.value)"
          >
            {{ mode.label }}
          </button>
        </div>

        <button
          v-if="showTodayButton && store.calendarTab !== 'table'"
          type="button"
          class="mt-3 rounded-full px-2 text-sm font-bold text-rose-500 transition hover:text-rose-600"
          @click="store.setToday()"
        >
          오늘
        </button>

        <div class="mt-4 flex flex-wrap items-center gap-2">
          <button
            v-for="tool in boardTools"
            :key="tool.label"
            type="button"
            class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100 px-3 py-2 text-sm text-slate-500 transition-colors hover:bg-slate-200"
            @click="tool.action()"
          >
            {{ tool.label }}
            <strong class="font-semibold text-slate-900">{{ tool.value }}</strong>
          </button>
        </div>
      </section>
    </aside>
  </section>
</template>
