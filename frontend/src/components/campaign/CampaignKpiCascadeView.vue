<script setup>
import { computed } from 'vue'

const props = defineProps({
  contributions: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

const total = computed(() => props.contributions?.length ?? 0)

function progressPct(c) {
  if (!c.committedValue || c.committedValue === 0) return 0
  const ratio = (c.actualValue ?? 0) / c.committedValue
  return Math.max(0, Math.min(100, Math.round(ratio * 100)))
}

function tone(pct) {
  if (pct >= 85) return 'high'
  if (pct >= 60) return 'mid'
  return 'low'
}
</script>

<template>
  <section class="cascade-view">
    <header class="cascade-view__head">
      <div>
        <span class="cascade-view__eyebrow">CASCADE</span>
        <h3 class="cascade-view__title">이 캠페인이 기여하는 상위 KPI</h3>
      </div>
      <span class="cascade-view__count">{{ total }}개 매핑</span>
    </header>

    <div v-if="loading" class="cascade-view__loading">불러오는 중…</div>

    <ul v-else-if="total > 0" class="cascade-view__list">
      <li
        v-for="c in contributions"
        :key="c.idx ?? c.targetOrgKpiId"
        class="cascade-row"
      >
        <div class="cascade-row__head">
          <div class="cascade-row__label">
            <span class="cascade-row__icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 6h13a4 4 0 0 1 4 4v8" />
                <path d="m16 14 4 4 4-4" />
              </svg>
            </span>
            <div>
              <p class="cascade-row__name">{{ c.targetOrgKpiName ?? c.name }}</p>
              <p class="cascade-row__owner">
                <span
                  class="cascade-chip"
                  :class="(c.ownerOrgType === 'HQ' || !c.ownerOrgType) ? 'cascade-chip--hq' : 'cascade-chip--ga'"
                >
                  {{ (c.ownerOrgType === 'HQ' || !c.ownerOrgType) ? '🟣 본사 cascade' : '🟢 계열사 cascade' }}
                </span>
                <span class="cascade-row__owner-name">{{ c.ownerOrgName ?? '본사' }} · {{ c.periodCode ?? '' }}</span>
              </p>
            </div>
          </div>
          <div class="cascade-row__numbers">
            <span class="cascade-row__commit">
              약속 <strong>{{ c.committedValue ?? 0 }}{{ c.targetOrgKpiUnit ? ` ${c.targetOrgKpiUnit}` : '' }}</strong>
            </span>
            <span class="cascade-row__actual" :class="`tone--${tone(progressPct(c))}`">
              실적 <strong>{{ c.actualValue ?? 0 }}{{ c.targetOrgKpiUnit ? ` ${c.targetOrgKpiUnit}` : '' }}</strong>
            </span>
          </div>
        </div>
        <div class="cascade-row__track">
          <div
            class="cascade-row__fill"
            :class="`tone--${tone(progressPct(c))}`"
            :style="{ width: progressPct(c) + '%' }"
          />
        </div>
        <div class="cascade-row__sub">
          <span>달성률 {{ progressPct(c) }}%</span>
          <span v-if="c.targetOrgKpiTargetValue">상위 KPI 목표 {{ c.targetOrgKpiTargetValue }}{{ c.targetOrgKpiUnit ? ` ${c.targetOrgKpiUnit}` : '' }}</span>
        </div>
      </li>
    </ul>

    <p v-else class="cascade-view__empty">
      이 캠페인에 매핑된 상위 KPI가 없습니다. 캠페인 생성 시 또는 KPI 탭에서 매핑할 수 있습니다.
    </p>
  </section>
</template>

<style scoped>
.cascade-view {
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.cascade-view__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}
.cascade-view__eyebrow {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-primary-700);
  text-transform: uppercase;
}
.cascade-view__title {
  margin: 4px 0 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}
.cascade-view__count {
  font-size: 11px;
  color: var(--muted-text);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.cascade-view__loading,
.cascade-view__empty {
  text-align: center;
  padding: 24px;
  color: var(--muted-text);
  font-size: 12px;
}

.cascade-view__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cascade-row {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--surface-card-subtle, var(--panel-color));
}

.cascade-row__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.cascade-row__label {
  display: inline-flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}
.cascade-row__icon {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: var(--color-primary-50);
  color: var(--color-primary-700);
  flex-shrink: 0;
}
.cascade-row__name {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}
.cascade-row__owner {
  margin: 4px 0 0;
  font-size: 11px;
  color: var(--muted-text);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.cascade-chip {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 8px;
  font-size: 10px;
  font-weight: 800;
  border-radius: 999px;
  letter-spacing: 0.02em;
}
.cascade-chip--hq {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}
.cascade-chip--ga {
  background: #d1fae5;
  color: #047857;
}
.cascade-row__owner-name { color: var(--muted-text); }

.cascade-row__numbers {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  font-variant-numeric: tabular-nums;
  font-size: 11px;
  color: var(--muted-text);
}
.cascade-row__numbers strong {
  margin-left: 4px;
  font-size: 13px;
  color: var(--text-primary);
}
.cascade-row__actual.tone--high strong { color: #047857; }
.cascade-row__actual.tone--mid strong { color: var(--color-primary-700); }
.cascade-row__actual.tone--low strong { color: var(--color-coral-700, #c04438); }

.cascade-row__track {
  height: 6px;
  border-radius: 999px;
  background: var(--panel-muted);
  overflow: hidden;
}
.cascade-row__fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.5s ease;
}
.cascade-row__fill.tone--high { background: #10b981; }
.cascade-row__fill.tone--mid { background: var(--color-primary-500); }
.cascade-row__fill.tone--low { background: var(--color-gray-400); }

.cascade-row__sub {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: var(--muted-text);
  font-variant-numeric: tabular-nums;
}
</style>
