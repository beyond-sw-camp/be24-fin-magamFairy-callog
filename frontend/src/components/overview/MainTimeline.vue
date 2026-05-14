<script setup>
import { computed, ref, onUnmounted } from 'vue';
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

// 다크모드 여부 계산
const isDark = computed(() => store.theme === 'dark');

/* ─── 컬럼 리사이즈 (담당사명 / 캠페인명 각각) ─── */
const LEFT_COL_KEY = 'overviewTimelineLeftColWidth'
const NAME_COL_KEY = 'overviewTimelineNameColWidth'
const LEFT_MIN = 80
const LEFT_MAX = 320
const NAME_MIN = 120
const NAME_MAX = 600

const leftColWidth = ref(
  Number(typeof window !== 'undefined' ? window.localStorage.getItem(LEFT_COL_KEY) : null) || 160
)
const nameColWidth = ref(
  Number(typeof window !== 'undefined' ? window.localStorage.getItem(NAME_COL_KEY) : null) || 340
)
const activeResize = ref(null)  // 'left' | 'name' | null

function clamp(v, min, max) { return Math.min(max, Math.max(min, v)) }

const panelWidth = computed(() => leftColWidth.value + nameColWidth.value)

function startResize(target, e) {
  e.preventDefault()
  activeResize.value = target
  const startX = e.clientX
  const startLeft = leftColWidth.value
  const startName = nameColWidth.value
  const onMove = (ev) => {
    const dx = ev.clientX - startX
    if (target === 'left') {
      leftColWidth.value = clamp(startLeft + dx, LEFT_MIN, LEFT_MAX)
    } else if (target === 'name') {
      nameColWidth.value = clamp(startName + dx, NAME_MIN, NAME_MAX)
    }
  }
  const onUp = () => {
    activeResize.value = null
    window.removeEventListener('pointermove', onMove)
    window.removeEventListener('pointerup', onUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    try {
      window.localStorage.setItem(LEFT_COL_KEY, String(leftColWidth.value))
      window.localStorage.setItem(NAME_COL_KEY, String(nameColWidth.value))
    } catch { /* ignore */ }
  }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', onUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}
const startLeftResize = (e) => startResize('left', e)
const startNameResize = (e) => startResize('name', e)

onUnmounted(() => {
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
})

// const events = ref([])
const props = defineProps({
  eventsData: {
    type: Array,
    required: true,
    default: () => []
  }
})

// events.value = props.eventsData;
const events = computed(() => props.eventsData);

// 2. 환경 설정
const dayWidth = 18;
const rowHeight = 56;

// 3. 헬퍼 함수
const parseDate = (dateStr) => {
  const [y, m, d] = dateStr.split('-');
  return new Date(y, m - 1, d);
};

// 4. 타임라인 동적 계산 로직 (기존과 동일)
const timelineRange = computed(() => {
  if (events.value.length === 0) return { start: new Date(), end: new Date() };

  const startDates = events.value.map(e => parseDate(e.start).getTime());
  const endDates = events.value.map(e => parseDate(e.end).getTime());

  const minDate = new Date(Math.min(...startDates));
  const maxDate = new Date(Math.max(...endDates));

  return {
    start: new Date(minDate.getFullYear(), minDate.getMonth(), 1),
    end: new Date(maxDate.getFullYear(), maxDate.getMonth() + 1, 0)
  };
});

const months = computed(() => {
  const arr = [];
  let current = new Date(timelineRange.value.start);
  const end = timelineRange.value.end;

  while (current <= end) {
    const y = current.getFullYear();
    const m = current.getMonth();
    const daysInMonth = new Date(y, m + 1, 0).getDate();

    arr.push({
      year: y,
      month: m + 1,
      days: daysInMonth,
      width: daysInMonth * dayWidth
    });
    current = new Date(y, m + 1, 1);
  }
  return arr;
});

const totalWidth = computed(() => months.value.reduce((acc, curr) => acc + curr.width, 0));

const getEventStyle = (event, index) => {
  const eStart = parseDate(event.start);
  const eEnd = parseDate(event.end);
  const tStart = timelineRange.value.start;

  const startDiff = (eStart.getTime() - tStart.getTime()) / (1000 * 3600 * 24);
  const duration = ((eEnd.getTime() - eStart.getTime()) / (1000 * 3600 * 24)) + 1;

  return {
    top: `${(index * rowHeight) + ((rowHeight - 36) / 2)}px`,
    left: `${startDiff * dayWidth}px`,
    width: `${duration * dayWidth}px`
  };
};

const getTodayLineStyle = () => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const tStart = timelineRange.value.start;
  const tEnd = timelineRange.value.end;

  if (today < tStart || today > tEnd) return { display: 'none' };
  const startDiff = (today.getTime() - tStart.getTime()) / (1000 * 3600 * 24);

  return {
    left: `${startDiff * dayWidth}px`,
    display: 'block'
  };
};
</script>

<template>
  <div
    class="lp-timeline w-full h-full flex flex-col text-sm"
    :class="isDark ? 'is-dark' : ''"
  >
    <div class="lp-timeline__scroll flex-1 overflow-auto flex relative custom-scrollbar">

      <!-- ═══ Left frozen column (담당사명 / 캠페인명) ═══ -->
      <div
        class="lp-frozen sticky left-0 z-30 flex-shrink-0 flex flex-col"
        :style="{ width: panelWidth + 'px' }"
      >
        <!-- 핸들 1: 담당사명 ↔ 캠페인명 -->
        <div
          class="col-resize-handle"
          :class="{ 'col-resize-handle--active': activeResize === 'left' }"
          :style="{ left: (leftColWidth - 3) + 'px' }"
          @pointerdown="startLeftResize"
          role="separator"
          aria-orientation="vertical"
          aria-label="담당사명 컬럼 너비 조절"
          title="좌우로 드래그해서 담당사명 컬럼 너비를 조절하세요"
        ></div>

        <!-- 핸들 2: 캠페인명 ↔ 타임라인 -->
        <div
          class="col-resize-handle"
          :class="{ 'col-resize-handle--active': activeResize === 'name' }"
          :style="{ left: (panelWidth - 3) + 'px' }"
          @pointerdown="startNameResize"
          role="separator"
          aria-orientation="vertical"
          aria-label="캠페인명 컬럼 너비 조절"
          title="좌우로 드래그해서 캠페인명 컬럼 너비를 조절하세요"
        ></div>

        <!-- 좌측 헤더 (sticky top) -->
        <div class="lp-frozen__head sticky top-0 z-50 flex flex-col box-border">
          <div class="lp-frozen__head-row">
            <div
              class="lp-frozen__head-cell lp-frozen__head-cell--manager"
              :style="{ width: leftColWidth + 'px' }"
            >담당사명</div>
            <div
              class="lp-frozen__head-cell lp-frozen__head-cell--name"
              :style="{ width: nameColWidth + 'px' }"
            >캠페인명</div>
          </div>
        </div>

        <!-- 좌측 row 본문 -->
        <div class="lp-frozen__body flex-1">
          <div
            v-for="event in events"
            :key="`sidebar-${event.id}`"
            class="lp-row flex items-center"
          >
            <div
              class="lp-row__manager flex-shrink-0 flex items-center"
              :style="{ width: leftColWidth + 'px' }"
            >
              <span class="lp-row__dot" :class="event.colorClass" aria-hidden="true"></span>
              <span class="lp-row__manager-text truncate">{{ event.projectManager }}</span>
            </div>
            <div
              class="lp-row__name flex-shrink-0 flex items-center truncate"
              :style="{ width: nameColWidth + 'px' }"
            >
              {{ event.title }}
            </div>
          </div>

        </div>
      </div>

      <!-- ═══ Right side: month/date axis + bars ═══ -->
      <div
        class="lp-axis relative"
        :style="{ width: `${totalWidth}px` }"
      >
        <!-- Top axis (sticky, 2 bands) -->
        <div class="lp-axis__head sticky top-0 z-40 flex flex-col box-border">
          <div class="lp-axis__head-months flex">
            <div
              v-for="(month, idx) in months"
              :key="`month-header-${idx}`"
              class="lp-axis__month"
              :style="{ width: `${month.width}px` }"
            >
              <span class="lp-axis__month-label">
                {{ month.year }}.{{ String(month.month).padStart(2, '0') }}
              </span>
            </div>
          </div>
          <div class="lp-axis__head-weeks flex">
            <div
              v-for="(month, idx) in months"
              :key="`week-header-${idx}`"
              class="lp-axis__weeks-cell"
              :style="{ width: `${month.width}px` }"
            >
              <span class="lp-axis__weeks-label">
                {{ String(month.month).padStart(2, '0') }}월
              </span>
            </div>
          </div>
        </div>

        <!-- Body grid + bars -->
        <div class="lp-grid relative" :style="{ height: `${events.length * 56}px` }">

          <!-- Vertical month dividers -->
          <div class="absolute inset-0 flex pointer-events-none z-0">
            <div
              v-for="(month, idx) in months"
              :key="`grid-col-${idx}`"
              class="lp-grid__col h-full"
              :style="{ width: `${month.width}px` }"
            ></div>
          </div>

          <!-- Horizontal row separators -->
          <div class="absolute inset-0 pointer-events-none z-0 flex flex-col">
            <div
              v-for="event in events"
              :key="`grid-row-${event.id}`"
              class="lp-grid__row"
            ></div>
          </div>

          <!-- Bars -->
          <div
            v-for="(event, index) in events"
            :key="`bar-${event.id}`"
            class="lp-bar absolute flex items-center px-3 z-10 overflow-hidden"
            :class="event.colorClass"
            :style="getEventStyle(event, index)"
          >
            <span class="lp-bar__title truncate">{{ event.title }}</span>
          </div>

          <!-- Today line -->
          <div
            class="lp-today absolute top-0 bottom-0 pointer-events-none z-20"
            :style="getTodayLineStyle()"
          >
            <span class="lp-today__arrow" aria-hidden="true">
              <svg width="12" height="10" viewBox="0 0 12 10" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M6 10L0 0H12L6 10Z" fill="currentColor"/>
              </svg>
            </span>
          </div>

        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
/* ─── Lavender Pop design tokens (cascade from :root) ─── */
.lp-timeline {
  --lp-card-lavender-3: #B0A4DA;
  --lp-now:             #E0344A;
  --r-md:               14px;
  --r-lg:               24px;
  --r-pill:             999px;
  --shadow-card:        0 1px 2px rgba(63,52,99,.04), 0 6px 18px rgba(63,52,99,.06);
  --shadow-bar:         0 1px 2px rgba(63,52,99,.05), 0 6px 14px rgba(63,52,99,.07);
  --shadow-bar-hover:   0 4px 8px rgba(63,52,99,.10), 0 12px 26px rgba(63,52,99,.14);

  background: var(--lp-surface);
  color: var(--lp-text);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-card);
  padding: 8px 8px 8px 8px;
  font-family: 'Pretendard Variable', 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif;
  font-feature-settings: 'tnum' 1, 'ss01' 1;
  box-sizing: border-box;
  overflow: hidden;
}

/* Scroll container inside the rounded card */
.lp-timeline__scroll {
  background: var(--lp-surface);
  border-radius: 18px;
  scrollbar-gutter: stable;
}

/* ═══ Custom scrollbar (Lavender Pop) ═══ */
.custom-scrollbar {
  scrollbar-color: var(--lp-border) transparent;
  scrollbar-width: thin;
}
.custom-scrollbar::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: var(--lp-border);
  border-radius: var(--r-pill);
  border: 2px solid var(--lp-surface);
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: var(--lp-primary);
}
.custom-scrollbar::-webkit-scrollbar-corner {
  background: transparent;
}

/* ═══ Resize handles ═══ */
.col-resize-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  z-index: 60;
  transition: background-color 0.15s;
  touch-action: none;
}
.col-resize-handle::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 1px;
  margin-left: -0.5px;
  background: transparent;
  transition: background-color 0.15s;
}
.col-resize-handle:hover::after,
.col-resize-handle--active::after {
  background: var(--lp-primary-strong);
  width: 2px;
  margin-left: -1px;
}
.col-resize-handle:hover,
.col-resize-handle--active {
  background: rgba(111, 90, 155, 0.10);
}

/* ═══ Left frozen column ═══ */
.lp-frozen {
  background: var(--lp-surface);
  border-right: 1px solid var(--lp-border);
  box-shadow: 3px 0 10px rgba(63, 52, 99, 0.04);
}

/* Left header (two-band height to match right axis) */
.lp-frozen__head {
  background: var(--lp-surface);
  border-bottom: 1px solid var(--lp-border);
  height: 64px;
}
.lp-frozen__head-row {
  display: flex;
  height: 100%;
  align-items: stretch;
}
.lp-frozen__head-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: -0.005em;
  color: var(--lp-primary-deep);
  flex-shrink: 0;
}
.lp-frozen__head-cell--manager {
  border-right: 1px solid var(--lp-border);
}

/* Left body rows */
.lp-frozen__body { background: var(--lp-surface); }

.lp-row {
  height: 56px;
  border-bottom: 1px solid var(--lp-border);
  transition: background-color 0.18s;
}
.lp-row:hover { background-color: var(--lp-surface-soft); }
.lp-row:last-child { border-bottom: 0; }

.lp-row__manager {
  padding: 0 12px;
  border-right: 1px solid var(--lp-border);
  gap: 8px;
  height: 100%;
}
.lp-row__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
  background: var(--lp-primary);
  box-shadow: 0 0 0 3px rgba(183,155,217,.20);
}
.lp-row__manager-text {
  font-size: 13px;
  font-weight: 700;
  color: var(--lp-primary-deep);
  letter-spacing: -0.005em;
}
.lp-row__name {
  padding: 0 16px;
  font-size: 13px;
  font-weight: 700;
  color: var(--lp-primary-deep);
  letter-spacing: -0.005em;
  height: 100%;
}

/* Status dot tones */
.lp-row__dot.evt-live,
.lp-row__dot.bar-live,
.lp-row__dot.status-live,
.lp-row__dot[class*="bg-violet-500"],
.lp-row__dot[class*="bg-purple-500"],
.lp-row__dot[class*="bg-indigo-500"] {
  background: var(--lp-card-lavender-3);
  box-shadow: 0 0 0 3px rgba(176,164,218,.30);
}
.lp-row__dot.evt-draft,
.lp-row__dot.bar-draft,
.lp-row__dot.status-draft,
.lp-row__dot[class*="bg-gray-300"],
.lp-row__dot[class*="bg-slate-300"] {
  background: var(--lp-card-lavender-1);
  box-shadow: 0 0 0 3px rgba(221,210,238,.45);
}
.lp-row__dot.evt-done,
.lp-row__dot.bar-done,
.lp-row__dot.status-done,
.lp-row__dot[class*="bg-yellow-300"],
.lp-row__dot[class*="bg-amber-300"] {
  background: var(--lp-card-cream);
  box-shadow: 0 0 0 3px rgba(245,237,216,.55);
}
.lp-row__dot.evt-review,
.lp-row__dot.bar-review,
.lp-row__dot.status-review,
.lp-row__dot[class*="bg-pink-300"],
.lp-row__dot[class*="bg-rose-300"],
.lp-row__dot[class*="bg-orange-300"] {
  background: var(--lp-card-peach);
  box-shadow: 0 0 0 3px rgba(255,226,221,.55);
}

/* ═══ Right axis ═══ */
.lp-axis { background: var(--lp-surface); }

.lp-axis__head {
  background: var(--lp-surface);
  border-bottom: 1px solid var(--lp-border);
  height: 64px;
}
.lp-axis__head-months {
  height: 32px;
  border-bottom: 1px solid var(--lp-border);
}
.lp-axis__head-weeks {
  height: 32px;
}
.lp-axis__month {
  border-right: 1px solid var(--lp-border);
  display: flex;
  align-items: center;
  height: 100%;
  position: relative;
}
.lp-axis__month:last-child { border-right: 0; }
.lp-axis__month-label {
  position: sticky;
  left: 8px;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--lp-primary-deep);
  font-variant-numeric: tabular-nums;
  padding: 0 10px;
  white-space: nowrap;
}
.lp-axis__weeks-cell {
  border-right: 1px solid var(--lp-border);
  display: flex;
  align-items: center;
  height: 100%;
  position: relative;
}
.lp-axis__weeks-cell:last-child { border-right: 0; }
.lp-axis__weeks-label {
  position: sticky;
  left: 8px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--lp-text-faint);
  font-variant-numeric: tabular-nums;
  padding: 0 10px;
  white-space: nowrap;
  text-transform: uppercase;
}

/* ═══ Grid body ═══ */
.lp-grid { background: var(--lp-surface); }
.lp-grid__col {
  border-right: 1px solid var(--lp-border);
}
.lp-grid__col:last-child { border-right: 0; }
.lp-grid__row {
  height: 56px;
  border-bottom: 1px solid var(--lp-border);
  transition: background-color 0.18s;
}

/* ═══ Bars (photo-style) ═══ */
.lp-bar {
  height: 28px;
  border-radius: var(--r-md);
  box-shadow: var(--shadow-bar);
  color: var(--lp-primary-deep);
  background: var(--lp-card-lavender-1);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  cursor: default;
}
.lp-bar:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-bar-hover);
  z-index: 12;
}
.lp-bar__title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: -0.005em;
  line-height: 1.1;
  display: none;
}
/* show title only when bar wide enough */
.lp-bar[style*="width"] .lp-bar__title { display: inline-block; }
.lp-bar { container-type: inline-size; }
@container (min-width: 80px) {
  .lp-bar__title { display: inline-block; }
}

/* Status palettes for bars */
.lp-bar.evt-live,
.lp-bar.bar-live,
.lp-bar.status-live,
.lp-bar[class*="bg-violet-500"],
.lp-bar[class*="bg-purple-500"],
.lp-bar[class*="bg-indigo-500"] {
  background: var(--lp-card-lavender-3);
  color: var(--lp-violet-deep);
}
.lp-bar.evt-draft,
.lp-bar.bar-draft,
.lp-bar.status-draft,
.lp-bar[class*="bg-gray-300"],
.lp-bar[class*="bg-slate-300"] {
  background: var(--lp-card-lavender-1);
  color: var(--lp-violet-deep);
}
.lp-bar.evt-done,
.lp-bar.bar-done,
.lp-bar.status-done,
.lp-bar[class*="bg-yellow-300"],
.lp-bar[class*="bg-amber-300"] {
  background: var(--lp-card-cream);
  color: var(--lp-violet-deep);
}
.lp-bar.evt-review,
.lp-bar.bar-review,
.lp-bar.status-review,
.lp-bar[class*="bg-pink-300"],
.lp-bar[class*="bg-rose-300"],
.lp-bar[class*="bg-orange-300"] {
  background: var(--lp-card-peach);
  color: var(--lp-violet-deep);
}
.lp-bar.evt-lime,
.lp-bar[class*="bg-lime"],
.lp-bar[class*="bg-green-300"] {
  background: var(--lp-lime);
  color: var(--lp-violet-deep);
}

/* ═══ Today line (red, photo-style) ═══ */
.lp-today {
  width: 2px;
  background: var(--lp-now);
  box-shadow: 0 0 0 4px rgba(224,52,74,.10);
}
.lp-today__arrow {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  color: var(--lp-now);
  filter: drop-shadow(0 2px 4px rgba(224,52,74,.32));
  line-height: 0;
}

/* ═══ Empty state (Lavender Pop) ═══ */
.lp-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 32px 16px;
  color: var(--lp-text-muted);
}
.lp-empty__icon {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  background: var(--lp-surface-soft);
  color: var(--lp-primary-strong);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}
.lp-empty__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--lp-primary-deep);
}
.lp-empty__sub {
  font-size: 11.5px;
  color: var(--lp-text-faint);
}

/* ═══ Dark mode (preserved gracefully) ═══ */
.lp-timeline.is-dark {
  --lp-surface:       #1E1E2D;
  --lp-surface-soft:  #252537;
  --lp-border:        #2D2D3F;
  --lp-text:          #E6E1F2;
  --lp-text-muted:    #A9A1BF;
  --lp-text-faint:    #7C7596;
  --lp-primary-deep:  #C6BAE6;
  --lp-primary-strong:#B79BD9;
  background: var(--lp-surface);
}
.lp-timeline.is-dark .lp-row__name,
.lp-timeline.is-dark .lp-row__manager-text {
  color: var(--lp-text);
}
.lp-timeline.is-dark .lp-axis__month-label {
  color: var(--lp-text);
}

/* ═══ Reduced motion ═══ */
@media (prefers-reduced-motion: reduce) {
  .lp-bar,
  .lp-row,
  .lp-grid__row {
    transition: none;
  }
}

/* ═══ Compatibility shims for any legacy :deep selectors ═══ */
:deep(.evt-violet-strong) {
  background: var(--lp-card-lavender-3);
  color: var(--lp-violet-deep);
}
:deep(.evt-lime) {
  background: var(--lp-lime);
  color: var(--lp-violet-deep);
}
:deep(.evt-lavender-soft) {
  background: var(--lp-card-lavender-1);
  color: var(--lp-violet-deep);
}
:deep(.evt-cream) {
  background: var(--lp-card-cream);
  color: var(--lp-violet-deep);
}
</style>
