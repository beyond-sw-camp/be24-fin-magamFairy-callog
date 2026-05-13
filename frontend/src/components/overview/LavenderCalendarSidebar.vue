<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  anchorDate: { type: Date, required: true },
  tasks: { type: Array, default: () => [] },
  deadlines: { type: Array, default: () => [] },
  milestones: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:anchorDate', 'task-toggle', 'event-click'])

/* 미니 월간 캘린더 — 현재 월 */
const viewedMonth = ref(new Date(props.anchorDate))
watch(() => props.anchorDate, (val) => {
  // anchorDate 가 새로운 월로 이동했을 때만 viewedMonth도 따라가게
  if (val.getFullYear() !== viewedMonth.value.getFullYear()
    || val.getMonth() !== viewedMonth.value.getMonth()) {
    viewedMonth.value = new Date(val)
  }
})

const monthLabel = computed(() =>
  `${viewedMonth.value.getFullYear()}년 ${viewedMonth.value.getMonth() + 1}월`,
)

function shiftMonth(delta) {
  const d = new Date(viewedMonth.value)
  d.setMonth(d.getMonth() + delta)
  viewedMonth.value = d
}

const miniDays = computed(() => {
  const m = viewedMonth.value
  const year = m.getFullYear()
  const month = m.getMonth()
  const firstWeekday = new Date(year, month, 1).getDay()
  const startOffset = firstWeekday === 0 ? -6 : 1 - firstWeekday
  const startDay = new Date(year, month, 1 + startOffset)
  const arr = []
  for (let i = 0; i < 42; i++) {
    const d = new Date(startDay)
    d.setDate(d.getDate() + i)
    arr.push({
      date: d,
      day: d.getDate(),
      inMonth: d.getMonth() === month,
    })
  }
  return arr
})

function startOfWeek(d) {
  const date = new Date(d)
  const day = date.getDay()
  const diff = day === 0 ? -6 : 1 - day
  date.setDate(date.getDate() + diff)
  date.setHours(0, 0, 0, 0)
  return date
}

const weekStartTs = computed(() => startOfWeek(props.anchorDate).getTime())
const weekEndTs = computed(() => weekStartTs.value + 6 * 24 * 60 * 60 * 1000)

function isInCurrentWeek(d) {
  const t = new Date(d).setHours(0, 0, 0, 0)
  return t >= weekStartTs.value && t < weekEndTs.value
}

function isMiniToday(d) {
  const today = new Date()
  return d.getFullYear() === today.getFullYear()
    && d.getMonth() === today.getMonth()
    && d.getDate() === today.getDate()
}

function selectDay(d) {
  emit('update:anchorDate', new Date(d))
}

/* My Calendar — 내 task 임박 4개 */
const myTasks = computed(() => {
  return (props.tasks ?? [])
    .filter((t) => String(t.status ?? '').toUpperCase() !== 'DONE')
    .sort((a, b) => {
      const da = a.dueDate ? new Date(a.dueDate).getTime() : Infinity
      const db = b.dueDate ? new Date(b.dueDate).getTime() : Infinity
      return da - db
    })
    .slice(0, 4)
    .map((t, i) => ({
      id: t.idx,
      title: t.name,
      done: false,
      cat: ['cat-lime', 'cat-lavender', 'cat-cream', 'cat-rose'][i % 4],
      raw: t,
    }))
})

/* Other Calendar — 마감/마일스톤 */
const otherItems = computed(() => {
  const arr = []
  ;(props.deadlines ?? []).forEach((d) => {
    arr.push({
      id: `dl-${d.campaignIdx ?? d.campaignId}`,
      title: `${d.campaignName ?? '캠페인'} 모집 마감`,
      cat: 'cat-rose',
      date: d.recruitDeadline,
      raw: d,
      type: 'deadline',
    })
  })
  ;(props.milestones ?? []).forEach((m) => {
    arr.push({
      id: `ms-${m.idx}`,
      title: m.name ?? '마일스톤',
      cat: 'cat-lavender',
      date: m.endDate ?? m.startDate,
      raw: m,
      type: 'milestone',
    })
  })
  return arr
    .filter((x) => x.date)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
    .slice(0, 4)
})

function onToggleTask(item) {
  emit('task-toggle', item)
}
function onItemClick(item) {
  emit('event-click', item)
}
</script>

<template>
  <aside class="lp-sidebar" aria-label="사이드바">
    <!-- 미니 캘린더 -->
    <div class="lp-mini">
      <div class="lp-mini-h">
        <strong>{{ monthLabel }}</strong>
        <span class="lp-mini-arrows">
          <button class="lp-mini-arrow" aria-label="이전 달" @click="shiftMonth(-1)">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </button>
          <button class="lp-mini-arrow" aria-label="다음 달" @click="shiftMonth(1)">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </button>
        </span>
      </div>
      <div class="lp-mini-grid">
        <span class="lp-mini-dow">M</span>
        <span class="lp-mini-dow">T</span>
        <span class="lp-mini-dow">W</span>
        <span class="lp-mini-dow">T</span>
        <span class="lp-mini-dow">F</span>
        <span class="lp-mini-dow">S</span>
        <span class="lp-mini-dow">S</span>
        <button
          v-for="(d, i) in miniDays"
          :key="i"
          type="button"
          class="lp-mini-day"
          :class="{
            'off': !d.inMonth,
            'in-view': d.inMonth && isInCurrentWeek(d.date),
            'is-today': isMiniToday(d.date),
          }"
          @click="selectDay(d.date)"
        >{{ d.day }}</button>
      </div>
    </div>

    <!-- My Calendar -->
    <div class="lp-panel">
      <div class="lp-panel-h">
        <strong>My Calendar</strong>
        <button class="lp-panel-action">체크리스트</button>
      </div>
      <div v-if="myTasks.length === 0" class="lp-panel-empty">표시할 업무가 없습니다.</div>
      <label
        v-for="t in myTasks"
        :key="t.id"
        class="lp-item"
        :class="{ 'is-done': t.done }"
      >
        <input
          type="checkbox"
          class="lp-check"
          :checked="t.done"
          @change="onToggleTask(t)"
        />
        <span class="lp-cat" :class="t.cat"></span>
        <span class="lp-item-title">{{ t.title }}</span>
      </label>
    </div>

    <!-- Other Calendar -->
    <div class="lp-panel">
      <div class="lp-panel-h">
        <strong>Other Calendar</strong>
        <button class="lp-panel-action">+ Add Task</button>
      </div>
      <div v-if="otherItems.length === 0" class="lp-panel-empty">표시할 일정이 없습니다.</div>
      <div
        v-for="item in otherItems"
        :key="item.id"
        class="lp-item lp-item--readonly"
        @click="onItemClick(item)"
      >
        <span class="lp-cat" :class="item.cat"></span>
        <span class="lp-item-title">{{ item.title }}</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.lp-sidebar {
  --lp-violet-bg: #3F3463;
  --lp-violet-deep: #2D2649;
  --lp-violet-line: #564A7D;
  --lp-text-on-violet: #ECE5F8;
  --lp-text-on-violet-faint: #A89BC4;
  --lp-primary: #B79BD9;
  --lp-primary-deep: #3F3463;
  --lp-lime: #D8EB75;
  --lp-card-lavender-1: #DDD2EE;

  background: var(--lp-violet-bg);
  border-radius: 22px;
  padding: 20px 18px;
  display: flex;
  flex-direction: column;
  gap: 22px;
  color: var(--lp-text-on-violet);
  font-family: 'Pretendard Variable', 'Pretendard', -apple-system, sans-serif;
  font-feature-settings: 'tnum' 1, 'ss01' 1;
  overflow: hidden;
  height: 100%;
  box-sizing: border-box;
}

/* 미니 캘린더 */
.lp-mini {
  background: var(--lp-violet-deep);
  border-radius: 16px;
  padding: 14px 14px 12px;
}
.lp-mini-h {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.lp-mini-h strong {
  font-size: 13px;
  font-weight: 700;
  color: #fff;
}
.lp-mini-arrows { display: inline-flex; gap: 4px; }
.lp-mini-arrow {
  width: 22px; height: 22px;
  border-radius: 999px;
  background: rgba(255,255,255,.10);
  border: 0;
  color: var(--lp-text-on-violet);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background .15s;
}
.lp-mini-arrow:hover { background: rgba(255,255,255,.20); }

.lp-mini-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  row-gap: 2px;
}
.lp-mini-dow {
  font-size: 9.5px;
  font-weight: 600;
  color: var(--lp-text-on-violet-faint);
  text-align: center;
  padding: 4px 0 6px;
}
.lp-mini-day {
  aspect-ratio: 1;
  font-size: 11px;
  font-weight: 500;
  color: var(--lp-text-on-violet);
  background: transparent;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  transition: background .15s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.lp-mini-day:hover { background: rgba(255,255,255,.08); }
.lp-mini-day.off { color: rgba(236,229,248,.35); }
.lp-mini-day.in-view { background: rgba(184,155,217,.22); color: #fff; }
.lp-mini-day.is-today {
  background: var(--lp-lime);
  color: var(--lp-primary-deep);
  font-weight: 700;
}

/* 패널 (My / Other Calendar) */
.lp-panel {
  display: flex;
  flex-direction: column;
}
.lp-panel-h {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.lp-panel-h strong {
  font-size: 12.5px;
  font-weight: 700;
  color: #fff;
}
.lp-panel-action {
  background: rgba(255,255,255,.10);
  border: 0;
  color: var(--lp-text-on-violet);
  font-size: 10.5px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: background .15s;
}
.lp-panel-action:hover { background: rgba(255,255,255,.18); }

.lp-panel-empty {
  font-size: 11px;
  color: var(--lp-text-on-violet-faint);
  padding: 6px 6px;
}

/* 행 */
.lp-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 6px;
  border-radius: 8px;
  font-size: 12px;
  color: var(--lp-text-on-violet);
  cursor: pointer;
  transition: background .15s;
}
.lp-item:hover { background: rgba(255,255,255,.06); }
.lp-item.is-done {
  color: var(--lp-text-on-violet-faint);
  text-decoration: line-through;
}
.lp-item--readonly { cursor: pointer; }

.lp-item-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lp-cat {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  flex-shrink: 0;
}
.cat-lime     { background: var(--lp-lime); }
.cat-lavender { background: var(--lp-card-lavender-1); }
.cat-cream    { background: #F5EDD8; }
.cat-rose     { background: #C58FA3; }

/* 체크박스 */
.lp-check {
  appearance: none;
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 4px;
  border: 1.5px solid rgba(255,255,255,.45);
  background: transparent;
  cursor: pointer;
  position: relative;
  flex-shrink: 0;
  transition: background .15s, border-color .15s;
}
.lp-check:checked {
  background: var(--lp-lime);
  border-color: var(--lp-lime);
}
.lp-check:checked::after {
  content: '';
  position: absolute;
  left: 3px;
  top: 0px;
  width: 4px;
  height: 8px;
  border: solid var(--lp-primary-deep);
  border-width: 0 1.6px 1.6px 0;
  transform: rotate(45deg);
}
</style>
