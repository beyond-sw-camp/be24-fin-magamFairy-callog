<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  currentDate: { type: Date, required: true },
  events: { type: Array, default: () => [] },
  toggles: {
    type: Object,
    default: () => ({ campaign: true, partnership: true }),
  },
})
const emit = defineEmits(['update:currentDate', 'update:toggles', 'event-click'])

const cursor = ref(new Date(props.currentDate))

const cursorYear = computed(() => cursor.value.getFullYear())
const cursorMonth = computed(() => cursor.value.getMonth())

function prevMonth() {
  cursor.value = new Date(cursorYear.value, cursorMonth.value - 1, 1)
}
function nextMonth() {
  cursor.value = new Date(cursorYear.value, cursorMonth.value + 1, 1)
}

const miniWeeks = computed(() => {
  const first = new Date(cursorYear.value, cursorMonth.value, 1)
  const last = new Date(cursorYear.value, cursorMonth.value + 1, 0)
  const start = new Date(first)
  start.setDate(start.getDate() - start.getDay())
  const weeks = []
  const cur = new Date(start)
  while (cur <= last || cur.getDay() !== 0) {
    const week = []
    for (let i = 0; i < 7; i++) {
      week.push({
        d: cur.getDate(),
        iso: cur.toISOString().slice(0, 10),
        inMonth: cur.getMonth() === cursorMonth.value,
        isToday: isSameDay(cur, new Date()),
        isSelected: isSameDay(cur, props.currentDate),
        hasEvent: hasEventOn(cur),
      })
      cur.setDate(cur.getDate() + 1)
    }
    weeks.push(week)
  }
  return weeks
})

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}
function hasEventOn(date) {
  const iso = date.toISOString().slice(0, 10)
  return props.events.some(e => e.start && e.end && iso >= e.start && iso <= e.end)
}
function selectDay(day) {
  emit('update:currentDate', new Date(day.iso))
}

const todayIso = new Date().toISOString().slice(0, 10)
const upcoming = computed(() => {
  const limit = new Date(); limit.setDate(limit.getDate() + 7)
  const limitIso = limit.toISOString().slice(0, 10)
  return [...props.events]
    .filter(e => e.start && e.end && e.end >= todayIso && e.start <= limitIso)
    .sort((a, b) => (a.start ?? '').localeCompare(b.start ?? ''))
    .slice(0, 5)
})

function fmtRange(s, e) {
  const fmt = (d) => {
    const dt = new Date(d)
    return `${dt.getMonth() + 1}/${dt.getDate()}`
  }
  return s && e ? `${fmt(s)} - ${fmt(e)}` : '-'
}
function dDayOf(end) {
  if (!end) return '-'
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const e = new Date(end); e.setHours(0, 0, 0, 0)
  const diff = Math.round((e - today) / 86400000)
  if (diff < 0) return `D+${-diff}`
  if (diff === 0) return 'D-DAY'
  return `D-${diff}`
}

function toggleKind(kind) {
  emit('update:toggles', { ...props.toggles, [kind]: !props.toggles[kind] })
}
</script>

<template>
  <aside class="cal-sidebar">
    <!-- 미니 캘린더 -->
    <section class="cal-sidebar__section mini">
      <header class="mini__head">
        <button class="mini__nav" @click="prevMonth" aria-label="이전 달">
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
        <h4 class="mini__title">{{ cursorYear }}.{{ String(cursorMonth + 1).padStart(2, '0') }}</h4>
        <button class="mini__nav" @click="nextMonth" aria-label="다음 달">
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
      </header>
      <div class="mini__row mini__row--head">
        <span v-for="d in ['일','월','화','수','목','금','토']" :key="d" class="mini__cell mini__cell--head">{{ d }}</span>
      </div>
      <div v-for="(week, i) in miniWeeks" :key="i" class="mini__row">
        <button
          v-for="day in week"
          :key="day.iso"
          type="button"
          class="mini__cell"
          :class="{
            'mini__cell--out': !day.inMonth,
            'mini__cell--today': day.isToday,
            'mini__cell--selected': day.isSelected,
          }"
          @click="selectDay(day)"
        >
          {{ day.d }}
          <span v-if="day.hasEvent" class="mini__dot" />
        </button>
      </div>
    </section>

    <!-- 캘린더 토글 -->
    <section class="cal-sidebar__section toggles">
      <h4 class="cal-sidebar__heading">일정 종류</h4>
      <label class="toggle-row">
        <input type="checkbox" :checked="toggles.campaign" @change="toggleKind('campaign')" />
        <span class="toggle-row__dot" style="background:#8B5CF6"></span>
        <span class="toggle-row__label">📣 캠페인 기간</span>
      </label>
      <label class="toggle-row">
        <input type="checkbox" :checked="toggles.deadline" @change="toggleKind('deadline')" />
        <span class="toggle-row__dot" style="background:#F59E0B"></span>
        <span class="toggle-row__label">⏰ 모집 마감</span>
      </label>
      <label class="toggle-row">
        <input type="checkbox" :checked="toggles.milestone" @change="toggleKind('milestone')" />
        <span class="toggle-row__dot" style="background:#3B82F6"></span>
        <span class="toggle-row__label">🚩 마일스톤</span>
      </label>
      <label class="toggle-row">
        <input type="checkbox" :checked="toggles.task" @change="toggleKind('task')" />
        <span class="toggle-row__dot" style="background:#10B981"></span>
        <span class="toggle-row__label">✅ 내 업무 마감</span>
      </label>
    </section>

    <!-- 다가오는 일정 -->
    <section class="cal-sidebar__section upcoming">
      <h4 class="cal-sidebar__heading">다가오는 일정 (7일)</h4>
      <ul v-if="upcoming.length" class="upcoming__list">
        <li
          v-for="ev in upcoming"
          :key="ev.id"
          class="upcoming__item"
          @click="emit('event-click', ev)"
        >
          <div class="upcoming__dot" :style="{ background: ev.customColor || (ev.type === 'deadline' ? '#F59E0B' : ev.type === 'milestone' ? '#3B82F6' : ev.type === 'task' ? '#10B981' : '#8B5CF6') }" />
          <div class="upcoming__body">
            <div class="upcoming__title">{{ ev.title }}</div>
            <div class="upcoming__meta">
              <span>{{ fmtRange(ev.start, ev.end) }}</span>
              <span class="upcoming__dday">{{ dDayOf(ev.end) }}</span>
            </div>
          </div>
        </li>
      </ul>
      <p v-else class="upcoming__empty">7일 내 일정이 없습니다.</p>
    </section>
  </aside>
</template>

<style scoped>
.cal-sidebar {
  width: 260px;
  flex-shrink: 0;
  border-left: 1px solid var(--border-color);
  background: var(--panel-color);
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.cal-sidebar__section { padding-bottom: 14px; border-bottom: 1px solid var(--border-color); }
.cal-sidebar__section:last-child { border-bottom: none; padding-bottom: 0; }
.cal-sidebar__heading {
  font-size: 11px;
  font-weight: 800;
  color: var(--muted-text);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin: 0 0 10px;
}

/* Mini calendar */
.mini__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.mini__title { font-size: 13px; font-weight: 800; color: var(--text-primary); margin: 0; font-variant-numeric: tabular-nums; }
.mini__nav {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mini__nav:hover { background: var(--panel-muted); color: var(--text-primary); }
.mini__nav .material-symbols-outlined { font-size: 16px; }
.mini__row { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; }
.mini__row--head { margin-bottom: 4px; }
.mini__cell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  font-size: 11px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  font-variant-numeric: tabular-nums;
}
.mini__cell--head {
  font-size: 10px;
  font-weight: 700;
  color: var(--muted-text);
  cursor: default;
  height: 20px;
}
.mini__cell:not(.mini__cell--head):hover { background: var(--panel-muted); }
.mini__cell--out { color: var(--subtle-text); opacity: 0.5; }
.mini__cell--today { color: var(--accent-color, #8B5CF6); font-weight: 800; }
.mini__cell--selected { background: var(--accent-color, #8B5CF6); color: #fff; }
.mini__cell--selected.mini__cell--today { color: #fff; }
.mini__dot {
  position: absolute;
  bottom: 3px;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--accent-color, #8B5CF6);
}
.mini__cell--selected .mini__dot { background: #fff; }

/* Toggles */
.toggle-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  cursor: pointer;
  user-select: none;
}
.toggle-row input { accent-color: var(--accent-color, #8B5CF6); width: 14px; height: 14px; cursor: pointer; }
.toggle-row__dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.toggle-row__label { font-size: 12.5px; color: var(--text-primary); }

/* Upcoming */
.upcoming__list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.upcoming__item {
  display: flex;
  gap: 8px;
  padding: 8px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.upcoming__item:hover { background: var(--panel-muted); }
.upcoming__dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; margin-top: 5px; }
.upcoming__body { flex: 1; min-width: 0; }
.upcoming__title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.upcoming__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 2px;
  font-size: 10px;
  color: var(--muted-text);
}
.upcoming__dday { font-weight: 800; color: var(--accent-color, #8B5CF6); font-variant-numeric: tabular-nums; }
.upcoming__empty {
  font-size: 11px;
  color: var(--muted-text);
  text-align: center;
  padding: 12px 0;
  margin: 0;
}
</style>
