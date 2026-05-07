<script setup>
import { computed } from 'vue'
import KpiCascadeBadge from './KpiCascadeBadge.vue'

const props = defineProps({
  kpi: { type: Object, required: true },
  editable: { type: Boolean, default: false },
})

const emit = defineEmits(['edit', 'archive', 'activate'])

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return value
  return num.toLocaleString('ko-KR')
}

const progressPct = computed(() => {
  if (typeof props.kpi.progressPct === 'number') {
    return Math.max(0, Math.min(100, Math.round(props.kpi.progressPct)))
  }
  if (props.kpi.targetValue && props.kpi.actualValue !== undefined && props.kpi.actualValue !== null) {
    return Math.max(0, Math.min(100, Math.round((props.kpi.actualValue / props.kpi.targetValue) * 100)))
  }
  return 0
})

const tone = computed(() => {
  if (progressPct.value >= 85) return 'high'
  if (progressPct.value >= 60) return 'mid'
  return 'low'
})

const statusInfo = computed(() => {
  switch (props.kpi.status) {
    case 'ACTIVE':
      return { label: '진행 중', cls: 'status--active' }
    case 'DRAFT':
      return { label: '초안', cls: 'status--draft' }
    case 'ARCHIVED':
      return { label: '보관', cls: 'status--archived' }
    default:
      return { label: props.kpi.status ?? '-', cls: 'status--draft' }
  }
})

const kindLabel = computed(() => {
  switch (props.kpi.kind) {
    case 'STRATEGIC': return '전략'
    case 'TACTICAL': return '전술'
    case 'OPERATIONAL': return '운영'
    default: return null
  }
})

const periodLabel = computed(() => props.kpi.periodCode || props.kpi.periodType || '')
</script>

<template>
  <article class="kpi-card" :class="`kpi-card--${tone}`">
    <header class="kpi-card__head">
      <div class="kpi-card__chips">
        <span class="chip chip--period">{{ periodLabel }}</span>
        <span v-if="kindLabel" class="chip chip--kind">{{ kindLabel }}</span>
        <span v-if="kpi.esgCategory" class="chip chip--esg">ESG · {{ kpi.esgCategory }}</span>
        <span class="chip chip--status" :class="statusInfo.cls">{{ statusInfo.label }}</span>
      </div>
      <button
        v-if="editable"
        type="button"
        class="kpi-card__btn"
        aria-label="편집"
        @click="emit('edit', kpi)"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z" />
        </svg>
      </button>
    </header>

    <h3 class="kpi-card__title">{{ kpi.name }}</h3>
    <p v-if="kpi.ownerOrgName" class="kpi-card__owner">{{ kpi.ownerOrgName }}</p>

    <div class="kpi-card__metric">
      <span class="kpi-card__target">{{ formatNumber(kpi.targetValue) }}</span>
      <span class="kpi-card__unit">{{ kpi.unit }}</span>
    </div>

    <div class="kpi-card__progress">
      <div class="kpi-card__progress-head">
        <span class="kpi-card__progress-label">진행률</span>
        <span class="kpi-card__progress-pct" :class="`pct--${tone}`">{{ progressPct }}%</span>
      </div>
      <div class="kpi-card__track">
        <div
          class="kpi-card__fill"
          :class="`fill--${tone}`"
          :style="{ width: progressPct + '%' }"
        />
      </div>
      <span v-if="kpi.actualValue !== undefined && kpi.actualValue !== null" class="kpi-card__progress-sub">
        {{ formatNumber(kpi.actualValue) }} / {{ formatNumber(kpi.targetValue) }} {{ kpi.unit }}
      </span>
    </div>

    <KpiCascadeBadge
      v-if="kpi.parentKpiName"
      :parent-name="kpi.parentKpiName"
      :contribution="kpi.contributionToParent"
      :unit="kpi.unit"
    />

    <footer v-if="editable && kpi.status !== 'ARCHIVED'" class="kpi-card__foot">
      <button
        v-if="kpi.status === 'DRAFT'"
        type="button"
        class="kpi-card__action"
        @click="emit('activate', kpi)"
      >활성화</button>
      <button
        type="button"
        class="kpi-card__action kpi-card__action--ghost"
        @click="emit('archive', kpi)"
      >보관</button>
    </footer>
  </article>
</template>

<style scoped>
.kpi-card {
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 18px 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}
.kpi-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--color-primary-500) 30%, var(--border-color));
  box-shadow: var(--shadow-md);
}

.kpi-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.kpi-card__chips {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
}
.chip {
  display: inline-flex;
  align-items: center;
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 999px;
  letter-spacing: 0.02em;
  background: var(--panel-muted);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}
.chip--period {
  background: color-mix(in srgb, var(--color-primary-500) 8%, transparent);
  color: var(--color-primary-700);
  border-color: color-mix(in srgb, var(--color-primary-500) 18%, transparent);
}
.chip--kind {
  background: var(--panel-muted);
  color: var(--text-primary);
}
.chip--esg {
  background: color-mix(in srgb, #10b981 14%, transparent);
  color: #047857;
  border-color: color-mix(in srgb, #10b981 24%, transparent);
}
.chip--status { font-weight: 800; }
.status--active { background: color-mix(in srgb, #10b981 14%, transparent); color: #047857; border-color: transparent; }
.status--draft { background: var(--panel-muted); color: var(--muted-text); border-color: var(--border-color); }
.status--archived { background: var(--panel-muted); color: var(--subtle-text); border-color: var(--border-color); }

.kpi-card__btn {
  display: inline-grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast);
}
.kpi-card__btn:hover {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
  border-color: color-mix(in srgb, var(--color-primary-500) 30%, var(--border-color));
}

.kpi-card__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.35;
}
.kpi-card__owner {
  margin: 0;
  font-size: 11px;
  color: var(--muted-text);
}

.kpi-card__metric {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 4px 0 -2px;
}
.kpi-card__target {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.kpi-card__unit {
  font-size: 13px;
  font-weight: 700;
  color: var(--muted-text);
}

.kpi-card__progress { display: flex; flex-direction: column; gap: 5px; }
.kpi-card__progress-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.kpi-card__progress-label { font-size: 11px; color: var(--muted-text); font-weight: 600; }
.kpi-card__progress-pct { font-size: 13px; font-weight: 800; font-variant-numeric: tabular-nums; }
.pct--high { color: #047857; }
.pct--mid { color: var(--color-primary-700); }
.pct--low { color: var(--muted-text); }

.kpi-card__track {
  height: 6px;
  background: var(--panel-muted);
  border-radius: 999px;
  overflow: hidden;
}
.kpi-card__fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.fill--high { background: #10b981; }
.fill--mid { background: var(--color-primary-500); }
.fill--low { background: var(--color-gray-400); }

.kpi-card__progress-sub {
  font-size: 10px;
  color: var(--muted-text);
  font-variant-numeric: tabular-nums;
}

.kpi-card__foot {
  display: flex;
  gap: 6px;
  padding-top: 4px;
  border-top: 1px dashed var(--border-color);
  margin-top: 4px;
}
.kpi-card__action {
  flex: 1;
  height: 28px;
  border-radius: 8px;
  border: 1px solid var(--color-primary-500);
  background: var(--color-primary-500);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition: background var(--transition-fast);
}
.kpi-card__action:hover { background: var(--color-primary-600); }
.kpi-card__action--ghost {
  background: var(--panel-color);
  color: var(--text-secondary);
  border-color: var(--border-color);
}
.kpi-card__action--ghost:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

/* Archived 상태는 카드 자체를 흐리게 */
.kpi-card[data-archived='true'] {
  opacity: 0.6;
}
</style>
