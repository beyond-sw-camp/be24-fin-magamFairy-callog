<script setup>
import { computed, onMounted, ref } from 'vue'
import { useOrganizationKpiStore } from '@/stores/organizationKpi'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
})
const emit = defineEmits(['update:modelValue'])

const store = useOrganizationKpiStore()
const search = ref('')

const contributions = computed({
  get: () => props.modelValue,
  set: (next) => emit('update:modelValue', next),
})

onMounted(async () => {
  if (store.items.length === 0) {
    store.setFilter({ status: 'ACTIVE' })
    await store.fetch()
  }
})

const candidateKpis = computed(() => {
  const all = (store.items ?? []).filter((k) => k.status === 'ACTIVE')
  if (!search.value.trim()) return all
  const term = search.value.trim().toLowerCase()
  return all.filter(
    (k) =>
      (k.name ?? '').toLowerCase().includes(term)
      || (k.ownerOrgName ?? '').toLowerCase().includes(term),
  )
})

function isPicked(kpi) {
  return contributions.value.some((c) => c.targetOrgKpiId === kpi.idx)
}

function pickedValue(kpi) {
  return contributions.value.find((c) => c.targetOrgKpiId === kpi.idx)?.committedValue ?? ''
}

function togglePick(kpi) {
  if (isPicked(kpi)) {
    contributions.value = contributions.value.filter((c) => c.targetOrgKpiId !== kpi.idx)
  } else {
    contributions.value = [
      ...contributions.value,
      {
        targetOrgKpiId: kpi.idx,
        targetOrgKpiName: kpi.name,
        unit: kpi.unit,
        ownerOrgName: kpi.ownerOrgName,
        committedValue: 0,
      },
    ]
  }
}

function setCommitted(kpi, value) {
  const num = Number(value)
  contributions.value = contributions.value.map((c) =>
    c.targetOrgKpiId === kpi.idx
      ? { ...c, committedValue: Number.isNaN(num) ? 0 : num }
      : c,
  )
}
</script>

<template>
  <div class="picker">
    <div class="picker__search">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.3-4.3" />
      </svg>
      <input
        v-model="search"
        type="text"
        placeholder="KPI 이름 또는 소속 조직 검색"
      />
    </div>

    <p v-if="contributions.length > 0" class="picker__summary">
      선택된 KPI {{ contributions.length }}개 — 각 KPI에 약속한 기여값을 입력해 주세요.
    </p>

    <ul v-if="candidateKpis.length" class="picker__list">
      <li
        v-for="kpi in candidateKpis"
        :key="kpi.idx"
        class="picker-row"
        :class="{ 'picker-row--picked': isPicked(kpi) }"
      >
        <label class="picker-row__check">
          <input
            type="checkbox"
            :checked="isPicked(kpi)"
            @change="togglePick(kpi)"
          />
          <span class="picker-row__main">
            <span class="picker-row__name">{{ kpi.name }}</span>
            <span class="picker-row__meta">
              <span class="picker-row__owner">{{ kpi.ownerOrgName ?? '' }}</span>
              <span class="picker-row__period">{{ kpi.periodCode }}</span>
              <span class="picker-row__target">목표 {{ kpi.targetValue }}{{ kpi.unit }}</span>
            </span>
          </span>
        </label>
        <div v-if="isPicked(kpi)" class="picker-row__commit">
          <span class="picker-row__commit-label">기여</span>
          <input
            type="number"
            :value="pickedValue(kpi)"
            min="0"
            step="any"
            class="picker-row__commit-input"
            @input="setCommitted(kpi, $event.target.value)"
          />
          <span class="picker-row__commit-unit">{{ kpi.unit }}</span>
        </div>
      </li>
    </ul>
    <p v-else class="picker__empty">조건에 맞는 활성 KPI가 없습니다.</p>
  </div>
</template>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.picker__search {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 0 14px;
  height: 38px;
  background: var(--control-color);
  color: var(--text-secondary);
}
.picker__search input {
  flex: 1;
  border: 0;
  background: transparent;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  color: var(--text-primary);
}
.picker__summary {
  margin: 0;
  font-size: 12px;
  color: var(--color-primary-700);
  font-weight: 600;
}
.picker__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow-y: auto;
}
.picker__empty {
  padding: 24px;
  text-align: center;
  color: var(--muted-text);
  font-size: 12px;
}
.picker-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}
.picker-row:hover {
  border-color: color-mix(in srgb, var(--color-primary-500) 30%, var(--border-color));
}
.picker-row--picked {
  background: var(--color-primary-50);
  border-color: color-mix(in srgb, var(--color-primary-500) 35%, transparent);
}
.picker-row__check {
  display: inline-flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
  min-width: 0;
  cursor: pointer;
}
.picker-row__check input[type='checkbox'] {
  margin-top: 2px;
  accent-color: var(--color-primary-500);
}
.picker-row__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.picker-row__name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}
.picker-row__meta {
  display: inline-flex;
  gap: 8px;
  font-size: 11px;
  color: var(--muted-text);
  font-variant-numeric: tabular-nums;
}
.picker-row__owner { font-weight: 600; }
.picker-row__commit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.picker-row__commit-label { font-size: 11px; color: var(--muted-text); font-weight: 700; }
.picker-row__commit-input {
  width: 80px;
  height: 32px;
  padding: 0 10px;
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
  font-variant-numeric: tabular-nums;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  color: var(--text-primary);
}
.picker-row__commit-input:focus {
  outline: none;
  border-color: var(--color-primary-500);
}
.picker-row__commit-unit {
  font-size: 11px;
  color: var(--muted-text);
  font-weight: 600;
}
</style>
