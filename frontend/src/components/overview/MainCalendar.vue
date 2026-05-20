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

// "+N more" 처리
const MAX_VISIBLE_PER_WEEK = 3

// 캘린더 로직 — slot 알고리즘으로 멀티-위크 이벤트 막대 배치
const calendarWeeks = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value - 1
  const firstDayOfMonth = new Date(year, month, 1)
  const lastDayOfMonth = new Date(year, month + 1, 0)

  const startDate = new Date(firstDayOfMonth)
  startDate.setDate(startDate.getDate() - startDate.getDay())

  const weeks = []
  let current = new Date(startDate)

  while (current <= lastDayOfMonth || current.getDay() !== 0) {
    const week = { days: [], events: [], extraByDay: [0,0,0,0,0,0,0] }
    for (let i = 0; i < 7; i++) {
      const dateStr = fmtIso(current)
      week.days.push({
        date: dateStr,
        dayOfMonth: current.getDate(),
        dayOfWeek: current.getDay(),
        isCurrentMonth: current.getMonth() === month,
      })
      current.setDate(current.getDate() + 1)
    }

    const weekStart = new Date(week.days[0].date)
    const weekEnd = new Date(week.days[6].date)
    const slots = []

    // sort events by start date for deterministic placement
    const sorted = [...events.value].sort((a, b) => (a.start ?? '').localeCompare(b.start ?? ''))
    sorted.forEach(evt => {
      // ⚠ 버그 수정: task 의 start/end 는 datetime("2026-05-22T10:00:00") 이라
      // 날짜 전용("2026-05-22") cell 과 === 비교가 안 됨 → findIndex -1 → span=7(한 주 전체).
      // 날짜 부분(YYYY-MM-DD)만 잘라서 비교한다.
      const startStr = (evt.start ?? '').slice(0, 10)
      const endStr = (evt.end ?? '').slice(0, 10) || startStr
      const evtStart = new Date(startStr)
      const evtEnd = new Date(endStr)
      if (!(evtStart <= weekEnd && evtEnd >= weekStart)) return

      const startOffset = evtStart < weekStart
        ? 0
        : Math.max(0, week.days.findIndex(d => d.date === startStr))
      const endIdx = week.days.findIndex(d => d.date === endStr)
      const endOffset = endIdx === -1 ? 6 : Math.min(6, endIdx)
      const span = endOffset - startOffset + 1

      let slotIndex = 0
      while (slots[slotIndex] && slots[slotIndex].some(s => !(startOffset > s.end || startOffset + span - 1 < s.start))) {
        slotIndex++
      }

      if (!slots[slotIndex]) slots[slotIndex] = []
      slots[slotIndex].push({ start: startOffset, end: startOffset + span - 1 })

      week.events[slotIndex] = week.events[slotIndex] || []
      week.events[slotIndex].push({
        ...evt,
        isVisible: true,
        startOffset,
        span,
        slotIndex,
      })
    })

    // overflow per day — slots beyond MAX_VISIBLE_PER_WEEK 모두 +N개로 합침
    for (let s = MAX_VISIBLE_PER_WEEK; s < slots.length; s++) {
      ;(slots[s] ?? []).forEach(seg => {
        for (let d = seg.start; d <= seg.end; d++) week.extraByDay[d] += 1
      })
    }
    // 상위 슬롯만 표시할 이벤트로 flatten
    const visible = []
    for (let i = 0; i < Math.min(MAX_VISIBLE_PER_WEEK, slots.length); i++) {
      if (week.events[i]) week.events[i].forEach(e => visible.push(e))
      else visible.push({ id: `empty-${i}`, isVisible: false, slotIndex: i })
    }
    week.events = visible
    weeks.push(week)
  }
  return weeks
})

// 그날 일정 전체 (모달용)
function eventsOnDay(iso) {
  return events.value.filter(e => {
    if (!e.start) return false
    const s = e.start.slice(0, 10)
    const en = (e.end ?? '').slice(0, 10) || s
    return iso >= s && iso <= en
  })
}
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
        <!-- 셀 (mini-card surface + 날짜 원 + 빈 영역 클릭) -->
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
        </div>

        <!-- 이벤트 막대 오버레이 (multi-day = single bar spanning cells) -->
        <div class="main-cal__events">
          <div
            v-for="(ev, idx) in week.events"
            :key="ev.id"
            class="main-cal__event-row"
            :style="{ top: (idx * 26) + 'px' }"
          >
            <div
              v-if="ev.isVisible"
              class="main-cal__event"
              :class="[ev.customColor ? '' : ev.colorClass, { 'main-cal__event--dragging': dragState?.event?.id === ev.id }]"
              :style="{
                marginLeft: `calc(${(ev.startOffset / 7) * 100}% + 6px)`,
                width: `calc(${(ev.span / 7) * 100}% - 12px)`,
                ...(ev.customColor ? {
                  background: ev.customColor,
                  color: '#2D2649',
                  borderColor: 'transparent',
                } : {}),
              }"
              draggable="true"
              @click.stop="emit('event-click', ev)"
              @mouseenter="onEventHover(ev, $event)"
              @mouseleave="onEventLeave"
              @dragstart="onEventDragStart(ev, $event, 'move')"
              @dragend="onDragEnd"
            >
              <!-- 좌측 리사이즈 핸들 -->
              <span
                class="main-cal__event-handle main-cal__event-handle--start"
                draggable="true"
                @dragstart.stop="onEventDragStart(ev, $event, 'resize-start')"
                @dragend.stop="onDragEnd"
                @click.stop
              ></span>
              <span v-if="ev.icon" class="main-cal__event-icon">{{ ev.icon }}</span>{{ ev.title }}
              <!-- 우측 리사이즈 핸들 -->
              <span
                class="main-cal__event-handle main-cal__event-handle--end"
                draggable="true"
                @dragstart.stop="onEventDragStart(ev, $event, 'resize-end')"
                @dragend.stop="onDragEnd"
                @click.stop
              ></span>
            </div>
          </div>
        </div>

        <!-- "+N more" 칩 (셀 하단) -->
        <div class="main-cal__extras">
          <button
            v-for="(extra, di) in week.extraByDay"
            :key="di"
            v-show="extra > 0"
            class="main-cal__more"
            :style="{ left: `calc(${(di / 7) * 100}% + 6px)`, width: `calc(${(1 / 7) * 100}% - 12px)` }"
            @click.stop="emit('more-click', { date: week.days[di].date, events: eventsOnDay(week.days[di].date) })"
          >+{{ extra }} more</button>
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
  overflow: hidden;
}
.main-cal__week {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  /* gap intentionally 0: event bars use % of week width and must align to columns */
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
  min-height: 96px;
}
.main-cal__cell::before {
  content: '';
  position: absolute;
  inset: 3px;
  border-radius: 14px;
  background: var(--lp-surface);
  box-shadow: inset 0 0 0 1px rgba(229, 221, 240, .55);
  z-index: 0;
  transition: box-shadow 0.18s ease, background 0.18s;
}
.main-cal__cell:hover::before {
  box-shadow:
    inset 0 0 0 1px rgba(229, 221, 240, .9),
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

/* ═══ Events overlay (Linear/Notion-style multi-day bars) ═══ */
.main-cal__events {
  position: absolute;
  top: 32px;
  left: 0;
  right: 0;
  bottom: 22px;
  pointer-events: none;
  overflow: hidden;
}
.main-cal__event-row {
  position: absolute;
  left: 0;
  right: 0;
  height: 22px;
}
.main-cal__event {
  display: block;
  position: relative;
  height: 22px;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 600;
  line-height: 14px;
  border-radius: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  pointer-events: auto;
  cursor: pointer;
  background: var(--lp-card-lavender-1);
  color: var(--lp-violet-deep);
  border: 0;
  box-shadow: 0 1px 2px rgba(63,52,99,.05);
  letter-spacing: -0.005em;
  transition: margin-left 0.2s cubic-bezier(0.16, 1, 0.3, 1),
              width 0.2s cubic-bezier(0.16, 1, 0.3, 1),
              transform 0.12s, box-shadow 0.15s;
}
.main-cal__event:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(63, 52, 99, 0.16);
}
.main-cal__event-icon { font-size: 11px; margin-right: 3px; }
.main-cal__event--dragging { opacity: 0.45; }

/* 양쪽 리사이즈 핸들 */
.main-cal__event-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: ew-resize;
  background: transparent;
  z-index: 1;
}
.main-cal__event-handle--start {
  left: 0;
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}
.main-cal__event-handle--end {
  right: 0;
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
}
.main-cal__event-handle:hover { background: rgba(63, 52, 99, 0.22); }

/* +N more */
.main-cal__extras {
  position: absolute;
  bottom: 4px;
  left: 0;
  right: 0;
  height: 16px;
  pointer-events: none;
}
.main-cal__more {
  position: absolute;
  height: 16px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--lp-primary-deep);
  background: transparent;
  border: none;
  cursor: pointer;
  pointer-events: auto;
  border-radius: 6px;
  text-align: left;
  padding: 0 4px;
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
  .main-cal__cell { min-height: 72px; border-radius: 12px; }
  .main-cal__date { font-size: 12px; }
  .main-cal__date--today { width: 24px; height: 24px; min-width: 24px; }
}
</style>
