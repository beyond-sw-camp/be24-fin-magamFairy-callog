<script setup>
import { ref, computed, watch } from 'vue';
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

// 다크모드 여부 계산
const isDark = computed(() => store.theme === 'dark');

const props = defineProps({
  eventsData: {
    type: Array,
    required: true,
    default: () => []
  }
})

// props 데이터 동기화
const events = ref(props.eventsData);
watch(() => props.eventsData, (newVal) => {
  events.value = newVal;
});
</script>

<template>
  <div class="lp-table-card" :class="{ 'is-dark': isDark }">
    <div class="lp-table-scroll">
      <table class="lp-table">
        <thead class="lp-thead">
          <tr>
            <th scope="col" class="lp-th lp-th--frozen">
              <span class="lp-th__inner">
                PM사명
                <svg class="lp-sort-ic" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </span>
            </th>
            <th scope="col" class="lp-th">
              <span class="lp-th__inner">
                캠페인명
                <svg class="lp-sort-ic" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </span>
            </th>
            <th scope="col" class="lp-th lp-th--num">
              <span class="lp-th__inner">
                시작일
                <svg class="lp-sort-ic" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </span>
            </th>
            <th scope="col" class="lp-th lp-th--num">
              <span class="lp-th__inner">
                종료일
                <svg class="lp-sort-ic" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </span>
            </th>
          </tr>
        </thead>

        <tbody class="lp-tbody">
          <tr v-for="event in events" :key="event.id" class="lp-row">
            <td class="lp-td lp-td--frozen">
              {{ event.projectManager }}
            </td>
            <td class="lp-td lp-td--title">
              {{ event.title }}
            </td>
            <td class="lp-td lp-td--num">
              {{ event.start }}
            </td>
            <td class="lp-td lp-td--num">
              {{ event.end }}
            </td>
          </tr>

        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
/* ═══ Lavender Pop Design Tokens (cascade from :root) ═══ */
.lp-table-card {
  --lp-card-lavender-3: #B0A4DA;
  --lp-tan:             #C9A86B;
  --lp-coral:           #E89A8B;
  --r-md: 14px;
  --r-lg: 24px;
  --r-pill: 999px;
  --shadow-card: 0 1px 2px rgba(63, 52, 99, .04), 0 6px 18px rgba(63, 52, 99, .06);

  font-family: 'Pretendard Variable', 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif;
  font-feature-settings: 'tnum' 1, 'ss01' 1;
  background: var(--lp-surface);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-card);
  color: var(--lp-text);
  padding: 24px;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;
}

/* ═══ Inner scroll wrapper ═══ */
.lp-table-scroll {
  width: 100%;
  overflow: auto;
  border-radius: calc(var(--r-lg) - 12px);
  scrollbar-gutter: stable;
}
.lp-table-scroll::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}
.lp-table-scroll::-webkit-scrollbar-thumb {
  background: var(--lp-border);
  border-radius: var(--r-pill);
  border: 2px solid var(--lp-surface);
}
.lp-table-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--lp-card-lavender-2);
}
.lp-table-scroll::-webkit-scrollbar-track {
  background: transparent;
}

/* ═══ Table base ═══ */
.lp-table {
  width: 100%;
  min-width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-family: inherit;
  color: var(--lp-text);
}

/* ═══ Header (sticky-top) ═══ */
.lp-thead {
  position: sticky;
  top: 0;
  z-index: 3;
}
.lp-th {
  background: var(--lp-surface-soft);
  color: var(--lp-primary-deep);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  text-align: left;
  padding: 14px 18px;
  border-top: 2px solid var(--lp-primary-strong);
  border-bottom: 1px solid var(--lp-border);
  white-space: nowrap;
  user-select: none;
}
.lp-th--num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.lp-th__inner {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: color 0.15s ease-in-out;
}
.lp-th--num .lp-th__inner {
  flex-direction: row-reverse;
}
.lp-th__inner:hover {
  color: var(--lp-primary-strong);
}
.lp-sort-ic {
  opacity: 0.55;
  transition: transform 0.2s ease, opacity 0.15s ease-in-out;
}
.lp-th__inner:hover .lp-sort-ic {
  opacity: 1;
}
.lp-th__inner.is-sorted-asc .lp-sort-ic {
  transform: rotate(180deg);
  opacity: 1;
  color: var(--lp-primary-strong);
}
.lp-th__inner.is-sorted-desc .lp-sort-ic {
  opacity: 1;
  color: var(--lp-primary-strong);
}

/* Frozen-left header */
.lp-th--frozen {
  position: sticky;
  left: 0;
  z-index: 4;
  background: var(--lp-surface-soft);
  border-right: 1px solid var(--lp-border);
}

/* ═══ Body ═══ */
.lp-tbody {
  background: var(--lp-surface);
}
.lp-row {
  background: var(--lp-surface);
  transition: background-color 0.15s ease-in-out, color 0.15s ease-in-out;
}
.lp-row:hover {
  background: var(--lp-surface-soft);
}

.lp-td {
  height: 56px;
  padding: 0 18px;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--lp-text);
  border-bottom: 1px solid var(--lp-border);
  white-space: nowrap;
  vertical-align: middle;
  transition: color 0.15s ease-in-out;
}
.lp-row:hover .lp-td {
  color: var(--lp-primary-deep);
}

.lp-td--title {
  font-weight: 600;
  letter-spacing: -0.005em;
}

.lp-td--num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  color: var(--lp-text-muted);
  letter-spacing: 0.01em;
}
.lp-row:hover .lp-td--num {
  color: var(--lp-primary-strong);
}

/* Frozen-left first column */
.lp-td--frozen {
  position: sticky;
  left: 0;
  background: inherit;
  z-index: 2;
  border-right: 1px solid var(--lp-border);
  font-weight: 600;
  color: var(--lp-primary-deep);
}

/* ═══ Empty state ═══ */
.lp-row--empty:hover {
  background: var(--lp-surface);
}
.lp-td--empty {
  text-align: center;
  padding: 64px 24px;
  color: var(--lp-text-faint);
  font-size: 13px;
  font-weight: 500;
  font-style: italic;
  background: var(--lp-surface);
  border-bottom: 0;
}

/* ═══ Status / Type chips (compatible with :deep(.evt-*) overrides) ═══ */
:deep(.evt-chip),
:deep(.evt-status),
:deep(.evt-type) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: var(--r-pill);
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  background: var(--lp-surface-soft);
  color: var(--lp-primary-deep);
  border: 1px solid var(--lp-border);
  letter-spacing: 0.02em;
  white-space: nowrap;
}

:deep(.evt-chip::before),
:deep(.evt-status::before),
:deep(.evt-type::before) {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: var(--r-pill);
  background: var(--lp-primary-strong);
  flex: 0 0 6px;
}

/* LIVE → lime */
:deep(.evt-live),
:deep(.evt-status-live),
:deep(.evt-chip.evt-live) {
  background: var(--lp-lime-soft);
  color: var(--lp-primary-deep);
  border-color: var(--lp-lime);
}
:deep(.evt-live::before),
:deep(.evt-status-live::before) {
  background: var(--lp-lime);
}

/* DRAFT → lavender */
:deep(.evt-draft),
:deep(.evt-status-draft),
:deep(.evt-chip.evt-draft) {
  background: var(--lp-card-lavender-1);
  color: var(--lp-primary-deep);
  border-color: var(--lp-card-lavender-2);
}
:deep(.evt-draft::before),
:deep(.evt-status-draft::before) {
  background: var(--lp-primary-strong);
}

/* DONE → cream */
:deep(.evt-done),
:deep(.evt-status-done),
:deep(.evt-chip.evt-done) {
  background: var(--lp-card-cream);
  color: var(--lp-primary-deep);
  border-color: var(--lp-card-cream);
}
:deep(.evt-done::before),
:deep(.evt-status-done::before) {
  background: var(--lp-tan);
}

/* REVIEW → peach */
:deep(.evt-review),
:deep(.evt-status-review),
:deep(.evt-chip.evt-review) {
  background: var(--lp-card-peach);
  color: var(--lp-primary-deep);
  border-color: var(--lp-card-peach);
}
:deep(.evt-review::before),
:deep(.evt-status-review::before) {
  background: var(--lp-coral);
}

/* ═══ Numeric cells helper ═══ */
:deep(td.num),
:deep(td[data-type="number"]) {
  font-variant-numeric: tabular-nums;
  text-align: right;
}

/* ═══ Responsive ═══ */
@media (max-width: 720px) {
  .lp-table-card {
    padding: 16px;
    border-radius: 18px;
  }
  .lp-th {
    padding: 12px 14px;
    font-size: 11px;
  }
  .lp-td {
    height: 52px;
    padding: 0 14px;
    font-size: 13px;
  }
}

/* ═══ Reduced motion ═══ */
@media (prefers-reduced-motion: reduce) {
  .lp-row,
  .lp-td,
  .lp-th__inner,
  .lp-sort-ic {
    transition: none;
  }
}
</style>
