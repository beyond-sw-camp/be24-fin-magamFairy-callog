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
  if (store.statusFilter === 'all') return '전체'
  return store.statusLabels[store.statusFilter] ?? store.statusFilter
})

const sortLabel = computed(() => {
  const map = { due: '마감일', priority: '우선순위', assignee: '담당자' }
  return map[store.sortMode] ?? store.sortMode
})

const fieldLabel = computed(() => (store.spanMode ? '타임라인' : '기본 보기'))
const toolbarDateLabel = computed(() =>
  store.calendarView === 'month' ? formatMonthLabel(store.currentDate) : formatLongDate(store.currentDate),
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
  if (!store.filteredTasks.length) return 0
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
  <section class="grid gap-4 p-4">
    <div class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
      <div class="mb-4">
        <div class="flex items-center gap-2 mb-2">
          <h2 class="text-3xl font-bold tracking-tight text-slate-900">캘린더</h2>
          <button class="p-1.5 rounded-full text-slate-400 hover:bg-slate-100 transition-colors">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 16v-4m0-4h.01M22 12A10 10 0 1 1 2 12a10 10 0 0 1 20 0Z" />
            </svg>
          </button>
          <button class="p-1.5 rounded-full text-slate-400 hover:bg-slate-100 transition-colors">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.48 3.5a.6.6 0 0 1 1.04 0l2.55 5.16 5.69.83a.58.58 0 0 1 .32.99l-4.12 4.01.97 5.66a.58.58 0 0 1-.84.61L12 18.06l-5.09 2.7a.58.58 0 0 1-.84-.61l.97-5.66-4.12-4.01a.58.58 0 0 1 .32-.99l5.69-.83 2.55-5.16Z" />
            </svg>
          </button>
        </div>
        <p class="text-slate-500 text-sm">보드 설명을 추가하세요</p>
      </div>

      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <PageTabs :active="store.calendarTab" :tabs="tabs" @select="store.setCalendarTab($event)" />
        
        <div class="flex p-1 bg-slate-100 rounded-full border border-slate-200 gap-1 w-fit">
          <button
            v-for="mode in ['personal', 'team']"
            :key="mode"
            type="button"
            class="px-5 py-1.5 rounded-full text-sm font-medium transition-all"
            :class="store.activeMode === mode ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'"
            @click="store.setActiveMode(mode)"
          >
            {{ mode === 'personal' ? '개인' : '팀' }}
          </button>
        </div>
      </div>

      <div class="flex flex-wrap items-center justify-between gap-4">
        <div class="flex flex-wrap items-center gap-3">
          <div v-if="store.calendarTab !== 'table'" class="inline-flex items-center gap-2 p-1 bg-slate-50 border border-slate-200 rounded-xl">
            <button class="p-1.5 rounded-lg hover:bg-white hover:shadow-sm transition-all" @click="store.shiftPeriod(-1)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 18l-6-6 6-6" /></svg>
            </button>
            <strong class="min-w-[120px] text-center text-sm font-semibold">{{ toolbarDateLabel }}</strong>
            <button class="p-1.5 rounded-lg hover:bg-white hover:shadow-sm transition-all" @click="store.shiftPeriod(1)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 18l6-6-6-6" /></svg>
            </button>
          </div>

          <div v-if="store.calendarTab !== 'table'" class="flex p-1 bg-slate-100 rounded-xl border border-slate-200 gap-1">
            <button
              v-for="view in ['week', 'month']"
              :key="view"
              class="px-4 py-1.5 rounded-lg text-sm font-medium transition-all"
              :class="store.calendarView === view ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
              @click="store.setCalendarView(view)"
            >
              {{ view === 'week' ? '주간' : '월간' }}
            </button>
          </div>

          <button v-if="showTodayButton && store.calendarTab !== 'table'" class="text-rose-500 font-bold text-sm px-2" @click="store.setToday()">오늘</button>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <button 
            v-for="tool in [
              { label: '필터', value: filterLabel, action: store.cycleStatusFilter },
              { label: '정렬', value: sortLabel, action: store.cycleSortMode },
              { label: '필드', value: fieldLabel, action: store.toggleSpanMode, show: store.calendarTab === 'calendar' }
            ].filter(t => t.show !== false)"
            :key="tool.label"
            class="inline-flex items-center gap-2 px-3 py-2 bg-slate-100 border border-slate-200 rounded-xl text-sm text-slate-500 hover:bg-slate-200 transition-colors"
            @click="tool.action"
          >
            {{ tool.label }} <strong class="text-slate-900 font-semibold">{{ tool.value }}</strong>
          </button>
        </div>
      </div>
    </div>

    <div :class="['grid gap-4 items-start', store.calendarTab === 'calendar' ? 'lg:grid-cols-[1fr_340px]' : 'grid-cols-1']">
      <div class="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm overflow-x-auto min-w-0">
        <CalendarBoard v-if="store.calendarTab === 'calendar'" :tasks="store.filteredTasks" :current-date="store.currentDate" :view-mode="store.calendarView" :span-mode="store.spanMode" @create-task="store.openCreateModal($event)" @move-task="moveTask" @open-task="store.openTask($event)" />
        <TableBoard v-else-if="store.calendarTab === 'table'" :tasks="store.filteredTasks" :current-date="store.currentDate" @create-task="store.openCreateModal($event)" @open-task="store.openTask($event)" />
        <GanttChart v-else :tasks="store.filteredTasks" :current-date="store.currentDate" :view-mode="store.calendarView" @move-task="moveTask" @open-task="store.openTask($event)" />
      </div>

      <aside v-if="store.calendarTab === 'calendar'" class="grid gap-4 w-full lg:w-[340px]">
        <article class="bg-white border border-slate-200 rounded-[22px] p-4 shadow-sm">
          <h3 class="text-base font-bold mb-4">콘텐츠 상태</h3>
          <div class="flex h-[90px] rounded-[18px] border border-slate-200 overflow-hidden bg-slate-50">
            <div
              v-for="segment in statusSummary"
              :key="segment.key"
              :style="{ flexBasis: `${Math.max(segment.percentage, 8)}%`, background: segment.color }"
              class="h-full"
            />
          </div>
          <div class="grid grid-cols-2 gap-2 mt-4">
            <div v-for="segment in statusSummary" :key="`${segment.key}-legend`" class="flex items-center gap-2 text-[13px]">
              <span class="w-2.5 h-2.5 rounded-full shrink-0" :style="{ backgroundColor: segment.color }" />
              <strong class="font-bold truncate">{{ segment.label }}</strong>
              <small class="text-slate-400 ml-auto">{{ segment.count }}</small>
            </div>
          </div>
        </article>

        <article class="bg-white border border-slate-200 rounded-[22px] p-4 shadow-sm">
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-base font-bold">진행 현황</h3>
            <span class="text-slate-500 text-sm">{{ completionRate }}% 완료</span>
          </div>
          <div class="flex justify-center py-4">
            <div class="w-[180px] h-[180px] rounded-full flex items-center justify-center relative" :style="donutStyle">
              <div class="w-[112px] h-[112px] rounded-full bg-white flex flex-col items-center justify-center shadow-inner border border-slate-100">
                <strong class="text-2xl font-bold leading-none">{{ completionRate }}%</strong>
                <small class="text-slate-400 text-xs mt-1">완료</small>
              </div>
            </div>
          </div>
        </article>

        <article class="bg-white border border-slate-200 rounded-[22px] p-4 shadow-sm">
          <h3 class="text-base font-bold mb-4">다음 일정</h3>
          <div class="grid gap-2.5">
            <button
              v-for="task in upcomingTasks"
              :key="task.id"
              class="w-full border border-slate-200 rounded-2xl bg-slate-50 p-3.5 flex items-center justify-between gap-4 text-left hover:bg-slate-100 transition-colors"
              @click="store.openTask(task.id)"
            >
              <div class="overflow-hidden">
                <strong class="block font-bold text-sm truncate text-slate-900">{{ task.title }}</strong>
                <small class="text-slate-400 text-xs">{{ task.customer }}</small>
              </div>
              <span class="text-slate-500 text-sm shrink-0">{{ formatShortDate(task.dueDate) }}</span>
            </button>
          </div>
        </article>
      </aside>
    </div>
  </section>
</template>