<script setup>
import { computed } from 'vue'

const props = defineProps({
  eventsData: { type: Array, default: () => [] },
})
const emit = defineEmits(['event-click'])

const todayIso = new Date().toISOString().slice(0, 10)

const grouped = computed(() => {
  const sorted = [...(props.eventsData ?? [])]
    .filter(e => e.start && e.end && e.end >= todayIso)
    .sort((a, b) => (a.start ?? '').localeCompare(b.start ?? ''))

  const groups = new Map()
  for (const ev of sorted) {
    const key = ev.start
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(ev)
  }
  return [...groups.entries()].map(([date, items]) => ({ date, items }))
})

function fmtDate(s) {
  const d = new Date(s)
  const wd = ['일','월','화','수','목','금','토'][d.getDay()]
  return { mm: String(d.getMonth() + 1).padStart(2, '0'), dd: String(d.getDate()).padStart(2, '0'), wd }
}
function dDayOf(end) {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const e = new Date(end); e.setHours(0, 0, 0, 0)
  const diff = Math.round((e - today) / 86400000)
  if (diff < 0) return `D+${-diff}`
  if (diff === 0) return 'D-DAY'
  return `D-${diff}`
}
</script>

<template>
  <div class="agenda">
    <div v-if="!grouped.length" class="agenda__empty">
      <span class="material-symbols-outlined">event_busy</span>
      <p>예정된 일정이 없습니다.</p>
    </div>
    <div v-for="g in grouped" :key="g.date" class="agenda__group">
      <div class="agenda__date">
        <span class="agenda__date-day">{{ fmtDate(g.date).dd }}</span>
        <span class="agenda__date-mm">{{ fmtDate(g.date).mm }}월 · {{ fmtDate(g.date).wd }}</span>
      </div>
      <ul class="agenda__list">
        <li
          v-for="ev in g.items"
          :key="ev.id"
          class="agenda__item"
          @click="emit('event-click', ev)"
        >
          <div class="agenda__dot" :style="{ background: ev.customColor || (ev.type === 'deadline' ? '#F59E0B' : ev.type === 'milestone' ? '#3B82F6' : ev.type === 'task' ? '#10B981' : '#8B5CF6') }" />
          <div class="agenda__body">
            <div class="agenda__title">{{ ev.title }}</div>
            <div class="agenda__meta">
              {{ ev.projectManager || '-' }}
              <span class="agenda__dday">{{ dDayOf(ev.end) }}</span>
            </div>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.agenda {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
  background: var(--panel-color);
}
.agenda__group {
  margin-bottom: 18px;
}
.agenda__date {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 8px;
  padding: 0 4px;
}
.agenda__date-day {
  font-size: 22px;
  font-weight: 800;
  color: var(--accent-color, #8B5CF6);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}
.agenda__date-mm {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted-text);
}
.agenda__list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.agenda__item {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  cursor: pointer;
  transition: transform 0.1s, box-shadow 0.15s;
}
.agenda__item:hover { transform: translateY(-1px); box-shadow: 0 4px 10px rgba(0, 0, 0, 0.06); }
.agenda__dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; margin-top: 6px; }
.agenda__body { flex: 1; min-width: 0; }
.agenda__title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}
.agenda__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: var(--muted-text);
}
.agenda__dday {
  font-weight: 800;
  color: var(--accent-color, #8B5CF6);
  font-variant-numeric: tabular-nums;
}

.agenda__empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  height: 60vh;
  color: var(--muted-text);
}
.agenda__empty .material-symbols-outlined { font-size: 36px; color: var(--subtle-text); }
.agenda__empty p { font-size: 13px; margin: 0; }
</style>
