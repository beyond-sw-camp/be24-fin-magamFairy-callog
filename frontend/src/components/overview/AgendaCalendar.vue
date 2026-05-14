<script setup>
import { computed } from 'vue'

const props = defineProps({
  eventsData: { type: Array, default: () => [] },
  anchorDate: { type: Date, default: () => new Date() },
})
const emit = defineEmits(['event-click'])

function toIso(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}
function startOfWeek(d) {
  const date = new Date(d)
  const day = date.getDay()
  const diff = day === 0 ? -6 : 1 - day
  date.setDate(date.getDate() + diff)
  date.setHours(0, 0, 0, 0)
  return date
}

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

/* Skeleton groups for empty state — show this week's 7 days so the structure is visible */
const skeletonGroups = computed(() => {
  const start = startOfWeek(props.anchorDate ?? new Date())
  const arr = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(start)
    d.setDate(d.getDate() + i)
    arr.push({ date: toIso(d), items: [], isSkeleton: true })
  }
  return arr
})

const displayGroups = computed(() => {
  return grouped.value.length > 0 ? grouped.value : skeletonGroups.value
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

/* ─── pure presentation helpers (no logic changes) ─── */
const KO_WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']
function isoToday() {
  const d = new Date(); d.setHours(0, 0, 0, 0)
  return d.toISOString().slice(0, 10)
}
function isoTomorrow() {
  const d = new Date(); d.setHours(0, 0, 0, 0); d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
}
function isToday(s) { return s === isoToday() }
function isTomorrow(s) { return s === isoTomorrow() }
function koLabel(s) {
  const d = new Date(s)
  const wd = KO_WEEKDAYS[d.getDay()]
  return `${d.getMonth() + 1}월 ${d.getDate()}일 (${wd})`
}
const TONE_ROTATION = ['evt-violet-strong', 'evt-lime', 'evt-lavender-soft', 'evt-cream']
function fallbackTone(groupIdx, withinIdx) {
  return TONE_ROTATION[(groupIdx + withinIdx) % TONE_ROTATION.length]
}

function onAddNew() {
  emit('event-click', { __action: 'add-task' })
}
</script>

<template>
  <div class="agenda">
    <!-- Grouped scroll list — uses skeleton (current week) when empty -->
    <div class="agenda__scroll">
      <section
        v-for="(g, gi) in displayGroups"
        :key="g.date"
        class="agenda__group"
        :class="{ 'is-empty': g.isSkeleton }"
      >
        <!-- Sticky day header -->
        <header
          class="agenda__date"
          :class="{ 'is-today': isToday(g.date), 'is-tomorrow': isTomorrow(g.date) }"
        >
          <span v-if="isToday(g.date)" class="agenda__date-tag">
            <span class="agenda__date-dot" />
            오늘
            <span class="agenda__date-sep">·</span>
            <span class="agenda__date-en">{{ koLabel(g.date) }}</span>
          </span>
          <span v-else-if="isTomorrow(g.date)" class="agenda__date-tag is-tomorrow">
            내일
            <span class="agenda__date-sep">·</span>
            <span class="agenda__date-en">{{ koLabel(g.date) }}</span>
          </span>
          <template v-else>
            <span class="agenda__date-day">{{ fmtDate(g.date).dd }}</span>
            <span class="agenda__date-mm">{{ fmtDate(g.date).mm }}월 · {{ fmtDate(g.date).wd }}</span>
          </template>
        </header>

        <!-- Event rows -->
        <ul class="agenda__list">
          <li
            v-for="(ev, ei) in g.items"
            :key="ev.id"
            class="agenda__item"
            :class="[ev.colorClass ? ev.colorClass : fallbackTone(gi, ei)]"
            tabindex="0"
            @click="emit('event-click', ev)"
            @keydown.enter="emit('event-click', ev)"
          >
            <span class="agenda__alltag">종일</span>
            <span
              class="agenda__bar"
              :style="ev.customColor ? { background: ev.customColor } : null"
            />
            <div class="agenda__body">
              <div class="agenda__title">
                <span v-if="ev.icon" class="agenda__icon">{{ ev.icon }}</span>
                {{ ev.title }}
              </div>
              <div class="agenda__meta">
                <span class="agenda__pm">{{ ev.projectManager || '-' }}</span>
              </div>
            </div>
            <span class="agenda__dday">{{ dDayOf(ev.end) }}</span>
          </li>
          <li v-if="g.isSkeleton" class="agenda__empty-row">
            <span class="agenda__empty-dot"></span>
            일정 없음
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<style scoped>
.agenda {
  /* Standard --lp-* tokens cascade from :root (base.css). */
  --lp-card-lavender-3: #B0A4DA;
  --r-md: 14px;
  --r-lg: 18px;
  --r-xl: 24px;
  --r-pill: 999px;
  --shadow-card: 0 1px 2px rgba(63,52,99,.04), 0 6px 18px rgba(63,52,99,.06);

  /* outer surface card */
  height: 100%;
  box-sizing: border-box;
  background: var(--lp-surface);
  border-radius: var(--r-xl);
  padding: 24px;
  box-shadow: var(--shadow-card);
  color: var(--lp-text);
  font-family: 'Pretendard Variable', 'Pretendard', -apple-system, sans-serif;
  font-feature-settings: 'tnum' 1, 'ss01' 1;
  display: flex;
  flex-direction: column;
  min-height: 360px;
}

/* ═══ Inner scroll surface ═══ */
.agenda__scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-gutter: stable;
  margin: -4px -4px 0;
  padding: 4px 4px 8px;
}
.agenda__scroll::-webkit-scrollbar { width: 8px; }
.agenda__scroll::-webkit-scrollbar-thumb { background: var(--lp-border); border-radius: var(--r-pill); }
.agenda__scroll::-webkit-scrollbar-track { background: transparent; }

.agenda__group {
  margin-bottom: 18px;
}
.agenda__group:last-child { margin-bottom: 0; }

/* ═══ Sticky day header ═══ */
.agenda__date {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0 -4px 10px;
  padding: 10px 8px 10px;
  background: var(--lp-surface);
  border-bottom: 1px solid var(--lp-border);
  font-variant-numeric: tabular-nums;
}
.agenda__date-day {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--lp-primary-deep);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.agenda__date-mm {
  font-size: 12px;
  font-weight: 600;
  color: var(--lp-text-muted);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
}
.agenda__date-en {
  font-size: 12px;
  font-weight: 600;
  color: var(--lp-text-faint);
  letter-spacing: 0.02em;
  font-variant-numeric: tabular-nums;
}
.agenda__date-en--inline { margin-left: auto; }

.agenda__date-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--lp-primary-deep);
  letter-spacing: -0.01em;
  line-height: 1;
}
.agenda__date-tag.is-tomorrow { color: var(--lp-primary-strong); font-weight: 700; }
.agenda__date-sep {
  font-size: 18px;
  font-weight: 400;
  color: var(--lp-text-faint);
  line-height: 1;
}
.agenda__date-dot {
  width: 10px;
  height: 10px;
  border-radius: var(--r-pill);
  background: var(--lp-lime);
  box-shadow: 0 0 0 3px rgba(216,235,117,.32);
  display: inline-block;
}
.agenda__date.is-today { border-bottom-color: rgba(216,235,117,.55); }

/* ═══ Event list ═══ */
.agenda__list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Empty placeholder row in skeleton mode */
.agenda__empty-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--lp-text-faint);
  border-radius: var(--r-md, 14px);
  background: var(--lp-bg);
  border: 1px dashed var(--lp-border);
}
.agenda__empty-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--lp-border);
  flex-shrink: 0;
}

/* ═══ Event row — big rounded card, alternating tones via class ═══ */
.agenda__item {
  display: grid;
  grid-template-columns: auto 4px 1fr auto;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: var(--lp-card-lavender-1);
  color: var(--lp-violet-deep);
  border-radius: var(--r-md);
  cursor: pointer;
  outline: none;
  transition: transform .15s, box-shadow .15s, background-color .15s;
  box-shadow: 0 1px 2px rgba(63,52,99,.05), 0 4px 12px rgba(63,52,99,.05);
}
.agenda__item:hover,
.agenda__item:focus-visible {
  transform: translateY(-1px) scale(1.005);
  box-shadow: 0 6px 22px rgba(63,52,99,.16);
}

/* ALL DAY prefix pill */
.agenda__alltag {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  padding: 4px 9px;
  border-radius: var(--r-pill);
  background: rgba(255,255,255,.55);
  color: var(--lp-violet-deep);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* Colored bar */
.agenda__bar {
  width: 4px;
  height: 28px;
  border-radius: var(--r-pill);
  background: var(--lp-primary-strong);
  flex-shrink: 0;
}

.agenda__body { min-width: 0; }
.agenda__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--lp-violet-deep);
  letter-spacing: -0.005em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.25;
}
.agenda__icon {
  margin-right: 4px;
}
.agenda__meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  font-size: 11.5px;
  color: rgba(45,38,73,.66);
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}
.agenda__pm {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agenda__dday {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.02em;
  padding: 4px 10px;
  border-radius: var(--r-pill);
  background: rgba(255,255,255,.7);
  color: var(--lp-primary-deep);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* ═══ Tone rotation (fallback when no colorClass provided) ═══ */
.agenda__item.evt-violet-strong { background: var(--lp-card-lavender-3); }
.agenda__item.evt-lime           { background: var(--lp-lime); }
.agenda__item.evt-lavender-soft  { background: var(--lp-card-lavender-1); }
.agenda__item.evt-cream          { background: var(--lp-card-cream); }

/* ═══ Empty state ═══ */
.agenda__empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  flex: 1;
  min-height: 240px;
  color: var(--lp-text-muted);
  text-align: center;
}
.agenda__empty .material-symbols-outlined { font-size: 36px; color: var(--lp-text-faint); }
.agenda__empty p {
  font-size: 13px;
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}
.agenda__empty-sep { color: var(--lp-text-faint); }
.agenda__empty-cta {
  background: var(--lp-button-bg);
  color: #fff;
  border: 0;
  padding: 6px 14px;
  border-radius: var(--r-pill);
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background .15s, transform .12s;
}
.agenda__empty-cta:hover { background: var(--lp-button-bg-hover); }
.agenda__empty-cta:active { transform: scale(0.96); }

/* ═══ Reduced motion ═══ */
@media (prefers-reduced-motion: reduce) {
  .agenda__item { transition: none; }
  .agenda__item:hover, .agenda__item:focus-visible { transform: none; }
}

/* ═══ Responsive ═══ */
@media (max-width: 720px) {
  .agenda { padding: 16px; border-radius: 18px; }
  .agenda__item { padding: 12px 14px; gap: 10px; grid-template-columns: auto 4px 1fr auto; }
  .agenda__title { font-size: 14px; }
  .agenda__alltag { font-size: 9px; padding: 3px 7px; }
  .agenda__date-day, .agenda__date-tag { font-size: 18px; }
  .agenda__date-en--inline { display: none; }
}

/* ═══ Type/status chip overrides used by parent (OverView.vue) :deep selectors ═══ */
/* Background tone tokens applied when event arrives pre-classed via colorClass */
:deep(.evt-default) { background: var(--lp-card-lavender-1); color: var(--lp-violet-deep); }
:deep(.evt-active)  { background: var(--lp-lime);            color: var(--lp-violet-deep); }
:deep(.evt-urgent)  { background: var(--lp-card-peach);      color: var(--lp-violet-deep); }
:deep(.evt-draft)   { background: var(--lp-card-cream);      color: var(--lp-violet-deep); }
</style>
