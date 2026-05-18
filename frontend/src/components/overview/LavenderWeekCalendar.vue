<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'

const props = defineProps({
  eventsData: { type: Array, default: () => [] },
  anchorDate: { type: Date, required: true },
  currentView: { type: String, default: 'week' },
  isDark: { type: Boolean, default: false },
})
const emit = defineEmits(['update:anchorDate', 'update:currentView', 'event-click', 'today'])

const HOUR_HEIGHT = 120
const HOUR_START = 0
const HOUR_END = 24
const VISIBLE_HOURS = HOUR_END - HOUR_START

function startOfWeek(d) {
  const date = new Date(d)
  const day = date.getDay()
  const diff = day === 0 ? -6 : 1 - day
  date.setDate(date.getDate() + diff)
  date.setHours(0, 0, 0, 0)
  return date
}

const weekStart = computed(() => startOfWeek(props.anchorDate))
const weekDays = computed(() => {
  const start = weekStart.value
  const arr = []
  for (let i = 0; i < 6; i++) {
    const d = new Date(start)
    d.setDate(d.getDate() + i)
    arr.push(d)
  }
  return arr
})

const now = ref(new Date())
let __nowTimer = null
onMounted(() => {
  __nowTimer = setInterval(() => { now.value = new Date() }, 60_000)
})
onBeforeUnmount(() => {
  if (__nowTimer) clearInterval(__nowTimer)
})

const hourLabels = computed(() => {
  const arr = []
  for (let h = HOUR_START; h < HOUR_END; h++) {
    const hour12 = h > 12 ? h - 12 : (h === 0 ? 12 : h)
    const ampm = h >= 12 ? 'PM' : 'AM'
    arr.push({ hour: h, label: `${hour12} ${ampm}` })
  }
  return arr
})

const rangeLabel = computed(() => {
  const start = weekStart.value
  const end = new Date(start)
  end.setDate(end.getDate() + 5)
  return `${start.getMonth() + 1}월 ${start.getDate()}일 — ${end.getDate()}일, ${start.getFullYear()}`
})

function isToday(d) {
  const today = now.value
  return d.getFullYear() === today.getFullYear()
    && d.getMonth() === today.getMonth()
    && d.getDate() === today.getDate()
}

const todayIdx = computed(() => weekDays.value.findIndex(isToday))

const nowY = computed(() => {
  const n = now.value
  const hour = n.getHours() + n.getMinutes() / 60
  return (hour - HOUR_START) * HOUR_HEIGHT
})

const nowTimeLabel = computed(() => {
  const n = now.value
  return `${String(n.getHours()).padStart(2, '0')}:${String(n.getMinutes()).padStart(2, '0')}`
})

const showNowLine = computed(() => {
  const n = now.value
  const hour = n.getHours() + n.getMinutes() / 60
  return hour >= HOUR_START && hour <= HOUR_END && todayIdx.value >= 0
})

function parseEventDate(v) {
  if (!v) return null
  if (v instanceof Date) return v
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}

function eventDayIdx(e) {
  const start = parseEventDate(e.start)
  if (!start) return -1
  return weekDays.value.findIndex((d) =>
    d.getFullYear() === start.getFullYear()
    && d.getMonth() === start.getMonth()
    && d.getDate() === start.getDate(),
  )
}

/* 일자만 있는 이벤트(자정 00:00)는 기본 9AM 1시간 블록으로 표시 */
function effectiveHours(e) {
  const start = parseEventDate(e.start)
  const endRaw = parseEventDate(e.end)
  if (!start) return { sH: HOUR_START, eH: HOUR_START + 1 }
  let sH = start.getHours() + start.getMinutes() / 60
  let eH = endRaw ? endRaw.getHours() + endRaw.getMinutes() / 60 : sH
  const startIsAllDay = sH === 0 && start.getSeconds() === 0
  const endIsAllDay = (eH === 0 || !endRaw) && (!endRaw || endRaw.getSeconds() === 0)
  if (startIsAllDay && endIsAllDay) {
    // 일자만 — 9-10 기본 블록
    sH = 9
    eH = 10
  } else if (eH <= sH) {
    eH = sH + 1
  }
  return { sH, eH }
}

function eventTop(e) {
  const { sH } = effectiveHours(e)
  return Math.max(0, (sH - HOUR_START) * HOUR_HEIGHT)
}

function eventHeight(e) {
  const { sH, eH } = effectiveHours(e)
  return Math.max(28, (eH - sH) * HOUR_HEIGHT - 4)
}

function fmtTime(d) {
  let h = d.getHours()
  const m = d.getMinutes()
  const ampm = h >= 12 ? 'PM' : 'AM'
  if (h === 0) h = 12
  else if (h > 12) h -= 12
  return m === 0 ? `${h}:00 ${ampm}` : `${h}:${String(m).padStart(2, '0')} ${ampm}`
}

function eventTimeLabel(e) {
  const start = parseEventDate(e.start)
  if (!start) return ''
  const { sH, eH } = effectiveHours(e)
  const sBase = new Date(start)
  sBase.setHours(Math.floor(sH), Math.round((sH % 1) * 60), 0, 0)
  const eBase = new Date(start)
  eBase.setHours(Math.floor(eH), Math.round((eH % 1) * 60), 0, 0)
  if (sH === eH) return fmtTime(sBase)
  return `${fmtTime(sBase)} – ${fmtTime(eBase)}`
}

function isEventPast(e) {
  const endRaw = parseEventDate(e.end) ?? parseEventDate(e.start)
  return endRaw && endRaw.getTime() < now.value.getTime()
}

function eventColorClass(e) {
  const t = String(e.type ?? '').toLowerCase()
  if (t === 'task') return 'evt-lime'
  if (t === 'deadline') return 'evt-cream'
  if (t === 'milestone') return 'evt-lavender-soft'
  if (t === 'campaign') return 'evt-lavender'
  return 'evt-lavender-soft'
}

const weekEvents = computed(() => {
  const days = weekDays.value
  if (days.length === 0) return []
  const start = days[0].getTime()
  const end = days[days.length - 1].getTime() + 24 * 60 * 60 * 1000
  return (props.eventsData ?? []).filter((e) => {
    const ed = parseEventDate(e.start)
    return ed && ed.getTime() >= start && ed.getTime() < end
  })
})

function eventsForDay(i) {
  return weekEvents.value.filter((e) => eventDayIdx(e) === i)
}

function shiftWeek(delta) {
  const d = new Date(props.anchorDate)
  d.setDate(d.getDate() + delta * 7)
  emit('update:anchorDate', d)
}
function gotoToday() {
  emit('update:anchorDate', new Date())
  emit('today')
  nextTick(() => scrollToNow(true))
}

const scrollRef = ref(null)
const isTransitioning = ref(false)
const transitionClass = ref('')
let __wheelLock = false

function scrollToNow(smooth = false) {
  if (!scrollRef.value) return
  const top = Math.max(0, nowY.value - scrollRef.value.clientHeight / 2)
  scrollRef.value.scrollTo({ top, behavior: smooth ? 'smooth' : 'auto' })
}

function onWheel(event) {
  if (isTransitioning.value || __wheelLock) return
  const el = scrollRef.value
  if (!el) return

  const atTop = el.scrollTop <= 0
  const atBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 1

  if (atTop && event.deltaY < 0) {
    event.preventDefault()
    __wheelLock = true
    transitionToWeek(-1)
  } else if (atBottom && event.deltaY > 0) {
    event.preventDefault()
    __wheelLock = true
    transitionToWeek(1)
  }
}

function transitionToWeek(delta) {
  isTransitioning.value = true
  transitionClass.value = delta > 0 ? 'is-leaving-up' : 'is-leaving-down'

  setTimeout(() => {
    const newDate = new Date(props.anchorDate)
    newDate.setDate(newDate.getDate() + delta * 7)
    emit('update:anchorDate', newDate)

    nextTick(() => {
      if (scrollRef.value) {
        scrollRef.value.scrollTop = delta > 0 ? 0 : scrollRef.value.scrollHeight
      }
      transitionClass.value = delta > 0 ? 'is-entering-from-bottom' : 'is-entering-from-top'
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          transitionClass.value = ''
        })
      })
      setTimeout(() => {
        isTransitioning.value = false
        __wheelLock = false
      }, 360)
    })
  }, 260)
}

onMounted(() => {
  nextTick(() => {
    scrollToNow(false)
    if (scrollRef.value) {
      scrollRef.value.addEventListener('wheel', onWheel, { passive: false })
    }
  })
})
onBeforeUnmount(() => {
  if (scrollRef.value) {
    scrollRef.value.removeEventListener('wheel', onWheel)
  }
})
watch(() => props.anchorDate, () => {
  if (isTransitioning.value) return
  nextTick(() => scrollToNow(false))
})

function dayHeaderName(d) {
  return ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'][((d.getDay() + 6) % 7)]
}

function onEventClick(e) { emit('event-click', e) }
</script>

<template>
  <main class="lp-week-main" :class="[transitionClass, { 'is-dark': isDark }]">
    <!-- Range nav + view toggle + Today -->
    <div class="lp-main-h">
      <div class="lp-range-nav">
        <button class="lp-arrow" aria-label="이전 주" @click="shiftWeek(-1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <strong class="lp-range-label">{{ rangeLabel }}</strong>
        <button class="lp-arrow" aria-label="다음 주" @click="shiftWeek(1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
      </div>
      <div class="lp-main-right">
        <button class="lp-today-btn" @click="gotoToday">Today</button>
      </div>
    </div>

    <!-- Week header -->
    <div class="lp-week-header">
      <div class="lp-gmt">GMT+9</div>
      <div
        v-for="(d, i) in weekDays"
        :key="i"
        class="lp-day-h"
        :class="{ 'is-today': isToday(d) }"
      >
        <div class="lp-day-num">{{ d.getDate() }}</div>
        <div class="lp-day-dow">{{ dayHeaderName(d) }}</div>
      </div>
    </div>

    <!-- Scroll body -->
    <div class="lp-week-body">
      <div ref="scrollRef" class="lp-week-scroll">
        <div class="lp-week-inner" :style="{ height: (VISIBLE_HOURS * HOUR_HEIGHT) + 'px' }">
          <!-- Time gutter -->
          <div class="lp-time-gutter">
            <span
              v-for="hl in hourLabels"
              :key="hl.hour"
              class="lp-hour-label"
              :style="{ top: ((hl.hour - HOUR_START) * HOUR_HEIGHT) + 'px' }"
            >{{ hl.label }}</span>
          </div>

          <!-- Day columns -->
          <div
            v-for="(d, i) in weekDays"
            :key="i"
            class="lp-day-col"
            :class="{ 'is-today': isToday(d) }"
          >
            <div
              v-for="hl in hourLabels"
              :key="'l-' + hl.hour"
              class="lp-hour-line"
              :style="{ top: ((hl.hour - HOUR_START) * HOUR_HEIGHT) + 'px' }"
            ></div>
            <article
              v-for="e in eventsForDay(i)"
              :key="e.id"
              class="lp-ev"
              :class="[eventColorClass(e), { 'is-past': isEventPast(e) }]"
              :style="{ top: eventTop(e) + 'px', height: eventHeight(e) + 'px' }"
              tabindex="0"
              @click="onEventClick(e)"
              @keydown.enter="onEventClick(e)"
            >
              <div class="lp-ev-title">{{ e.title }}</div>
              <div class="lp-ev-time">{{ eventTimeLabel(e) }}</div>
            </article>
          </div>

          <!-- Now-line (전체 가로) -->
          <template v-if="showNowLine">
            <div class="lp-now-line" :style="{ top: nowY + 'px' }"></div>
            <span class="lp-now-tag" :style="{ top: nowY + 'px' }">{{ nowTimeLabel }}</span>
            <span class="lp-now-dot" :style="{ top: nowY + 'px' }"></span>
          </template>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.lp-week-main {
  --lp-bg: #F5F1FA;
  --lp-surface: #FFFFFF;
  --lp-surface-soft: #EEE6F7;
  --lp-primary: #B79BD9;
  --lp-primary-strong: #6F5A9B;
  --lp-primary-deep: #3F3463;
  --lp-lime: #D8EB75;
  --lp-card-lavender-1: #DDD2EE;
  --lp-card-lavender-2: #B0A4DA;
  --lp-card-cream: #F5EDD8;
  --lp-text: #2A2440;
  --lp-text-muted: #6B6582;
  --lp-text-faint: #9991AE;
  --lp-border: #E5DDF0;
  --lp-coral: #C04438;

  background: var(--lp-surface);
  border-radius: 22px;
  padding: 22px 24px 8px;
  display: flex;
  flex-direction: column;
  min-height: 600px;
  box-shadow: 0 1px 2px rgba(63,52,99,.04), 0 6px 18px rgba(63,52,99,.06);
  font-family: 'Pretendard Variable', 'Pretendard', -apple-system, sans-serif;
  font-feature-settings: 'tnum' 1, 'ss01' 1;
  color: var(--lp-text);
}

.lp-main-h {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 12px;
}
.lp-range-nav { display: inline-flex; align-items: center; gap: 12px; }
.lp-arrow {
  width: 28px; height: 28px;
  border-radius: 999px;
  background: var(--lp-surface-soft);
  border: 0;
  color: var(--lp-primary-deep);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background .15s;
}
.lp-arrow:hover { background: #E0D2F0; }
.lp-range-label {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--lp-text);
}
.lp-main-right { display: inline-flex; align-items: center; gap: 10px; }
.lp-view-toggle {
  display: inline-flex;
  background: var(--lp-surface-soft);
  border-radius: 999px;
  padding: 3px;
  gap: 2px;
}
.lp-view-toggle button {
  padding: 5px 14px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--lp-text-muted);
  background: transparent;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  transition: background .15s, color .15s;
}
.lp-view-toggle button:hover { color: var(--lp-text); }
.lp-view-toggle button.is-on {
  background: var(--lp-surface);
  color: var(--lp-text);
  box-shadow: 0 1px 3px rgba(63,52,99,.10);
}
.lp-today-btn {
  background: var(--lp-primary-deep);
  color: #fff;
  border: 0;
  padding: 7px 18px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}
.lp-today-btn:hover { background: #4F4275; }

/* Week header row */
.lp-week-header {
  display: grid;
  grid-template-columns: 56px repeat(6, 1fr);
  border-bottom: 1px solid var(--lp-border);
  background: var(--lp-surface);
  position: sticky;
  top: 0;
  z-index: 2;
}
.lp-gmt {
  font-size: 9.5px;
  font-weight: 600;
  color: var(--lp-text-faint);
  padding: 12px 6px 14px;
  text-align: center;
  border-right: 1px solid var(--lp-border);
  letter-spacing: 0.02em;
  align-self: end;
}
.lp-day-h {
  padding: 12px 4px 14px;
  text-align: center;
  border-right: 1px solid var(--lp-border);
  position: relative;
}
.lp-day-h:last-child { border-right: 0; }
.lp-day-h.is-today {
  background: linear-gradient(180deg, rgba(124, 58, 237, 0.32) 0%, rgba(124, 58, 237, 0.10) 100%);
  position: relative;
}
.lp-day-h.is-today::after {
  content: '';
  position: absolute;
  left: 4px;
  right: 4px;
  bottom: -1.5px;
  height: 3px;
  background: linear-gradient(90deg, #A78BFA 0%, #7C3AED 50%, #A78BFA 100%);
  border-radius: 2px;
  box-shadow: 0 2px 10px rgba(124, 58, 237, 0.55);
}
.lp-day-num {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--lp-text);
}
.lp-day-h.is-today .lp-day-num { color: #5B21B6; font-weight: 800; }
.lp-day-dow {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--lp-text-faint);
  letter-spacing: 0.04em;
  margin-top: 2px;
}
.lp-day-h.is-today .lp-day-dow { color: #6D28D9; font-weight: 700; }

/* Scroll body */
.lp-week-body {
  position: relative;
  overflow: hidden;
  height: 480px;
  flex: 1;
}
.lp-week-scroll {
  height: 100%;
  overflow-y: auto;
  position: relative;
  scrollbar-width: none;          /* Firefox */
  -ms-overflow-style: none;       /* IE/legacy Edge */
}
.lp-week-scroll::-webkit-scrollbar { width: 0; height: 0; display: none; }

.lp-week-inner {
  position: relative;
  display: grid;
  grid-template-columns: 56px repeat(6, 1fr);
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, opacity;
}

.lp-week-header {
  transition: opacity 0.28s cubic-bezier(0.4, 0, 0.2, 1), transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.lp-week-main.is-leaving-up .lp-week-inner {
  transform: translateY(-36px);
  opacity: 0;
}
.lp-week-main.is-leaving-down .lp-week-inner {
  transform: translateY(36px);
  opacity: 0;
}
.lp-week-main.is-entering-from-bottom .lp-week-inner {
  transform: translateY(36px);
  opacity: 0;
  transition: none;
}
.lp-week-main.is-entering-from-top .lp-week-inner {
  transform: translateY(-36px);
  opacity: 0;
  transition: none;
}

.lp-week-main.is-leaving-up .lp-week-header,
.lp-week-main.is-leaving-down .lp-week-header { opacity: 0.35; }
.lp-week-main.is-entering-from-top .lp-week-header,
.lp-week-main.is-entering-from-bottom .lp-week-header {
  opacity: 0.35;
  transition: none;
}

.lp-time-gutter {
  position: relative;
  border-right: 1px solid var(--lp-border);
}
.lp-hour-label {
  position: absolute;
  right: 8px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--lp-text-faint);
  transform: translateY(-50%);
  font-variant-numeric: tabular-nums;
}

.lp-day-col {
  position: relative;
  border-right: 1px solid var(--lp-border);
}
.lp-day-col:last-child { border-right: 0; }
.lp-day-col.is-today {
  background: linear-gradient(180deg,
    rgba(124, 58, 237, 0.18) 0%,
    rgba(139, 92, 246, 0.10) 35%,
    rgba(167, 139, 250, 0.05) 100%);
  box-shadow:
    inset 2px 0 0 rgba(124, 58, 237, 0.55),
    inset -2px 0 0 rgba(124, 58, 237, 0.55);
}

.lp-hour-line {
  position: absolute;
  left: 0;
  right: 0;
  border-top: 1px solid var(--lp-border);
  pointer-events: none;
}

/* Event block */
.lp-ev {
  position: absolute;
  left: 6px;
  right: 6px;
  border-radius: 12px;
  padding: 9px 11px 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform .15s, box-shadow .15s;
  outline: none;
  z-index: 3;
}
.lp-ev:hover, .lp-ev:focus-visible {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(63,52,99,.20);
  z-index: 4;
}
.evt-lime { background: var(--lp-lime); color: var(--lp-primary-deep); }
.evt-lime .lp-ev-time { color: var(--lp-primary-strong); }
.evt-lavender { background: var(--lp-card-lavender-2); color: #fff; }
.evt-lavender .lp-ev-time { color: rgba(255,255,255,.86); }
.evt-lavender-soft { background: var(--lp-card-lavender-1); color: var(--lp-primary-deep); }
.evt-lavender-soft .lp-ev-time { color: var(--lp-primary-strong); }
.evt-cream { background: var(--lp-card-cream); color: var(--lp-primary-deep); }
.evt-cream .lp-ev-time { color: var(--lp-primary-strong); }

.lp-ev-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: -0.005em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.lp-ev-time {
  font-size: 10px;
  font-weight: 500;
  margin-top: 2px;
  font-variant-numeric: tabular-nums;
}

.lp-ev.is-past { opacity: 0.55; }
.lp-ev.is-past::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: repeating-linear-gradient(135deg, transparent 0 6px, rgba(255,255,255,.18) 6px 8px);
  border-radius: 12px;
}

/* Now-line */
.lp-now-line {
  position: absolute;
  left: 56px;
  right: 0;
  height: 0;
  border-top: 1.5px solid var(--lp-coral);
  z-index: 5;
  pointer-events: none;
  transform: translateY(-0.75px);
}
.lp-now-tag {
  position: absolute;
  left: 6px;
  background: var(--lp-coral);
  color: #fff;
  font-size: 9.5px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  z-index: 7;
  transform: translateY(-50%);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.lp-now-dot {
  position: absolute;
  left: 50px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--lp-coral);
  z-index: 6;
  transform: translate(0, -5px);
}

@media (max-width: 720px) {
  .lp-week-main { padding: 14px 16px 8px; border-radius: 16px; }
  .lp-range-label { font-size: 15px; }
}

/* ============================================ */
/*  Dark Theme (2026 trend: deep dusk + violet) */
/* ============================================ */
.lp-week-main.is-dark {
  --lp-bg: #0F0D1A;
  --lp-surface: #16131F;
  --lp-surface-soft: #221D32;
  --lp-primary: #A78BFA;
  --lp-primary-strong: #C4B5FD;
  --lp-primary-deep: #E9D5FF;
  --lp-lime: #C4DD66;
  --lp-card-lavender-1: #2E2748;
  --lp-card-lavender-2: #6B5BB5;
  --lp-card-cream: #3A3122;
  --lp-text: #ECE7F7;
  --lp-text-muted: #A9A2C0;
  --lp-text-faint: #6E687F;
  --lp-border: #2A2440;
  --lp-coral: #F87171;

  background: var(--lp-surface);
  box-shadow: 0 1px 2px rgba(0,0,0,.4), 0 8px 24px rgba(0,0,0,.32);
}

/* Header chrome */
.lp-week-main.is-dark .lp-arrow {
  background: var(--lp-surface-soft);
  color: var(--lp-primary-strong);
}
.lp-week-main.is-dark .lp-arrow:hover { background: #2E2748; }
.lp-week-main.is-dark .lp-range-label { color: var(--lp-text); }
.lp-week-main.is-dark .lp-view-toggle { background: var(--lp-surface-soft); }
.lp-week-main.is-dark .lp-view-toggle button { color: var(--lp-text-muted); }
.lp-week-main.is-dark .lp-view-toggle button:hover { color: var(--lp-text); }
.lp-week-main.is-dark .lp-view-toggle button.is-on {
  background: #2E2748;
  color: var(--lp-primary-deep);
  box-shadow: 0 1px 3px rgba(0,0,0,.35);
}
.lp-week-main.is-dark .lp-today-btn {
  background: #7C3AED;
  color: #fff;
}
.lp-week-main.is-dark .lp-today-btn:hover { background: #8B5CF6; }

/* Week grid */
.lp-week-main.is-dark .lp-week-header { background: var(--lp-surface); }
.lp-week-main.is-dark .lp-gmt { color: var(--lp-text-faint); }
.lp-week-main.is-dark .lp-day-h { color: var(--lp-text); }
.lp-week-main.is-dark .lp-day-num { color: var(--lp-text); }
.lp-week-main.is-dark .lp-day-dow { color: var(--lp-text-faint); }
.lp-week-main.is-dark .lp-hour-label { color: var(--lp-text-faint); }
.lp-week-main.is-dark .lp-hour-line { border-top-color: rgba(167, 139, 250, 0.10); }
.lp-week-main.is-dark .lp-time-gutter,
.lp-week-main.is-dark .lp-day-col { border-right-color: rgba(167, 139, 250, 0.10); }

/* Today highlight (dark) — brighter violet over dark canvas */
.lp-week-main.is-dark .lp-day-h.is-today {
  background: linear-gradient(180deg, rgba(167, 139, 250, 0.42) 0%, rgba(167, 139, 250, 0.12) 100%);
}
.lp-week-main.is-dark .lp-day-h.is-today::after {
  background: linear-gradient(90deg, #C4B5FD 0%, #A78BFA 50%, #C4B5FD 100%);
  box-shadow: 0 2px 14px rgba(167, 139, 250, 0.85);
}
.lp-week-main.is-dark .lp-day-h.is-today .lp-day-num { color: #F3E8FF; font-weight: 800; }
.lp-week-main.is-dark .lp-day-h.is-today .lp-day-dow { color: #C4B5FD; font-weight: 700; }
.lp-week-main.is-dark .lp-day-col.is-today {
  background: linear-gradient(180deg,
    rgba(167, 139, 250, 0.24) 0%,
    rgba(167, 139, 250, 0.12) 40%,
    rgba(167, 139, 250, 0.05) 100%);
  box-shadow:
    inset 2px 0 0 rgba(167, 139, 250, 0.75),
    inset -2px 0 0 rgba(167, 139, 250, 0.75);
}

/* Scrollbar */
.lp-week-main.is-dark .lp-week-scroll::-webkit-scrollbar-thumb {
  background: rgba(167, 139, 250, 0.22);
}
.lp-week-main.is-dark .lp-week-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(167, 139, 250, 0.38);
}

/* Event blocks — re-tune for dark canvas */
.lp-week-main.is-dark .evt-lime {
  background: #B6D650;
  color: #1A1A1F;
}
.lp-week-main.is-dark .evt-lime .lp-ev-time { color: #2E2A1A; }
.lp-week-main.is-dark .evt-lavender {
  background: #6B5BB5;
  color: #fff;
}
.lp-week-main.is-dark .evt-lavender .lp-ev-time { color: rgba(255,255,255,.78); }
.lp-week-main.is-dark .evt-lavender-soft {
  background: #3A3160;
  color: #E9D5FF;
}
.lp-week-main.is-dark .evt-lavender-soft .lp-ev-time { color: #C4B5FD; }
.lp-week-main.is-dark .evt-cream {
  background: #4A3F2B;
  color: #F5EDD8;
}
.lp-week-main.is-dark .evt-cream .lp-ev-time { color: #D6C9A1; }
.lp-week-main.is-dark .lp-ev:hover,
.lp-week-main.is-dark .lp-ev:focus-visible {
  box-shadow: 0 4px 16px rgba(0,0,0,.5), 0 0 0 1px rgba(167, 139, 250, 0.3);
}
.lp-week-main.is-dark .lp-ev.is-past::after {
  background: repeating-linear-gradient(135deg, transparent 0 6px, rgba(0,0,0,.32) 6px 8px);
}

/* Now line */
.lp-week-main.is-dark .lp-now-line { border-top-color: #F87171; }
.lp-week-main.is-dark .lp-now-tag { background: #F87171; color: #1A0F0F; }
.lp-week-main.is-dark .lp-now-dot { background: #F87171; }
</style>
