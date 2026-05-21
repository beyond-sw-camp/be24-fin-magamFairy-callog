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

/* ═══ 구글 캘린더식 레인(lane) 레이아웃 ═══
 *  - 멀티-데이/종일 이벤트 → 컬럼을 가로지르는 막대(bar)
 *  - 시간 단일일 이벤트 → "● 오전 10:13 제목" 한 줄
 *  - 같은 주 안에서 레인을 공유해 막대가 다른 일정과 세로로 겹치지 않게 배치
 *  - 레인 초과분은 컬럼별 "+N 더보기"
 */
const HEADER_OFFSET = 30   // 날짜 숫자 영역 높이
const LANE_H = 22          // 레인 한 줄 높이
const MAX_LANES = 4        // 보이는 레인 수 (초과 → +N 더보기)

function dateStr(iso) { return (iso ?? '').slice(0, 10) }

function spanDays(e) {
  const s = new Date(dateStr(e.start))
  const en = new Date(dateStr(e.end) || dateStr(e.start))
  if (Number.isNaN(s.getTime()) || Number.isNaN(en.getTime())) return 1
  return Math.round((en - s) / 86400000) + 1
}
// 종일성: 시작이 자정이고 종료가 없거나 하루 끝(23:59/00:00)
function isAllDayLike(e) {
  const s = e.start ?? ''
  const en = e.end ?? s
  const sTime = s.includes('T') ? s.slice(11, 19) : ''
  const eTime = en.includes('T') ? en.slice(11, 19) : ''
  const startMidnight = sTime === '' || sTime === '00:00:00'
  const endEod = eTime === '' || eTime === '23:59:59' || eTime === '00:00:00'
  return startMidnight && endEod
}
function isBarEvent(e) { return spanDays(e) > 1 || isAllDayLike(e) }

function fmtKoTime(iso) {
  if (!iso || !iso.includes('T')) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const h = d.getHours()
  const m = d.getMinutes()
  const period = h < 12 ? '오전' : '오후'
  let h12 = h % 12
  if (h12 === 0) h12 = 12
  return m === 0 ? `${period} ${h12}시` : `${period} ${h12}:${String(m).padStart(2, '0')}`
}
function displayTitle(ev) {
  let t = ev.title ?? ''
  if (ev.type === 'task') t = t.replace(/^✅\s*/, '')   // 시간 줄은 체크 이모지 제거(깔끔)
  return t
}
function dotColor(ev) { return ev.customColor || 'var(--lp-primary, #8B5CF6)' }
function barBg(ev) {
  if (ev.customColor) return ev.customColor
  if (ev.type === 'milestone') return 'var(--lp-card-lavender-2, #B0A4DA)'
  return 'var(--lp-card-lavender-1, #DDD2EE)'
}

// 위치 계산 헬퍼
function itemTop(it) { return HEADER_OFFSET + it.lane * LANE_H }
function itemLeftPct(it) { return (it.startCol / 7) * 100 }
function itemWidthPct(it) { return ((it.endCol - it.startCol + 1) / 7) * 100 }
function moreTop() { return HEADER_OFFSET + MAX_LANES * LANE_H }

// 그날 일정 전체 (모달/더보기용)
function eventsOnDay(iso) {
  return events.value.filter((e) => {
    const s = dateStr(e.start)
    if (!s) return false
    const en = dateStr(e.end) || s
    return iso >= s && iso <= en
  })
}

// 캘린더 격자 + 주별 레인 배치
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
    const weekStart = days[0].date
    const weekEnd = days[6].date

    // 이 주에 걸치는 이벤트 → 컬럼 범위/종류 계산
    const inWeek = []
    for (const e of events.value) {
      const s = dateStr(e.start)
      if (!s) continue
      const en = dateStr(e.end) || s
      if (s > weekEnd || en < weekStart) continue
      const startIdx = s <= weekStart ? 0 : days.findIndex((d) => d.date === s)
      const endIdx = en >= weekEnd ? 6 : days.findIndex((d) => d.date === en)
      const startCol = startIdx < 0 ? 0 : startIdx
      const endCol = endIdx < 0 ? 6 : Math.max(startCol, endIdx)
      inWeek.push({
        ev: e,
        bar: isBarEvent(e),
        startCol,
        endCol,
        continuesLeft: s < weekStart,
        continuesRight: en > weekEnd,
        isStartSegment: s >= weekStart,
      })
    }

    // 정렬: 막대 먼저(시작 컬럼 → 긴 것 우선), 그다음 시간 이벤트(시작 시각순)
    inWeek.sort((a, b) => {
      if (a.bar !== b.bar) return a.bar ? -1 : 1
      if (a.bar) {
        if (a.startCol !== b.startCol) return a.startCol - b.startCol
        return (b.endCol - b.startCol) - (a.endCol - a.startCol)
      }
      return (a.ev.start ?? '').localeCompare(b.ev.start ?? '')
    })

    // 레인 배정 — 같은 컬럼 범위가 겹치지 않는 가장 낮은 레인
    const lanes = []
    for (const it of inWeek) {
      let L = 0
      while (lanes[L] && lanes[L].some((seg) => !(it.endCol < seg[0] || it.startCol > seg[1]))) L++
      if (!lanes[L]) lanes[L] = []
      lanes[L].push([it.startCol, it.endCol])
      it.lane = L
    }

    // 가시 레인 / 컬럼별 초과 개수
    const extraByCol = [0, 0, 0, 0, 0, 0, 0]
    const items = []
    for (const it of inWeek) {
      if (it.lane < MAX_LANES) {
        items.push(it)
      } else {
        for (let c = it.startCol; c <= it.endCol; c++) extraByCol[c] += 1
      }
    }
    weeks.push({ days, items, extraByCol })
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
        <!-- 배경 셀 (날짜 숫자 + 드롭 타깃) -->
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
            <button
              v-if="day.isCurrentMonth"
              type="button"
              class="main-cal__add"
              :title="`${day.date} 일정 추가`"
              @click.stop="emit('day-click', { date: day.date, event: $event })"
            >+</button>
          </div>
        </div>

        <!-- 이벤트 오버레이 (막대 + 시간 줄) -->
        <div class="main-cal__lanes">
          <button
            v-for="it in week.items"
            :key="it.ev.id"
            type="button"
            class="main-cal__ev"
            :class="[
              it.bar ? 'main-cal__ev--bar' : 'main-cal__ev--timed',
              {
                'is-cont-left': it.continuesLeft,
                'is-cont-right': it.continuesRight,
                'main-cal__ev--dragging': dragState?.event?.id === it.ev.id,
              },
            ]"
            :style="{
              top: itemTop(it) + 'px',
              left: 'calc(' + itemLeftPct(it) + '% + 4px)',
              width: 'calc(' + itemWidthPct(it) + '% - 8px)',
              ...(it.bar ? { background: barBg(it.ev) } : {}),
            }"
            draggable="true"
            @click.stop="emit('event-click', it.ev)"
            @mouseenter="onEventHover(it.ev, $event)"
            @mouseleave="onEventLeave"
            @dragstart="onEventDragStart(it.ev, $event, 'move')"
            @dragend="onDragEnd"
          >
            <template v-if="it.bar">
              <span
                v-if="it.isStartSegment && !isAllDayLike(it.ev)"
                class="main-cal__ev-time"
              >{{ fmtKoTime(it.ev.start) }}</span>
              <span class="main-cal__ev-title">{{ it.ev.title }}</span>
            </template>
            <template v-else>
              <span class="main-cal__ev-dot" :style="{ background: dotColor(it.ev) }" />
              <span class="main-cal__ev-time">{{ fmtKoTime(it.ev.start) }}</span>
              <span class="main-cal__ev-title">{{ displayTitle(it.ev) }}</span>
            </template>
          </button>

          <!-- 컬럼별 "+N 더보기" -->
          <button
            v-for="(extra, di) in week.extraByCol"
            v-show="extra > 0"
            :key="'more-' + di"
            type="button"
            class="main-cal__more"
            :style="{ top: moreTop() + 'px', left: 'calc(' + (di / 7) * 100 + '% + 6px)', width: 'calc(' + (1 / 7) * 100 + '% - 10px)' }"
            @click.stop="emit('more-click', { date: week.days[di].date, events: eventsOnDay(week.days[di].date) })"
          >+{{ extra }} 더보기</button>
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

/* ═══ Grid: 구글식 7×N 플러시 셀 (얇은 격자선) ═══ */
.main-cal__grid {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  border-top: 1px solid var(--lp-border);
  border-left: 1px solid var(--lp-border);
  border-radius: 10px;
}
.main-cal__week {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-template-rows: 1fr;
  position: relative;
  min-height: 132px;
}

.main-cal__cell {
  position: relative;
  border-right: 1px solid var(--lp-border);
  border-bottom: 1px solid var(--lp-border);
  background: var(--lp-surface);
  transition: background 0.15s;
  overflow: hidden;
}
.main-cal__cell--today { background: var(--lp-lime-soft, rgba(216,235,117,.16)); }
.main-cal__cell--past .main-cal__date { opacity: 0.55; }
.main-cal__cell--out .main-cal__date { opacity: 0.4; }
.main-cal__cell--drag-over { box-shadow: inset 0 0 0 2px var(--lp-primary-strong); background: color-mix(in srgb, var(--lp-primary) 10%, transparent); }

.main-cal__cell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 8px 0;
  position: relative;
  z-index: 1;
}
.main-cal__add {
  width: 20px; height: 20px;
  border: 0;
  background: color-mix(in srgb, var(--lp-primary) 22%, transparent);
  color: var(--lp-primary-strong);
  border-radius: 999px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, background 0.15s, transform 0.1s;
  font-size: 14px; font-weight: 700; line-height: 1;
  display: flex; align-items: center; justify-content: center;
  z-index: 5; padding: 0;
}
.main-cal__cell:hover .main-cal__add { opacity: 1; }
.main-cal__add:hover { background: var(--lp-button-bg); color: #fff; transform: scale(1.10); }

/* Date number */
.main-cal__date {
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: var(--lp-text);
  letter-spacing: -0.01em; font-variant-numeric: tabular-nums;
  padding: 0; border-radius: 999px;
  width: auto; height: 22px; min-width: 22px;
  transition: background .2s, color .2s;
}
.main-cal__date--sun { color: #C0837A; }
.main-cal__date--sat { color: var(--lp-primary-strong); }
.main-cal__date--out { color: var(--lp-text-faint); }
.main-cal__date--today {
  background: var(--lp-button-bg); color: #fff !important; font-weight: 700;
  width: 26px; height: 26px; min-width: 26px; border-radius: 999px;
  box-shadow: 0 2px 6px rgba(63, 52, 99, 0.20);
}

/* ═══ 이벤트 오버레이 — 막대(bar) + 시간 줄(timed) ═══ */
.main-cal__lanes { position: absolute; inset: 0; pointer-events: none; z-index: 3; }
.main-cal__ev {
  position: absolute; height: 20px;
  display: flex; align-items: center; gap: 5px;
  padding: 0 7px;
  font-size: 11px; font-weight: 600; line-height: 20px;
  border: 0; border-radius: 5px; cursor: pointer; pointer-events: auto;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  background: transparent; color: var(--lp-text);
  transition: filter 0.12s;
}
.main-cal__ev:hover { filter: brightness(0.95); }
.main-cal__ev--bar { color: #2D2649; box-shadow: 0 1px 1px rgba(63,52,99,.10); }
.main-cal__ev--bar.is-cont-left { border-top-left-radius: 0; border-bottom-left-radius: 0; }
.main-cal__ev--bar.is-cont-right { border-top-right-radius: 0; border-bottom-right-radius: 0; }
.main-cal__ev--dragging { opacity: 0.45; }
.main-cal__ev-dot { width: 7px; height: 7px; border-radius: 999px; flex-shrink: 0; }
.main-cal__ev-time { font-weight: 700; flex-shrink: 0; }
.main-cal__ev--timed .main-cal__ev-time { color: var(--lp-text-muted); }
.main-cal__ev--bar .main-cal__ev-time { opacity: 0.85; }
.main-cal__ev-title { overflow: hidden; text-overflow: ellipsis; }

/* +N 더보기 */
.main-cal__more {
  position: absolute; height: 18px;
  font-size: 11px; font-weight: 600;
  color: var(--lp-text-muted); background: transparent; border: 0;
  cursor: pointer; pointer-events: auto;
  padding: 0 4px; text-align: left;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.main-cal__more:hover { color: var(--lp-primary-deep); }

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
  .main-cal__cell, .main-cal__ev { transition: none; }
}

@media (max-width: 720px) {
  .main-cal { padding: 16px 14px 12px; border-radius: 18px; }
  .main-cal__title, .lp-range-label { font-size: 15px; }
  .main-cal__week { min-height: 110px; }
  .main-cal__date { font-size: 12px; }
  .main-cal__date--today { width: 24px; height: 24px; min-width: 24px; }
}
</style>
