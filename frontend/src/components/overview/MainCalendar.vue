<script setup>
import { ref, computed, watch } from 'vue';
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()
const isDark = computed(() => store.theme === 'dark')

const props = defineProps({
  eventsData: { type: Array, required: true, default: () => [] },
  anchorDate: { type: Date, default: () => new Date() },
})
const emit = defineEmits([
  'event-click',
  'day-click',
  'more-click',
  'event-drop',
  'update:anchorDate',
])

// 1. 상태
const currentDate = ref(new Date(props.anchorDate))
watch(() => props.anchorDate, (d) => { currentDate.value = new Date(d) })

const todayDate = new Date()
todayDate.setHours(0, 0, 0, 0)

const daysOfWeek = ['일', '월', '화', '수', '목', '금', '토']
const events = computed(() => props.eventsData)

const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth() + 1)

function shift(delta) {
  const d = new Date(currentYear.value, currentMonth.value - 1 + delta, 1)
  currentDate.value = d
  emit('update:anchorDate', d)
}
function gotoToday() {
  const d = new Date()
  currentDate.value = d
  emit('update:anchorDate', d)
}
const prevMonth = () => shift(-1)
const nextMonth = () => shift(1)
const today = gotoToday

function fmtIso(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
function isToday(iso) {
  return iso === fmtIso(todayDate)
}
function isPast(iso) {
  return iso < fmtIso(todayDate)
}

// Hover preview
const hoverEvent = ref(null)
const hoverPos = ref({ x: 0, y: 0 })
let hoverTimer = null
function onEventHover(ev, mouseEvt) {
  clearTimeout(hoverTimer)
  hoverTimer = setTimeout(() => {
    hoverEvent.value = ev
    const r = mouseEvt.currentTarget?.getBoundingClientRect()
    hoverPos.value = { x: r ? r.left + r.width / 2 : mouseEvt.clientX, y: r ? r.bottom + 6 : mouseEvt.clientY + 6 }
  }, 220)
}
function onEventLeave() {
  clearTimeout(hoverTimer)
  hoverEvent.value = null
}

// Drag-drop state — 'move' (전체 이동) | 'resize-start' | 'resize-end' (한쪽 끝)
const dragState = ref(null)
const hoverDate = ref(null)  // 드래그 중 hover한 셀 날짜 (미리보기용)

function onEventDragStart(ev, evt, mode = 'move') {
  dragState.value = { event: ev, mode, startClientX: evt.clientX }
  evt.dataTransfer.effectAllowed = 'move'
  try { evt.dataTransfer.setData('text/plain', String(ev.id)) } catch { /* ignore */ }
  // 드래그 ghost 이미지 숨기기 (UX 더 깔끔)
  const empty = document.createElement('canvas')
  empty.width = empty.height = 1
  try { evt.dataTransfer.setDragImage(empty, 0, 0) } catch { /* ignore */ }
}
function onCellDragOver(date, evt) {
  if (!dragState.value) return
  evt.preventDefault()
  evt.dataTransfer.dropEffect = 'move'
  hoverDate.value = date
}
function onCellDrop(date, evt) {
  if (!dragState.value) return
  evt.preventDefault()
  const { event: ev, mode } = dragState.value
  let newStart = ev.start
  let newEnd = ev.end
  if (mode === 'move') {
    const span = Math.round((new Date(ev.end) - new Date(ev.start)) / 86400000)
    newStart = date
    const ns = new Date(date); ns.setDate(ns.getDate() + span)
    newEnd = fmtIso(ns)
  } else if (mode === 'resize-start') {
    if (date <= ev.end) newStart = date
    else newStart = ev.end
  } else if (mode === 'resize-end') {
    if (date >= ev.start) newEnd = date
    else newEnd = ev.start
  }
  emit('event-drop', { event: ev, newStart, newEnd })
  dragState.value = null
  hoverDate.value = null
}
function onDragEnd() {
  dragState.value = null
  hoverDate.value = null
}

// 한 셀(날짜)에 들어갈 수 있는 최대 "줄" 수 (칩 + "+N more" 버튼 포함).
// 이벤트가 이 수보다 많으면 마지막 한 줄을 "+N more"로 양보해 잘리지 않게 한다.
const MAX_ROWS_PER_DAY = 3

// 날짜(iso, "YYYY-MM-DD") → 그날에 걸치는 이벤트 배열 맵.
// 멀티-데이 이벤트는 시작~종료 사이의 모든 날짜 셀에 동일하게 표시된다.
const eventsByDay = computed(() => {
  const map = new Map()
  for (const evt of events.value) {
    const startStr = (evt.start ?? '').slice(0, 10)
    if (!startStr) continue
    const endStr = (evt.end ?? '').slice(0, 10) || startStr
    const start = new Date(startStr)
    const end = new Date(endStr)
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) continue

    const cursor = new Date(start)
    let guard = 0
    while (cursor <= end && guard < 400) {
      const iso = fmtIso(cursor)
      if (!map.has(iso)) map.set(iso, [])
      map.get(iso).push(evt)
      cursor.setDate(cursor.getDate() + 1)
      guard++
    }
  }
  // 결정적 정렬: 시작일 → 제목
  for (const list of map.values()) {
    list.sort((a, b) =>
      (a.start ?? '').localeCompare(b.start ?? '') ||
      (a.title ?? '').localeCompare(b.title ?? ''),
    )
  }
  return map
})

// 그날 일정 전체 (모달/오버플로우용)
function eventsOnDay(iso) {
  return eventsByDay.value.get(iso) ?? []
}
function visibleEventsOnDay(iso) {
  const list = eventsOnDay(iso)
  // 다 들어가면 전부, 초과하면 "+N more" 한 줄을 위해 한 칸 양보.
  if (list.length <= MAX_ROWS_PER_DAY) return list
  return list.slice(0, MAX_ROWS_PER_DAY - 1)
}
function extraCountOnDay(iso) {
  const n = eventsOnDay(iso).length
  return n > MAX_ROWS_PER_DAY ? n - (MAX_ROWS_PER_DAY - 1) : 0
}

// 캘린더 격자 — 주(week) × 일(day) 구조만 계산. 이벤트는 셀별로 직접 렌더.
const calendarWeeks = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value - 1
  const firstDayOfMonth = new Date(year, month, 1)
  const lastDayOfMonth = new Date(year, month + 1, 0)

  const startDate = new Date(firstDayOfMonth)
  startDate.setDate(startDate.getDate() - startDate.getDay())

  const weeks = []
  const current = new Date(startDate)

  while (current <= lastDayOfMonth || current.getDay() !== 0) {
    const days = []
    for (let i = 0; i < 7; i++) {
      days.push({
        date: fmtIso(current),
        dayOfMonth: current.getDate(),
        dayOfWeek: current.getDay(),
        isCurrentMonth: current.getMonth() === month,
      })
      current.setDate(current.getDate() + 1)
    }
    weeks.push({ days })
  }
  return weeks
})
</script>

<template>
  <div class="main-cal lp-month" :class="{ 'main-cal--dark': isDark }">

    <!-- ═══ Top header bar: range label + Today + nav arrows ═══ -->
    <header class="main-cal__head lp-month-h">
      <div class="lp-month-h__left">
        <h1 class="main-cal__title lp-range-label">
          {{ currentYear }}년 {{ currentMonth }}월
        </h1>
      </div>
      <div class="main-cal__nav lp-month-h__right">
        <button @click="today" class="main-cal__btn main-cal__btn--primary lp-today-btn">오늘</button>
        <div class="lp-arrow-group">
          <button @click="prevMonth" class="main-cal__btn lp-arrow" aria-label="이전 달">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </button>
          <button @click="nextMonth" class="main-cal__btn lp-arrow" aria-label="다음 달">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </button>
        </div>
      </div>
    </header>

    <!-- ═══ Weekday row: Sun → Sat, no border, faint caps ═══ -->
    <div class="main-cal__dow-row">
      <div
        v-for="(d, i) in daysOfWeek"
        :key="d"
        class="main-cal__dow-cell"
        :class="{ 'main-cal__dow-cell--sun': i === 0, 'main-cal__dow-cell--sat': i === 6 }"
      >{{ d }}</div>
    </div>

    <div class="main-cal__grid">
      <div v-for="(week, wi) in calendarWeeks" :key="wi" class="main-cal__week">
        <!-- 셀 (mini-card surface + 날짜 원 + 그날의 이벤트 칩) -->
        <div
          v-for="day in week.days"
          :key="day.date"
          class="main-cal__cell"
          :class="{
            'main-cal__cell--today': isToday(day.date),
            'main-cal__cell--past': isPast(day.date) && !isToday(day.date),
            'main-cal__cell--out': !day.isCurrentMonth,
            'main-cal__cell--drag-over': dragState && hoverDate === day.date,
          }"
          @dragover="onCellDragOver(day.date, $event)"
          @drop="onCellDrop(day.date, $event)"
        >
          <div class="main-cal__cell-head">
            <span
              class="main-cal__date"
              :class="{
                'main-cal__date--today': isToday(day.date),
                'main-cal__date--sun': day.dayOfWeek === 0 && day.isCurrentMonth,
                'main-cal__date--sat': day.dayOfWeek === 6 && day.isCurrentMonth,
                'main-cal__date--out': !day.isCurrentMonth,
              }"
            >{{ day.dayOfMonth }}</span>
            <!-- + 버튼 (셀 우측 상단, hover 시 노출) -->
            <button
              v-if="day.isCurrentMonth"
              type="button"
              class="main-cal__add"
              :title="`${day.date} 일정 추가`"
              @click.stop="emit('day-click', { date: day.date, event: $event })"
            >+</button>
          </div>

          <!-- 그날의 이벤트 칩 (셀 내부에 직접 렌더) -->
          <div class="main-cal__cell-events">
            <button
              v-for="ev in visibleEventsOnDay(day.date)"
              :key="ev.id"
              type="button"
              class="main-cal__event"
              :class="[ev.customColor ? '' : ev.colorClass, { 'main-cal__event--dragging': dragState?.event?.id === ev.id }]"
              :style="ev.customColor ? { background: ev.customColor, color: '#2D2649', borderColor: 'transparent' } : {}"
              draggable="true"
              @click.stop="emit('event-click', ev)"
              @mouseenter="onEventHover(ev, $event)"
              @mouseleave="onEventLeave"
              @dragstart="onEventDragStart(ev, $event, 'move')"
              @dragend="onDragEnd"
            >
              <span v-if="ev.icon" class="main-cal__event-icon">{{ ev.icon }}</span>{{ ev.title }}
            </button>

            <!-- "+N more" -->
            <button
              v-if="extraCountOnDay(day.date) > 0"
              type="button"
              class="main-cal__more"
              @click.stop="emit('more-click', { date: day.date, events: eventsOnDay(day.date) })"
            >+{{ extraCountOnDay(day.date) }} more</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Hover preview -->
    <transition name="hover-fade">
      <div
        v-if="hoverEvent"
        class="hover-tip"
        :style="{ left: hoverPos.x + 'px', top: hoverPos.y + 'px' }"
      >
        <div class="hover-tip__title">{{ hoverEvent.title }}</div>
        <div class="hover-tip__row">
          <span class="material-symbols-outlined">event</span>
          {{ hoverEvent.start }} ~ {{ hoverEvent.end }}
        </div>
        <div v-if="hoverEvent.projectManager" class="hover-tip__row">
          <span class="material-symbols-outlined">person</span>
          {{ hoverEvent.projectManager }}
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.main-cal {
  /* --lp-* tokens cascade from :root (base.css). */
  --lp-card-lavender-3: #B0A4DA;
  --r-md: 14px;
  --r-lg: 18px;
  --r-xl: 24px;
  --r-pill: 999px;
  --shadow-card: 0 1px 2px rgba(63,52,99,.04), 0 6px 18px rgba(63,52,99,.06);

  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
  background: var(--lp-surface);
  color: var(--lp-text);
  position: relative;
  border-radius: 24px;
  padding: 24px 26px 18px;
  box-shadow: var(--shadow-card);
  font-family: 'Pretendard Variable', 'Pretendard', -apple-system, sans-serif;
  font-feature-settings: 'tnum' 1, 'ss01' 1;
}

/* ═══ Top header bar: range label · Today · arrows ═══ */
.main-cal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 14px;
  border-bottom: 0;
  gap: 12px;
  flex-wrap: wrap;
}
.main-cal__title,
.lp-range-label {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  letter-spacing: -0.01em;
  color: var(--lp-primary-deep);
}
.main-cal__nav { display: inline-flex; align-items: center; gap: 10px; }

.lp-arrow-group { display: inline-flex; align-items: center; gap: 4px; }
.main-cal__btn.lp-arrow {
  width: 30px;
  height: 30px;
  padding: 0;
  border-radius: 999px;
  background: var(--lp-surface);
  border: 1px solid var(--lp-border);
  color: var(--lp-primary-deep);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background .15s, border-color .15s;
}
.main-cal__btn.lp-arrow:hover {
  background: var(--lp-surface-soft);
  border-color: var(--lp-primary);
}

.main-cal__btn.main-cal__btn--primary.lp-today-btn {
  background: var(--lp-button-bg);
  color: #fff;
  border: 0;
  padding: 7px 18px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}
.main-cal__btn.main-cal__btn--primary.lp-today-btn:hover { background: var(--lp-button-bg-hover); }

/* ═══ Day-of-week row: faint caps, no border ═══ */
.main-cal__dow-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: transparent;
  border-bottom: 0;
  margin-bottom: 6px;
}
.main-cal__dow-cell {
  padding: 8px 0 6px;
  text-align: center;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--lp-text-faint);
  text-transform: capitalize;
  letter-spacing: 0.04em;
  border-right: 0;
}
.main-cal__dow-cell--sun { color: #C0837A; }
.main-cal__dow-cell--sat { color: var(--lp-primary-strong); }

/* ═══ Grid: 7×6 rounded mini-cards ═══ */
.main-cal__grid {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  overflow-x: hidden;
}
.main-cal__week {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  /* 단일 행이 주(week)의 flex 높이를 꽉 채우도록 1fr 지정 →
     셀이 아래까지 늘어나 둥근 사각형이 닫힌다(바닥 뚫림 방지). */
  grid-template-rows: 1fr;
  gap: 0;
  border-bottom: 0;
  position: relative;
  min-height: 0;
}

.main-cal__cell {
  padding: 3px;
  border: 0;
  background: transparent;
  position: relative;
  transition: background 0.18s;
  overflow: hidden;
  min-height: 116px;
}
.main-cal__cell::before {
  content: '';
  position: absolute;
  inset: 3px;
  border-radius: 14px;
  background: var(--lp-surface);
  box-shadow: inset 0 0 0 1.5px rgba(199, 187, 224, .85);
  z-index: 0;
  transition: box-shadow 0.18s ease, background 0.18s;
}
.main-cal__cell:hover::before {
  box-shadow:
    inset 0 0 0 1.5px var(--lp-primary, #9D85FF),
    0 6px 18px rgba(63,52,99,.10);
}
.main-cal__cell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px 0;
  position: relative;
  z-index: 2;
}
.main-cal__add {
  position: relative;
  top: auto;
  right: auto;
  width: 20px;
  height: 20px;
  border: 0;
  background: color-mix(in srgb, var(--lp-primary) 22%, transparent);
  color: var(--lp-primary-strong);
  border-radius: 999px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, background 0.15s, transform 0.1s;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
  padding: 0;
}
.main-cal__cell:hover .main-cal__add { opacity: 1; }
.main-cal__add:hover {
  background: var(--lp-button-bg);
  color: #fff;
  transform: scale(1.10);
}

/* Today cell: lime soft gradient + accent disc */
.main-cal__cell--today::before {
  background:
    radial-gradient(circle at 14px 14px, rgba(216,235,117,.55) 0, rgba(216,235,117,.18) 24px, transparent 70px),
    linear-gradient(180deg, var(--lp-lime-soft) 0%, var(--lp-surface) 65%);
  box-shadow: inset 0 0 0 1px rgba(216, 235, 117, .9);
}

/* Past / adjacent-month — keep concepts, photo-style treatment */
.main-cal__cell--past .main-cal__date { opacity: 0.55; }
.main-cal__cell--out::before {
  background: transparent;
  box-shadow: inset 0 0 0 1px rgba(229, 221, 240, .28);
}
.main-cal__cell--out .main-cal__date { opacity: 0.45; }

.main-cal__cell--drag-over::before {
  background: color-mix(in srgb, var(--lp-primary) 14%, transparent);
  box-shadow: inset 0 0 0 2px var(--lp-primary-strong);
}

/* Date number: 14px weight 700, deep purple; today = 28px filled circle */
.main-cal__date {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--lp-primary-deep);
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum' 1;
  padding: 0;
  border-radius: 999px;
  width: auto;
  height: 22px;
  min-width: 22px;
  transition: background .2s, color .2s;
}
.main-cal__date--sun { color: #C0837A; }
.main-cal__date--sat { color: var(--lp-primary-strong); }
.main-cal__date--out { color: var(--lp-text-faint); }
.main-cal__date--today {
  background: var(--lp-button-bg);
  color: #fff !important;
  font-weight: 700;
  width: 28px;
  height: 28px;
  min-width: 28px;
  border-radius: 999px;
  box-shadow: 0 2px 6px rgba(63, 52, 99, 0.20);
}

/* ═══ Events — 셀 내부에 직접 쌓이는 칩 ═══ */
.main-cal__cell-events {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 4px 6px 6px;
  min-height: 0;
}
.main-cal__event {
  display: block;
  width: 100%;
  box-sizing: border-box;
  height: 20px;
  padding: 3px 8px;
  font-size: 10.5px;
  font-weight: 600;
  line-height: 14px;
  border-radius: 7px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
  cursor: pointer;
  background: var(--lp-card-lavender-1);
  color: var(--lp-violet-deep);
  border: 0;
  box-shadow: 0 1px 2px rgba(63,52,99,.05);
  letter-spacing: -0.005em;
  transition: transform 0.12s, box-shadow 0.15s, filter 0.12s;
}
.main-cal__event:hover {
  transform: translateY(-1px);
  filter: brightness(0.97);
  box-shadow: 0 4px 12px rgba(63, 52, 99, 0.16);
}
.main-cal__event-icon { font-size: 11px; margin-right: 3px; }
.main-cal__event--dragging { opacity: 0.45; }

/* +N more */
.main-cal__more {
  display: block;
  width: 100%;
  height: 17px;
  font-size: 10.5px;
  font-weight: 700;
  color: var(--lp-primary-deep);
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: 6px;
  text-align: left;
  padding: 0 6px;
  letter-spacing: 0.01em;
}
.main-cal__more:hover {
  background: color-mix(in srgb, var(--lp-primary) 14%, transparent);
  color: var(--lp-violet-deep);
}

/* Hover preview tooltip */
.hover-tip {
  position: fixed;
  transform: translateX(-50%);
  background: var(--lp-surface);
  border: 1px solid var(--lp-border);
  border-radius: var(--r-md);
  padding: 10px 12px;
  font-size: 12px;
  box-shadow: var(--shadow-card);
  z-index: 150;
  pointer-events: none;
  min-width: 200px;
  max-width: 280px;
}
.hover-tip__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--lp-text);
  margin-bottom: 6px;
}
.hover-tip__row {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--lp-text-muted);
  padding: 1px 0;
}
.hover-tip__row .material-symbols-outlined { font-size: 13px; }

.hover-fade-enter-active, .hover-fade-leave-active { transition: opacity 0.12s; }
.hover-fade-enter-from, .hover-fade-leave-to { opacity: 0; }

@media (prefers-reduced-motion: reduce) {
  .main-cal__cell, .main-cal__event { transition: none; }
}

@media (max-width: 720px) {
  .main-cal { padding: 16px 14px 12px; border-radius: 18px; }
  .main-cal__title, .lp-range-label { font-size: 15px; }
  .main-cal__cell { min-height: 124px; border-radius: 12px; }
  .main-cal__date { font-size: 12px; }
  .main-cal__date--today { width: 24px; height: 24px; min-width: 24px; }
}
</style>
