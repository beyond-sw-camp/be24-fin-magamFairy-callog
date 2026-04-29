<script setup>
import { ref, onMounted, computed } from 'vue';
import MainCalendar from '@/components/overview/MainCalendar.vue';
import MainTable from '@/components/overview/MainTable.vue';
import MainTimeline from '@/components/overview/MainTimeline.vue';
import { usePlannerStore } from '@/stores/planner';
import { usePartnershipsStore } from '@/stores/partnerships';
import { getCampagin } from '@/api/overview';

const store = usePlannerStore()
const partnershipStore = usePartnershipsStore()
const isDark = computed(() => store.theme === 'dark')
const searchQuery = ref('')
const currentView = ref('calendar')
const sidebarTab = ref('upcoming')

const viewOptions = [
  { id: 'calendar', name: '캘린더', icon: 'calendar_month' },
  { id: 'timeline', name: '타임라인', icon: 'timeline' },
  { id: 'table', name: '테이블', icon: 'table_rows' },
]

onMounted(async () => {
  try {
    const res = await getCampagin()
    events.value = res.data.map(event => ({
      ...event,
      colorIndex: (event.id % 10) % 5
    }))
  } catch (error) {
    console.error('캠페인 정보를 불러오는데 실패했습니다.', error)
  }
})

const getEventColor = (index) => {
  const lightPalette = [
    'bg-violet-100 text-violet-800 border border-violet-200',
    'bg-fuchsia-100 text-fuchsia-800 border border-fuchsia-200',
    'bg-blue-100 text-blue-800 border border-blue-200',
    'bg-emerald-100 text-emerald-800 border border-emerald-200',
    'bg-amber-100 text-amber-800 border border-amber-200',
  ]
  const darkPalette = [
    'bg-violet-900/30 text-violet-300 border border-violet-700/50',
    'bg-fuchsia-900/30 text-fuchsia-300 border border-fuchsia-700/50',
    'bg-blue-900/30 text-blue-300 border border-blue-700/50',
    'bg-emerald-900/30 text-emerald-300 border border-emerald-700/50',
    'bg-amber-900/30 text-amber-300 border border-amber-700/50',
  ]
  return isDark.value ? darkPalette[index % 5] : lightPalette[index % 5]
}

const events = ref([
  { id: 1, title: '한화 시스템 신규 개발 프로젝트 캠페인', start: '2026-03-20', end: '2026-04-24', projectManager: '한화 시스템' },
  { id: 2, title: '갤러리아 아이브 팝업스토어 캠페인', start: '2026-04-21', end: '2026-05-20', projectManager: '한화 갤러리아' },
  { id: 3, title: '한화 호텔앤리조트 콜라보 이벤트 캠페인', start: '2026-04-27', end: '2026-05-02', projectManager: '한화 호텔앤리조트' },
  { id: 4, title: '한화 캠페인 4', start: '2026-04-26', end: '2026-05-15', projectManager: '한화 계열사1' },
  { id: 5, title: '한화 캠페인 5', start: '2026-05-04', end: '2026-05-28', projectManager: '한화 계열사2' },
  { id: 6, title: '한화 캠페인 6', start: '2026-05-13', end: '2026-06-02', projectManager: '한화 계열사3' },
  { id: 7, title: '한화 캠페인 7', start: '2026-05-13', end: '2026-06-02', projectManager: '한화 계열사3' },
])
events.value = events.value.map(event => ({
  ...event,
  colorIndex: (event.id % 10) % 5,
}))

const partnershipEvents = computed(() => {
  const confirmed = (partnershipStore.programs ?? []).filter(
    (p) => ['accepted', 'operating'].includes(p.stage),
  )
  return confirmed
    .filter((p) => p.startDate && p.endDate)
    .map((program, index) => ({
      id: 9000 + index,
      title: `[제휴] ${program.name}`,
      start: program.startDate,
      end: program.endDate,
      projectManager: program.owner ?? '제휴팀',
      colorIndex: 3,
      isPartnership: true,
    }))
})

const formattedEvents = computed(() => {
  const allEvents = [...events.value, ...partnershipEvents.value]
  return allEvents.map(event => ({
    ...event,
    colorClass: getEventColor(event.colorIndex),
  }))
})

const filteredEvents = computed(() => {
  if (!searchQuery.value.trim()) return formattedEvents.value
  const q = searchQuery.value.trim().toLowerCase()
  return formattedEvents.value.filter(e =>
    e.title.toLowerCase().includes(q) ||
    (e.projectManager ?? '').toLowerCase().includes(q),
  )
})

// Deadlines sidebar
const MONTH_ABBR = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']

function parseDateBadge(dateStr) {
  if (!dateStr) return { month: '', day: '' }
  const d = new Date(dateStr)
  return { month: MONTH_ABBR[d.getMonth()], day: String(d.getDate()) }
}

const DEADLINE_STATUS_META = {
  due_soon: { label: '임박' },
  upcoming: { label: '예정' },
  campaign: { label: '캠페인' },
}

const upcomingDeadlines = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  const limit = new Date()
  limit.setDate(limit.getDate() + 60)
  const maxDate = limit.toISOString().slice(0, 10)

  const rfpItems = (partnershipStore.recruitingMilestones ?? [])
    .filter(m => ['upcoming', 'due_soon'].includes(m.status) && m.date >= today)
    .map(m => ({
      id: `rfp-${m.id}`,
      date: m.date,
      title: m.title,
      statusKey: m.status,
      category: 'rfp',
      subtitle: partnershipStore.programs?.find(p => p.id === m.programId)?.name ?? '제휴',
    }))

  const opsItems = (partnershipStore.operationsMilestones ?? [])
    .filter(m => m.status === 'upcoming' && m.date >= today)
    .map(m => ({
      id: `ops-${m.id}`,
      date: m.date,
      title: m.title,
      statusKey: 'upcoming',
      category: 'ops',
      subtitle: partnershipStore.programs?.find(p => p.id === m.programId)?.name ?? '운영',
    }))

  const campaignItems = events.value
    .filter(e => e.end && e.end >= today && e.end <= maxDate)
    .map(e => ({
      id: `campaign-${e.id}`,
      date: e.end,
      title: e.title,
      statusKey: 'campaign',
      category: 'campaign',
      subtitle: e.projectManager ?? '',
    }))

  return [...rfpItems, ...opsItems, ...campaignItems]
    .sort((a, b) => a.date.localeCompare(b.date))
    .slice(0, 12)
})
</script>

<template>
  <div class="campaign-calendar">

    <!-- Header -->
    <header class="campaign-calendar__header">
      <h2 class="campaign-calendar__title">캠페인 캘린더</h2>

      <div class="campaign-calendar__view-tabs">
        <template v-for="(view, i) in viewOptions" :key="view.id">
          <button
            class="campaign-calendar__view-btn"
            :class="{ 'campaign-calendar__view-btn--active': currentView === view.id }"
            @click="currentView = view.id"
          >
            <span class="material-symbols-outlined">{{ view.icon }}</span>
            {{ view.name }}
          </button>
          <span v-if="i < viewOptions.length - 1" class="campaign-calendar__view-sep" />
        </template>
      </div>

      <div class="campaign-calendar__controls">
        <div class="campaign-calendar__search">
          <span class="material-symbols-outlined">search</span>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="캠페인 검색..."
            class="campaign-calendar__search-input"
          />
        </div>
        <button class="campaign-calendar__filter-btn">
          <span class="material-symbols-outlined">filter_list</span>
          필터
        </button>
      </div>
    </header>

    <!-- Body -->
    <div class="campaign-calendar__body">
      <transition name="view-fade" mode="out-in">

        <!-- Calendar view: two-panel -->
        <div v-if="currentView === 'calendar'" key="calendar" class="campaign-calendar__body-inner">
          <main class="campaign-calendar__main">
            <MainCalendar :eventsData="filteredEvents" />
          </main>

          <aside class="campaign-calendar__sidebar">
            <div class="deadlines-panel">
              <div class="deadlines-panel__header">
                <h3>Deadlines</h3>
                <button class="deadlines-panel__more-btn">
                  <span class="material-symbols-outlined">more_horiz</span>
                </button>
              </div>

              <div class="deadlines-panel__tabs">
                <button
                  class="deadlines-panel__tab"
                  :class="{ 'deadlines-panel__tab--active': sidebarTab === 'upcoming' }"
                  @click="sidebarTab = 'upcoming'"
                >Upcoming</button>
                <button
                  class="deadlines-panel__tab"
                  :class="{ 'deadlines-panel__tab--active': sidebarTab === 'timeline' }"
                  @click="sidebarTab = 'timeline'"
                >Timeline View</button>
              </div>

              <div class="deadlines-panel__list">
                <div
                  v-for="item in upcomingDeadlines"
                  :key="item.id"
                  class="deadline-item"
                >
                  <div class="deadline-item__date">
                    <span class="deadline-item__month">{{ parseDateBadge(item.date).month }}</span>
                    <span class="deadline-item__day">{{ parseDateBadge(item.date).day }}</span>
                  </div>
                  <div class="deadline-item__content">
                    <p class="deadline-item__title">{{ item.title }}</p>
                    <p class="deadline-item__subtitle">{{ item.subtitle }}</p>
                    <div class="deadline-item__status">
                      <span class="deadline-item__dot" :data-status="item.statusKey" :data-category="item.category"></span>
                      <span class="deadline-item__status-label">{{ DEADLINE_STATUS_META[item.statusKey]?.label ?? '예정' }}</span>
                    </div>
                    <div class="deadline-item__bar" :data-status="item.statusKey" :data-category="item.category"></div>
                  </div>
                </div>

                <div v-if="upcomingDeadlines.length === 0" class="deadlines-panel__empty">
                  <span class="material-symbols-outlined">event_available</span>
                  <p>예정된 마감 일정이 없습니다</p>
                </div>
              </div>
            </div>
          </aside>
        </div>

        <!-- Timeline view -->
        <div v-else-if="currentView === 'timeline'" key="timeline" class="campaign-calendar__body-inner campaign-calendar__body-inner--full">
          <main class="campaign-calendar__main">
            <MainTimeline :eventsData="filteredEvents" />
          </main>
        </div>

        <!-- Table view -->
        <div v-else-if="currentView === 'table'" key="table" class="campaign-calendar__body-inner campaign-calendar__body-inner--full">
          <main class="campaign-calendar__main">
            <MainTable :eventsData="filteredEvents" />
          </main>
        </div>

      </transition>
    </div>

  </div>
</template>

<style scoped>
/* === Layout === */
.campaign-calendar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--panel-color);
  color: var(--text-primary);
}

/* === Header === */
.campaign-calendar__header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-color);
  position: sticky;
  top: 0;
  z-index: 20;
  flex-shrink: 0;
}

.campaign-calendar__title {
  font-size: 17px;
  font-weight: 750;
  color: var(--text-primary);
  white-space: nowrap;
  flex-shrink: 0;
  letter-spacing: -0.01em;
}

.campaign-calendar__view-tabs {
  display: flex;
  align-items: center;
  gap: 2px;
  background: var(--panel-muted);
  padding: 3px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-color);
  flex-shrink: 0;
}

.campaign-calendar__view-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 650;
  color: var(--muted-text);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
  background: none;
}

.campaign-calendar__view-btn .material-symbols-outlined {
  font-size: 15px;
}

.campaign-calendar__view-btn:hover {
  color: var(--text-primary);
  background: color-mix(in srgb, var(--panel-color) 60%, transparent);
}

.campaign-calendar__view-btn--active {
  background: var(--panel-color);
  color: var(--accent-color);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.campaign-calendar__view-sep {
  width: 1px;
  height: 12px;
  background: var(--border-color);
  margin: 0 2px;
}

.campaign-calendar__controls {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.campaign-calendar__search {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 6px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--control-color);
  transition: border-color var(--transition-fast);
}

.campaign-calendar__search:focus-within {
  border-color: var(--accent-color);
}

.campaign-calendar__search .material-symbols-outlined {
  font-size: 15px;
  color: var(--subtle-text);
  flex-shrink: 0;
}

.campaign-calendar__search-input {
  border: none;
  background: none;
  outline: none;
  font-size: 13px;
  color: var(--text-primary);
  width: 160px;
}

.campaign-calendar__search-input::placeholder {
  color: var(--subtle-text);
}

.campaign-calendar__filter-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 12px;
  font-weight: 650;
  color: var(--text-secondary);
  background: var(--control-color);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.campaign-calendar__filter-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
}

.campaign-calendar__filter-btn .material-symbols-outlined {
  font-size: 15px;
}

/* === Body === */
.campaign-calendar__body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.campaign-calendar__body-inner {
  display: flex;
  height: 100%;
}

.campaign-calendar__body-inner--full {
  display: block;
}

.campaign-calendar__main {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* === Deadlines Sidebar === */
.campaign-calendar__sidebar {
  width: 290px;
  flex-shrink: 0;
  border-left: 1px solid var(--border-color);
  overflow-y: auto;
  background: var(--panel-muted);
}

.deadlines-panel {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.deadlines-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  background: var(--panel-muted);
  z-index: 5;
}

.deadlines-panel__header h3 {
  font-size: 13px;
  font-weight: 750;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.deadlines-panel__more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  color: var(--subtle-text);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.deadlines-panel__more-btn:hover {
  background: var(--panel-color);
}

.deadlines-panel__more-btn .material-symbols-outlined {
  font-size: 17px;
}

.deadlines-panel__tabs {
  display: flex;
  gap: 4px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color);
}

.deadlines-panel__tab {
  flex: 1;
  padding: 5px 8px;
  font-size: 11px;
  font-weight: 650;
  color: var(--muted-text);
  background: none;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  text-align: center;
  transition: all var(--transition-fast);
}

.deadlines-panel__tab:hover {
  color: var(--text-primary);
}

.deadlines-panel__tab--active {
  background: var(--panel-color);
  color: var(--text-primary);
  border-color: var(--border-color);
}

.deadlines-panel__list {
  flex: 1;
}

/* === Deadline Item === */
.deadline-item {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid color-mix(in srgb, var(--border-color) 50%, transparent);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.deadline-item:hover {
  background: color-mix(in srgb, var(--panel-color) 70%, var(--panel-muted));
}

.deadline-item__date {
  display: grid;
  place-items: center;
  align-content: center;
  flex-shrink: 0;
  width: 40px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  text-align: center;
  gap: 1px;
}

.deadline-item__month {
  font-size: 8px;
  font-weight: 800;
  text-transform: uppercase;
  color: var(--accent-color);
  letter-spacing: 0.06em;
}

.deadline-item__day {
  font-size: 15px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
}

.deadline-item__content {
  flex: 1;
  min-width: 0;
}

.deadline-item__title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
  margin: 0 0 2px;
}

.deadline-item__subtitle {
  font-size: 10px;
  color: var(--muted-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0 0 5px;
}

.deadline-item__status {
  display: flex;
  align-items: center;
  gap: 4px;
}

.deadline-item__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--subtle-text);
}

.deadline-item__dot[data-status='due_soon'] { background: var(--color-warning); }
.deadline-item__dot[data-status='upcoming'][data-category='rfp'] { background: var(--accent-color); }
.deadline-item__dot[data-status='upcoming'][data-category='ops'] { background: var(--color-success); }
.deadline-item__dot[data-status='campaign'] { background: #8B5CF6; }

.deadline-item__status-label {
  font-size: 10px;
  color: var(--muted-text);
  font-weight: 650;
}

.deadline-item__bar {
  height: 2px;
  border-radius: 1px;
  margin-top: 7px;
  background: var(--border-color);
}

.deadline-item__bar[data-status='due_soon'] { background: var(--color-warning); }
.deadline-item__bar[data-status='upcoming'][data-category='rfp'] { background: var(--accent-color); }
.deadline-item__bar[data-status='upcoming'][data-category='ops'] { background: var(--color-success); }
.deadline-item__bar[data-status='campaign'] { background: #8B5CF6; }

.deadlines-panel__empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  padding: 48px 20px;
  color: var(--muted-text);
  text-align: center;
}

.deadlines-panel__empty .material-symbols-outlined {
  font-size: 30px;
  color: var(--subtle-text);
}

.deadlines-panel__empty p {
  font-size: 12px;
  margin: 0;
}

/* === View Transition === */
.view-fade-enter-active,
.view-fade-leave-active {
  transition: opacity 0.18s ease;
}
.view-fade-enter-from,
.view-fade-leave-to {
  opacity: 0;
}
</style>
