<script setup>
import { computed, ref, watch } from 'vue'
import { usePlannerStore } from '@/stores/planner'

const props = defineProps({
  eventsData: { type: Array, default: () => [] },
  anchorDate: { type: Date, default: () => new Date() },
})
const emit = defineEmits(['event-click', 'day-click', 'update:anchorDate'])

const store = usePlannerStore()
const isDark = computed(() => store.theme === 'dark')

const cursor = ref(new Date(props.anchorDate))
watch(() => props.anchorDate, (d) => { cursor.value = new Date(d) })

const weekDays = computed(() => {
  const start = new Date(cursor.value)
  start.setDate(start.getDate() - start.getDay())
  start.setHours(0, 0, 0, 0)
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(start); d.setDate(start.getDate() + i)
    return d
  })
})
const rangeLabel = computed(() => {
  const a = weekDays.value[0]
  const b = weekDays.value[6]
  return `${a.getFullYear()}.${String(a.getMonth() + 1).padStart(2, '0')}.${String(a.getDate()).padStart(2, '0')} - ${String(b.getMonth() + 1).padStart(2, '0')}.${String(b.getDate()).padStart(2, '0')}`
})

function shiftWeek(delta) {
  const d = new Date(cursor.value)
  d.setDate(d.getDate() + 7 * delta)
  cursor.value = d
  emit('update:anchorDate', d)
}
function gotoToday() {
  cursor.value = new Date()
  emit('update:anchorDate', cursor.value)
}

function fmtIso(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
function isToday(d) {
  const t = new Date(); t.setHours(0, 0, 0, 0)
  return d.getTime() === t.getTime()
}
function isPast(d) {
  const t = new Date(); t.setHours(0, 0, 0, 0)
  return d.getTime() < t.getTime()
}

const eventsByDay = computed(() => {
  const out = weekDays.value.map(() => [])
  for (const ev of props.eventsData) {
    if (!ev.start || !ev.end) continue
    weekDays.value.forEach((day, i) => {
      const iso = fmtIso(day)
      if (iso >= ev.start && iso <= ev.end) out[i].push(ev)
    })
  }
  return out
})

const DAYS_KOR = ['일', '월', '화', '수', '목', '금', '토']
</script>

<template>
  <div class="week-cal" :class="{ 'week-cal--dark': isDark }">
    <header class="week-cal__head">
      <h2 class="week-cal__title">{{ rangeLabel }}</h2>
      <div class="week-cal__nav">
        <button @click="shiftWeek(-1)" class="week-cal__btn">지난 주</button>
        <button @click="gotoToday" class="week-cal__btn week-cal__btn--primary">이번 주</button>
        <button @click="shiftWeek(1)" class="week-cal__btn">다음 주</button>
      </div>
    </header>

    <div class="week-cal__grid">
      <div
        v-for="(day, i) in weekDays"
        :key="day.toISOString()"
        class="week-col"
        :class="{
          'week-col--today': isToday(day),
          'week-col--past': isPast(day) && !isToday(day),
          'week-col--weekend': day.getDay() === 0 || day.getDay() === 6,
        }"
      >
        <div class="week-col__head">
          <div class="week-col__dow"
            :class="{
              'week-col__dow--sun': day.getDay() === 0,
              'week-col__dow--sat': day.getDay() === 6,
            }">
            {{ DAYS_KOR[day.getDay()] }}
          </div>
          <div class="week-col__date" :class="{ 'week-col__date--today': isToday(day) }">
            {{ day.getDate() }}
          </div>
        </div>
        <div class="week-col__body">
          <div
            v-for="ev in eventsByDay[i]"
            :key="ev.id"
            class="week-event"
            :class="[ev.customColor ? '' : ev.colorClass]"
            :style="ev.customColor ? {
              background: `color-mix(in srgb, ${ev.customColor} 14%, transparent)`,
              color: ev.customColor,
              borderColor: `color-mix(in srgb, ${ev.customColor} 32%, transparent)`,
            } : {}"
            @click.stop="emit('event-click', ev)"
          >
            <div class="week-event__title">
              <span v-if="ev.icon">{{ ev.icon }} </span>{{ ev.title }}
            </div>
            <div class="week-event__sub">{{ ev.projectManager || '' }}</div>
          </div>
          <button
            type="button"
            class="week-col__add"
            :title="`${fmtIso(day)} 일정 추가`"
            @click.stop="emit('day-click', { date: fmtIso(day), event: $event })"
          >+ 일정 추가</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.week-cal {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--panel-color);
  color: var(--text-primary);
}
.week-cal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  border-bottom: 1px solid var(--border-color);
}
.week-cal__title { font-size: 18px; font-weight: 800; margin: 0; font-variant-numeric: tabular-nums; }
.week-cal__nav { display: flex; gap: 6px; }
.week-cal__btn {
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
.week-cal__btn:hover { background: var(--panel-muted); }
.week-cal__btn--primary {
  background: var(--accent-color, #8B5CF6);
  color: #fff;
  border-color: var(--accent-color, #8B5CF6);
}
.week-cal__btn--primary:hover { background: #7C3AED; }

.week-cal__grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  min-height: 0;
}
.week-col {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  overflow: hidden;
}
.week-col:last-child { border-right: none; }
.week-col--today { background: rgba(139, 92, 246, 0.04); }
.week-col--today { border-left: 2px solid var(--accent-color, #8B5CF6); margin-left: -1px; }
.week-col--past { opacity: 0.78; }
.week-col--weekend { background: color-mix(in srgb, var(--panel-muted) 50%, transparent); }
.week-col--today.week-col--weekend { background: rgba(139, 92, 246, 0.06); }

.week-col__head {
  text-align: center;
  padding: 12px 0 8px;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-color);
  position: sticky;
  top: 0;
  z-index: 1;
}
.week-col__dow {
  font-size: 11px;
  font-weight: 700;
  color: var(--muted-text);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 2px;
}
.week-col__dow--sun { color: #EF4444; }
.week-col__dow--sat { color: #3B82F6; }
.week-col__date {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.week-col__date--today {
  color: #fff;
  background: var(--accent-color, #8B5CF6);
  border-radius: 50%;
  width: 30px;
  height: 30px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.week-col__body {
  flex: 1;
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  cursor: pointer;
}
.week-event {
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.1s, box-shadow 0.15s;
  background: rgba(139, 92, 246, 0.12);
  color: #5B21B6;
  border: 1px solid rgba(139, 92, 246, 0.2);
}
.week-event:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}
.week-event__title {
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.week-event__sub {
  font-size: 10px;
  opacity: 0.75;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.week-col__add {
  align-self: stretch;
  margin-top: 4px;
  padding: 8px;
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-color, #8B5CF6);
  background: transparent;
  border: 1px dashed var(--border-color);
  border-radius: 6px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, background 0.15s, border-color 0.15s;
}
.week-col__body:hover .week-col__add { opacity: 0.85; }
.week-col__add:hover {
  opacity: 1;
  background: rgba(139, 92, 246, 0.06);
  border-color: var(--accent-color, #8B5CF6);
}
</style>
