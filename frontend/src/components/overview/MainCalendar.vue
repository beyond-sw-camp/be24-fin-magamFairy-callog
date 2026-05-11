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
      const evtStart = new Date(evt.start)
      const evtEnd = new Date(evt.end)
      if (!(evtStart <= weekEnd && evtEnd >= weekStart)) return

      const startOffset = evtStart < weekStart
        ? 0
        : Math.max(0, week.days.findIndex(d => d.date === evt.start))
      const endIdx = week.days.findIndex(d => d.date === evt.end)
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
  return events.value.filter(e => e.start && e.end && iso >= e.start && iso <= e.end)
}
</script>

<template>
  <div class="main-cal" :class="{ 'main-cal--dark': isDark }">

    <header class="main-cal__head">
      <h1 class="main-cal__title">{{ currentYear }}년 {{ currentMonth }}월</h1>
      <div class="main-cal__nav">
        <button @click="prevMonth" class="main-cal__btn">지난 달</button>
        <button @click="today" class="main-cal__btn main-cal__btn--primary">이번 달</button>
        <button @click="nextMonth" class="main-cal__btn">다음 달</button>
      </div>
    </header>

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
        <!-- 셀 (배경 + 날짜 + 빈 영역 클릭) -->
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

        <!-- 이벤트 막대 오버레이 -->
        <div class="main-cal__events">
          <div
            v-for="(ev, idx) in week.events"
            :key="ev.id"
            class="main-cal__event-row"
            :style="{ top: (idx * 22) + 'px' }"
          >
            <div
              v-if="ev.isVisible"
              class="main-cal__event"
              :class="[ev.customColor ? '' : ev.colorClass, { 'main-cal__event--dragging': dragState?.event?.id === ev.id }]"
              :style="{
                marginLeft: `calc(${(ev.startOffset / 7) * 100}% + 4px)`,
                width: `calc(${(ev.span / 7) * 100}% - 8px)`,
                ...(ev.customColor ? {
                  background: `color-mix(in srgb, ${ev.customColor} 14%, transparent)`,
                  color: ev.customColor,
                  borderColor: `color-mix(in srgb, ${ev.customColor} 32%, transparent)`,
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

        <!-- "+N개 더" 칩 (셀 하단) -->
        <div class="main-cal__extras">
          <button
            v-for="(extra, di) in week.extraByDay"
            :key="di"
            v-show="extra > 0"
            class="main-cal__more"
            :style="{ left: `calc(${(di / 7) * 100}% + 4px)`, width: `calc(${(1 / 7) * 100}% - 8px)` }"
            @click.stop="emit('more-click', { date: week.days[di].date, events: eventsOnDay(week.days[di].date) })"
          >+{{ extra }}개 더</button>
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
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--panel-color);
  color: var(--text-primary);
  position: relative;
}

/* Header */
.main-cal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  border-bottom: 1px solid var(--border-color);
}
.main-cal__title { font-size: 18px; font-weight: 800; margin: 0; letter-spacing: -0.02em; }
.main-cal__nav { display: flex; gap: 6px; }
.main-cal__btn {
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 650;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-primary);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.12s;
}
.main-cal__btn:hover { background: var(--panel-muted); }
.main-cal__btn--primary {
  background: var(--accent-color, #8B5CF6);
  color: #fff;
  border-color: var(--accent-color, #8B5CF6);
}
.main-cal__btn--primary:hover { background: #7C3AED; }

/* Day-of-week row */
.main-cal__dow-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-muted);
}
.main-cal__dow-cell {
  padding: 8px 0;
  text-align: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--muted-text);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-right: 1px solid var(--border-color);
}
.main-cal__dow-cell:last-child { border-right: none; }
.main-cal__dow-cell--sun { color: #EF4444; }
.main-cal__dow-cell--sat { color: #3B82F6; }

/* Grid */
.main-cal__grid {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.main-cal__week {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border-bottom: 1px solid var(--border-color);
  position: relative;
  min-height: 0;
}
.main-cal__cell {
  padding: 4px;
  border-right: 1px solid var(--border-color);
  transition: background 0.12s;
  position: relative;
}
.main-cal__cell:last-child { border-right: none; }
.main-cal__cell:hover { background: color-mix(in srgb, var(--accent-color) 4%, transparent); }
.main-cal__add {
  position: absolute;
  top: 3px;
  right: 4px;
  width: 18px;
  height: 18px;
  border: none;
  background: rgba(139, 92, 246, 0.14);
  color: var(--accent-color, #8B5CF6);
  border-radius: 50%;
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
  background: var(--accent-color, #8B5CF6);
  color: #fff;
  transform: scale(1.12);
}
.main-cal__cell--today {
  background: color-mix(in srgb, var(--accent-color, #8B5CF6) 6%, transparent);
  box-shadow: inset 2px 0 0 var(--accent-color, #8B5CF6);
}
.main-cal__cell--past { background: color-mix(in srgb, var(--panel-muted) 30%, transparent); }
.main-cal__cell--past .main-cal__date { opacity: 0.55; }
.main-cal__cell--out { background: var(--panel-muted); }
.main-cal__cell--drag-over {
  background: color-mix(in srgb, var(--accent-color, #8B5CF6) 12%, transparent) !important;
  outline: 2px dashed var(--accent-color, #8B5CF6);
  outline-offset: -2px;
}

.main-cal__date {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  padding: 2px 6px;
  border-radius: 4px;
  font-variant-numeric: tabular-nums;
}
.main-cal__date--sun { color: #EF4444; }
.main-cal__date--sat { color: #3B82F6; }
.main-cal__date--out { color: var(--subtle-text); opacity: 0.5; }
.main-cal__date--today {
  background: var(--accent-color, #8B5CF6);
  color: #fff !important;
  font-weight: 800;
  border-radius: 999px;
  min-width: 22px;
  height: 22px;
}

/* Events overlay */
.main-cal__events {
  position: absolute;
  top: 28px;
  left: 0;
  right: 0;
  bottom: 18px;
  pointer-events: none;
  overflow: hidden;
}
.main-cal__event-row {
  position: absolute;
  left: 0;
  right: 0;
  height: 20px;
}
.main-cal__event {
  display: block;
  height: 20px;
  padding: 0 8px;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 20px;
  border-radius: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  pointer-events: auto;
  cursor: pointer;
  background: rgba(139, 92, 246, 0.12);
  color: #5B21B6;
  border: 1px solid rgba(139, 92, 246, 0.2);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: transform 0.1s, box-shadow 0.15s;
}
.main-cal__event:hover { transform: translateY(-1px); box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12); }
.main-cal__event-icon { font-size: 11px; margin-right: 3px; }
.main-cal__event {
  position: relative;
  /* drag-drop 후 위치 전환 (margin-left/width 변경) 부드럽게 */
  transition: margin-left 0.2s cubic-bezier(0.16, 1, 0.3, 1),
              width 0.2s cubic-bezier(0.16, 1, 0.3, 1),
              transform 0.1s, box-shadow 0.15s;
}
.main-cal__event--dragging { opacity: 0.45; }

/* 양쪽 리사이즈 핸들 */
.main-cal__event-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 5px;
  cursor: ew-resize;
  background: transparent;
  z-index: 1;
}
.main-cal__event-handle--start { left: 0; border-top-left-radius: 4px; border-bottom-left-radius: 4px; }
.main-cal__event-handle--end   { right: 0; border-top-right-radius: 4px; border-bottom-right-radius: 4px; }
.main-cal__event-handle:hover { background: rgba(0, 0, 0, 0.18); }

/* +N more */
.main-cal__extras {
  position: absolute;
  bottom: 2px;
  left: 0;
  right: 0;
  height: 16px;
  pointer-events: none;
}
.main-cal__more {
  position: absolute;
  height: 16px;
  font-size: 10px;
  font-weight: 700;
  color: var(--accent-color, #8B5CF6);
  background: transparent;
  border: none;
  cursor: pointer;
  pointer-events: auto;
  border-radius: 3px;
  text-align: left;
  padding: 0 4px;
}
.main-cal__more:hover { background: color-mix(in srgb, var(--accent-color) 10%, transparent); }

/* Hover preview tooltip */
.hover-tip {
  position: fixed;
  transform: translateX(-50%);
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
  z-index: 150;
  pointer-events: none;
  min-width: 200px;
  max-width: 280px;
}
.hover-tip__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.hover-tip__row {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--muted-text);
  padding: 1px 0;
}
.hover-tip__row .material-symbols-outlined { font-size: 13px; }

.hover-fade-enter-active, .hover-fade-leave-active { transition: opacity 0.12s; }
.hover-fade-enter-from, .hover-fade-leave-to { opacity: 0; }
</style>
